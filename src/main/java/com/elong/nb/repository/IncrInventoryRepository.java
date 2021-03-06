/**   
 * @(#)IncrInventoryRepository.java	2016年9月21日	下午4:23:56	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.hotel.searchagent.thrift.dss.GetInvAndInstantConfirmRequest;
import com.elong.hotel.searchagent.thrift.dss.GetInvAndInstantConfirmResponse;
import com.elong.hotel.searchagent.thrift.dss.MhotelAttr;
import com.elong.hotel.searchagent.thrift.dss.ShotelAttr;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.adataper.IncrInventoryAdapter;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.ms.agent.HotelDataServiceAgent;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.ConfigUtils;
import com.elong.nb.util.DateHandlerUtils;
import com.elong.nb.util.ExecutorUtils;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:23:56   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrInventoryRepository {

	private static final Logger logger = Logger.getLogger("IncrInventoryLogger");

	private static final int MAXDAYS = 90;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private GoodsMetaRepository goodsMetaRepository;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	/** 
	 * 同步库存增量
	 *
	 * @param productInventoryIncrementList
	 * @return
	 */
	public long syncInventoryToDB(List<Map<String, Object>> productInventoryIncrementList) {
		long changID = -1;
		if (productInventoryIncrementList == null || productInventoryIncrementList.size() == 0)
			return changID;

		// 开始结束日期过滤
		filterUnvalidDate(productInventoryIncrementList);
		if (productInventoryIncrementList == null || productInventoryIncrementList.size() == 0)
			return changID;

		// 分批次批量调用商品库库存元数据接口
		List<Callable<List<IncrInventory>>> callableList = new ArrayList<Callable<List<IncrInventory>>>();
		int recordCount = productInventoryIncrementList.size();
		int batchSize = ConfigUtils.getIntConfigValue("GoodsInventoryBatchSize", 10);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / batchSize);
		long startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * batchSize;
			int endNum = pageIndex * batchSize > recordCount ? recordCount : pageIndex * batchSize;
			callableList.add(new GoodsInventoryThread(productInventoryIncrementList.subList(startNum, endNum)));
		}
		List<IncrInventory> incrInventorys = new ArrayList<IncrInventory>();
		int goodsInventoryThreadCount = ConfigUtils.getIntConfigValue("GoodsInventoryThreadCount", 3);
		ExecutorService executorService = ExecutorUtils.newSelfThreadPool(goodsInventoryThreadCount, 300);
		try {
			List<Future<List<IncrInventory>>> futureList = executorService.invokeAll(callableList);
			executorService.shutdown();
			for (Future<List<IncrInventory>> future : futureList) {
				List<IncrInventory> threadIncrInventorys = future.get();
				incrInventorys.addAll(threadIncrInventorys);
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",getIncrInventoryList from goods,incrInventorys size = "
				+ incrInventorys.size());

		// 过滤掉携程去哪儿酒店
		filterShotelsIds(incrInventorys);
		// 按照ChangeID排序
		sortIncrInventorysByChangeID(incrInventorys);
		// 库存增量数据压缩
		compressIncrInventory(incrInventorys);
		// 插入数据库
		builkInsert(incrInventorys);
		Number lastChangeId = (Number) productInventoryIncrementList.get(productInventoryIncrementList.size() - 1).get("id");
		return lastChangeId.longValue();
	}

	/** 
	 * 将hotelcode分配到20个组,这样存redis时最多连接20次 
	 *
	 * @param hotelCode
	 * @return
	 */
	private String getCompressHashKey(String hotelCode) {
		return "incrinv_" + Integer.parseInt(hotelCode) / 20;
	}

	/** 
	 * 库存增量数据压缩 
	 *
	 * @param incrInventorys
	 */
	private void compressIncrInventory(List<IncrInventory> incrInventorys) {
		if (incrInventorys == null || incrInventorys.size() == 0)
			return;
		long startTime = System.currentTimeMillis();
		Map<String, List<String>> keyMap = new HashMap<String, List<String>>();
		for (IncrInventory incrInventory : incrInventorys) {
			String hashKey = getCompressHashKey(incrInventory.getHotelCode());
			List<String> md5keyList = keyMap.get(hashKey);
			if (md5keyList == null) {
				md5keyList = new ArrayList<String>();
			}
			String md5key = incrInventory.getHotelCode().trim() + incrInventory.getRoomTypeID().trim()
					+ DateHandlerUtils.formatDate(incrInventory.getAvailableDate(), "yyyyMMddHHmmss");
			md5keyList.add(md5key);
			keyMap.put(hashKey, md5keyList);
		}
		Map<String, String> cacheMap = commonRepository.batchHashGetMapFromRedis(keyMap);
		int beforeSize = incrInventorys.size();
		Iterator<IncrInventory> iter = incrInventorys.iterator();
		Map<String, Map<String, String>> waitSaveMap = new HashMap<String, Map<String, String>>();
		while (iter.hasNext()) {
			IncrInventory incrInventory = iter.next();
			String md5key = incrInventory.getHotelCode().trim() + incrInventory.getRoomTypeID().trim()
					+ DateHandlerUtils.formatDate(incrInventory.getAvailableDate(), "yyyyMMddHHmmss");
			String currentValue = (incrInventory.isStatus() ? "Y" : "N") + incrInventory.getOverBooking() + incrInventory.getStartDate()
					+ incrInventory.getEndDate() + incrInventory.getAvailableAmount() + (incrInventory.isIsInstantConfirm() ? "Y" : "N")
					+ incrInventory.getIC_BeginTime() + incrInventory.getIC_EndTime();
			String md5CurrentValue = DigestUtils.md5Hex(currentValue);
			String md5ExistValue = cacheMap.get(md5key);
			if (StringUtils.isEmpty(md5ExistValue) || !md5ExistValue.equals(md5CurrentValue)) {
				String hashKey = getCompressHashKey(incrInventory.getHotelCode());
				Map<String, String> valMap = waitSaveMap.get(hashKey);
				if (valMap == null) {
					valMap = new HashMap<String, String>();
				}
				valMap.put(md5key, md5CurrentValue);
				waitSaveMap.put(hashKey, valMap);
			} else {
				iter.remove();
			}
		}
		commonRepository.batchHashSetMapToRedis(waitSaveMap);
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",compressIncrInventory and filter size = "
				+ (beforeSize - incrInventorys.size()));
	}

	/** 
	 * 按照ChangeID排序
	 *
	 * @param incrInventorys
	 */
	private void sortIncrInventorysByChangeID(List<IncrInventory> incrInventorys) {
		if (incrInventorys == null || incrInventorys.size() == 0)
			return;
		long startTime = System.currentTimeMillis();
		Collections.sort(incrInventorys, new Comparator<IncrInventory>() {
			@Override
			public int compare(IncrInventory o1, IncrInventory o2) {
				return (int) ((long) (o1.getChangeID()) - (long) (o2.getChangeID()));
			}
		});
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",sortIncrInventorysByChangeID");
	}

	/** 
	 * 批量插入数据库 
	 *
	 * @param incrInventorys
	 */
	private void builkInsert(List<IncrInventory> incrInventorys) {
		int recordCount = incrInventorys.size();
		if (recordCount == 0)
			return;
		logger.info("IncrInventory BulkInsert start,recordCount = " + recordCount);
		String builkInsertSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrInventoryInsertSizePerTask");
		int pageSize = StringUtils.isEmpty(builkInsertSize) ? 8000 : Integer.valueOf(builkInsertSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		List<MysqlInventoryThread> callableList = new ArrayList<MysqlInventoryThread>();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			MysqlInventoryThread mysqlInventoryThread = new MysqlInventoryThread(incrInventorys.subList(startNum, endNum));
			callableList.add(mysqlInventoryThread);
		}
		int callableListSize = callableList.size();

		// 多线程插数据
		int mysqlInventoryThreadCount = ConfigUtils.getIntConfigValue("MysqlInsertThreadCount", 10);
		mysqlInventoryThreadCount = callableListSize < mysqlInventoryThreadCount ? callableListSize : mysqlInventoryThreadCount;
		ExecutorService executorService = Executors.newFixedThreadPool(mysqlInventoryThreadCount);
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		try {
			List<Future<Integer>> futureList = executorService.invokeAll(callableList);
			executorService.shutdown();
			for (Future<Integer> future : futureList) {
				int perThreadSuccessCount = future.get();
				successCount += perThreadSuccessCount;
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrInventory BulkInsert successfully,successCount = "
				+ successCount + ",threadCount = " + mysqlInventoryThreadCount);
	}

	/** 
	 * 获取IncrInventory集合
	 *
	 * @param subList
	 * @return
	 */
	private List<IncrInventory> getIncrInventoryList(List<Map<String, Object>> productInventoryIncrementList) {
		Date minStartDate = null;
		Date maxEndDate = null;
		Map<String, List<String>> mhotelParams = new HashMap<String, List<String>>();
		Map<String, List<Integer>> shotelParams = new HashMap<String, List<Integer>>();
		for (Map<String, Object> productInventoryIncrement : productInventoryIncrementList) {
			String shotelid = (String) productInventoryIncrement.get("hotel_id");
			String mhotelId = (String) productInventoryIncrement.get("mhotel_id");
			mhotelId = StringUtils.isEmpty(mhotelId) ? msRelationRepository.getValidMHotelId(shotelid) : mhotelId;
			if (mhotelId == null)
				continue;
			// 最大日期 与 最小日期
			Date startDate = (Date) productInventoryIncrement.get("begin_date");
			if (minStartDate == null || startDate.before(minStartDate)) {
				minStartDate = startDate;
			}
			Date endDate = (Date) productInventoryIncrement.get("end_date");
			if (maxEndDate == null || endDate.after(maxEndDate)) {
				maxEndDate = endDate;
			}
			// shotelid 与 roomtypeid关系
			List<Integer> roomTypeIds = shotelParams.get(shotelid);
			if (roomTypeIds == null) {
				roomTypeIds = new ArrayList<Integer>();
			}
			// 兼容room_type_id 0001,0003,0004,0005,0006,0007 这种数据格式
			String roomTypeIdStr = (String) productInventoryIncrement.get("room_type_id");
			String[] roomTypeIdArray = StringUtils.split(roomTypeIdStr, ",", -1);
			for (String element : roomTypeIdArray) {
				Integer roomTypeId = Integer.valueOf(element);
				if (!roomTypeIds.contains(roomTypeId)) {
					roomTypeIds.add(roomTypeId);
				}
			}
			shotelParams.put(shotelid, roomTypeIds);

			// mhotelid 与 shotelid关系
			List<String> hotelCodes = mhotelParams.get(mhotelId);
			if (hotelCodes == null) {
				hotelCodes = new ArrayList<String>();
			}
			if (!hotelCodes.contains(shotelid)) {
				hotelCodes.add(shotelid);
			}
			mhotelParams.put(mhotelId, hotelCodes);
		}
		// 组装mhotelAttrs
		List<MhotelAttr> mhotelAttrs = new ArrayList<MhotelAttr>();
		for (Map.Entry<String, List<String>> mhotelEntry : mhotelParams.entrySet()) {
			String mhotelid = mhotelEntry.getKey();
			List<String> shotelids = mhotelEntry.getValue();
			List<ShotelAttr> shotelAttrs = new ArrayList<ShotelAttr>();
			for (String shotelid : shotelids) {
				ShotelAttr shotelAttr = new ShotelAttr();
				shotelAttr.setShotel_id(Integer.valueOf(shotelid));
				shotelAttr.setSroom_ids(shotelParams.get(shotelid));
				shotelAttrs.add(shotelAttr);
			}
			MhotelAttr mhotelAttr = new MhotelAttr();
			mhotelAttr.setMhotel_id(Integer.valueOf(mhotelid));
			mhotelAttr.setShotel_attr(shotelAttrs);
			mhotelAttrs.add(mhotelAttr);
		}
		Map<String, List<IncrInventory>> incrInventoryMap = getInventorysFromGoods(mhotelAttrs, minStartDate, maxEndDate);
		List<IncrInventory> resultList = new ArrayList<IncrInventory>();
		for (Map<String, Object> productInventoryIncrement : productInventoryIncrementList) {
			Date opDate = (Date) productInventoryIncrement.get("op_date");
			Number id = (Number) productInventoryIncrement.get("id");
			Date startDate = (Date) productInventoryIncrement.get("begin_date");
			Date endDate = (Date) productInventoryIncrement.get("end_date");

			String shotelid = (String) productInventoryIncrement.get("hotel_id");
			String roomTypeIdStr = (String) productInventoryIncrement.get("room_type_id");
			String[] roomTypeIdArray = StringUtils.split(roomTypeIdStr, ",", -1);
			for (String roomTypeId : roomTypeIdArray) {
				String key = shotelid + "|" + roomTypeId;
				List<IncrInventory> incrInventorys = incrInventoryMap.get(key);
				if (incrInventorys == null || incrInventorys.size() == 0)
					continue;

				for (IncrInventory incrInventory : incrInventorys) {
					Date availableDate = incrInventory.getAvailableDate();
					if (availableDate.after(endDate) || availableDate.before(startDate))
						continue;
					incrInventory.setOperateTime(opDate);
					incrInventory.setInsertTime(DateTime.now().toDate());
					incrInventory.setChangeID(id.longValue());
					incrInventory.setChangeTime(opDate);
					resultList.add(incrInventory);
				}
			}
		}
		return resultList;
	}

	/** 
	 * 批量调用库存元数据
	 *
	 * @param mhotel_attr
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private Map<String, List<IncrInventory>> getInventorysFromGoods(List<MhotelAttr> mhotel_attr, Date startDate, Date endDate) {
		Map<String, List<IncrInventory>> incrInventoryMap = null;
		GetInvAndInstantConfirmRequest request = new GetInvAndInstantConfirmRequest();
		request.setStart_date(startDate != null ? startDate.getTime() : new Date().getTime());
		request.setEnd_date(endDate != null ? endDate.getTime() : DateTime.now().plusDays(MAXDAYS).toDate().getTime());
		request.setSearch_from(3);// 3：NBAPI
		request.setMhotel_attr(mhotel_attr);
		request.setNeed_instant_confirm(true);

		GetInvAndInstantConfirmResponse response = null;
		Exception exception = null;
		int reqCount = 0;
		while (++reqCount <= 3) {
			exception = null;
			try {
				response = goodsMetaRepository.getInventory(request);
				break;
			} catch (Exception ex) {
				logger.error("ThriftUtils.getInventory,reqCount = " + reqCount + "," + ex.getMessage());
				exception = ex;
			}
		}
		if (exception != null) {
			throw new RuntimeException("ThriftUtils.getInventory:" + exception.getMessage(), exception);
		}

		try {
			if (response != null && response.return_code == 0) {
				IncrInventoryAdapter incrInventoryAdapter = new IncrInventoryAdapter();
				incrInventoryMap = incrInventoryAdapter.toNBObect(response);
				if (incrInventoryMap == null || incrInventoryMap.size() == 0) {
					logger.error("ThriftUtils.getInventory,response.return_code = 0,inventorys size = 0,request = "
							+ JSON.toJSONString(request) + ",response = " + JSON.toJSONString(response));
				}
			} else if (response.return_code > 0) {
				incrInventoryMap = new HashMap<String, List<IncrInventory>>();
				logger.info("ThriftUtils.getInventory, response.return_code > 0,request = " + JSON.toJSONString(request) + ",response = "
						+ JSON.toJSONString(response));
			} else {
				throw new RuntimeException("ThriftUtils.getInventory,response.return_msg = " + response.getReturn_msg());
			}
		} catch (Exception ex) {
			throw new RuntimeException("getInventorysFromGoods:" + ex.getMessage(), ex);
		}
		return incrInventoryMap;
	}

	/** 
	 * 仅提供昨天和最近90天的房态数据 判断开始结束时间段是否在昨天和MaxDays之内
	 *
	 * @param productInventoryIncrementList
	 */
	private void filterUnvalidDate(List<Map<String, Object>> productInventoryIncrementList) {
		long startTime = System.currentTimeMillis();
		Iterator<Map<String, Object>> iter = productInventoryIncrementList.iterator();
		while (iter.hasNext()) {
			Map<String, Object> productInventoryIncrement = iter.next();
			if (productInventoryIncrement == null)
				continue;

			Date startDate = (Date) productInventoryIncrement.get("begin_date");
			Date endDate = (Date) productInventoryIncrement.get("end_date");

			// #region 仅提供昨天和最近90天的房态数据 判断开始结束时间段是否在昨天和MaxDays之内
			int startDays = Days.daysBetween(DateTime.now(), new DateTime(startDate.getTime())).getDays();
			int endDays = Days.daysBetween(DateTime.now(), new DateTime(endDate.getTime())).getDays();
			if (startDays > MAXDAYS || endDays < 0) {
				iter.remove();
				continue;
			}
			if (startDays < -1) {
				startDate = new Date();
				productInventoryIncrement.put("begin_date", new Date());
			}
			if (endDays > MAXDAYS) {
				endDate = DateTime.now().plusDays(MAXDAYS).toDate();
				productInventoryIncrement.put("end_date", endDate);
			}
			if (startDate.compareTo(endDate) > 0) {
				iter.remove();
				continue;
			}
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime)
				+ ",after filterUnvalidDate,productInventoryIncrementList size = " + productInventoryIncrementList.size());
	}

	/** 
	 * 过滤掉携程去哪shotelid 
	 *
	 * @param productInventoryIncrementList
	 */
	private void filterShotelsIds(List<IncrInventory> incrInventorys) {
		long startTime = System.currentTimeMillis();
		Iterator<IncrInventory> iter = incrInventorys.iterator();
		Set<String> roomTypeIdList = new HashSet<String>();
		while (iter.hasNext()) {
			IncrInventory incrInventory = iter.next();
			if (incrInventory == null)
				continue;
			incrInventory.setChannel(0);
			String hotelCode = incrInventory.getHotelCode();
			String roomTypeId = incrInventory.getRoomTypeID();
			roomTypeIdList.add(hotelCode + "_" + roomTypeId);
		}
		Map<String, String> sellChannelMap = HotelDataServiceAgent.getSellChannelsByRoomTypeId(roomTypeIdList.toArray(new String[0]));
		for (IncrInventory incrInventory : incrInventorys) {
			String hotelCode = incrInventory.getHotelCode();
			String roomTypeId = incrInventory.getRoomTypeID();

			String sellChannelKey = hotelCode + "_" + roomTypeId;
			String sellChannel = sellChannelMap.get(sellChannelKey);
			sellChannel = StringUtils.isEmpty(sellChannel) ? "65534" : sellChannel;
			incrInventory.setSellChannel(Integer.parseInt(sellChannel));
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",after fillFilteredSHotelsIds,incrInventorys size = "
				+ incrInventorys.size());
	}

	/**
	 * 商品库库存获取内部类
	 *
	 * <p>
	 * 修改历史:											<br>  
	 * 修改日期    		修改人员   	版本	 		修改内容<br>  
	 * -------------------------------------------------<br>  
	 * 2017年6月20日 下午5:30:47   suht     1.0    	初始化创建<br>
	 * </p> 
	 *
	 * @author		suht  
	 * @version		1.0  
	 * @since		JDK1.7
	 */
	private class GoodsInventoryThread implements Callable<List<IncrInventory>> {

		private List<Map<String, Object>> productInventoryIncrementList;

		public GoodsInventoryThread(List<Map<String, Object>> productInventoryIncrementList) {
			this.productInventoryIncrementList = productInventoryIncrementList;
		}

		@Override
		public List<IncrInventory> call() throws Exception {
			return getIncrInventoryList(productInventoryIncrementList);
		}

	}

	/**
	 * 数据库插入数据任务
	 *
	 * <p>
	 * 修改历史:											<br>  
	 * 修改日期    		修改人员   	版本	 		修改内容<br>  
	 * -------------------------------------------------<br>  
	 * 2017年9月12日 上午11:32:51   suht     1.0    	初始化创建<br>
	 * </p> 
	 *
	 * @author		suht  
	 * @version		1.0  
	 * @since		JDK1.7
	 */
	private class MysqlInventoryThread implements Callable<Integer> {

		private List<IncrInventory> incrInventoryList;

		public MysqlInventoryThread(List<IncrInventory> incrInventoryList) {
			this.incrInventoryList = incrInventoryList;
		}

		@Override
		public Integer call() throws Exception {
			return incrInventorySubmeterService.builkInsert(incrInventoryList);
		}

	}

}