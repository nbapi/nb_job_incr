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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;

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
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.dao.adataper.IncrInventoryAdapter;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.ConfigUtils;
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

	@Resource
	private MySqlDataDao mySqlDataDao;

	/** 
	 * 根据changeID同步库存增量
	 *
	 * @param changID
	 * @return
	 */
	public long syncInventoryToDB(long changID) {
		// 库存变化流水表获取数据
		List<Map<String, Object>> productInventoryIncrementList = getProductInventoryIncrement(changID);
		if (productInventoryIncrementList == null || productInventoryIncrementList.size() == 0)
			return changID;

		// 过滤掉携程去哪儿酒店
		filterShotelsIds(productInventoryIncrementList);
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

		final List<IncrInventory> incrInventorys = Collections.synchronizedList(new ArrayList<IncrInventory>());
		int goodsInventoryThreadCount = ConfigUtils.getIntConfigValue("goodsInventoryThreadCount", 3);
		ExecutorService executorService = ExecutorUtils.newSelfThreadPool(goodsInventoryThreadCount, 300);
		try {
			List<Future<List<IncrInventory>>> futureList = executorService.invokeAll(callableList);
			for (Future<List<IncrInventory>> future : futureList) {
				List<IncrInventory> threadIncrInventorys = future.get();
				incrInventorys.addAll(threadIncrInventorys);
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",getIncrInventoryList from goods,incrInventorys size = "
				+ incrInventorys.size());

		// 按照ChangeID排序
		sortIncrInventorysByChangeID(incrInventorys);
		// 插入数据库
		builkInsert(incrInventorys);
		Number lastChangeId = (Number) productInventoryIncrementList.get(productInventoryIncrementList.size() - 1).get("id");
		return lastChangeId.longValue();
	}

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
	 * 库存变化流水表获取数据
	 *
	 * @param changID
	 * @return
	 */
	private List<Map<String, Object>> getProductInventoryIncrement(long changID) {
		Map<String, Object> params = new HashMap<String, Object>();
		// 延迟3分钟maxRecordCount
		int maxRecordCount = ConfigUtils.getIntConfigValue("MaxProductInventoryIncrementCount", 1000);
		params.put("maxRecordCount", maxRecordCount);
		params.put("delay_time", DateTime.now().minusMinutes(3).toString("yyyy-MM-dd HH:mm:ss"));
		if (changID > 0) {
			params.put("id", changID);
		} else {
			params.put("op_date", DateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss"));
		}
		logger.info("getProductInventoryIncrement, params = " + params);
		long startTime = System.currentTimeMillis();
		List<Map<String, Object>> productInventoryIncrementList = mySqlDataDao.getProductInventoryIncrement(params);
		long endTime = System.currentTimeMillis();
		int incrementListSize = (productInventoryIncrementList == null) ? 0 : productInventoryIncrementList.size();
		logger.info("use time = " + (endTime - startTime) + ",getProductInventoryIncrement, productInventoryIncrementList size = "
				+ incrementListSize);
		return productInventoryIncrementList;
	}

	/** 
	 * 按照ChangeID排序
	 *
	 * @param incrInventorys
	 */
	private void sortIncrInventorysByChangeID(List<IncrInventory> incrInventorys) {
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
		int successCount = 0;
		logger.info("IncrInventory BulkInsert start,recordCount = " + recordCount);
		int pageSize = ConfigUtils.getIntConfigValue("IncrRateBatchSize", 50);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		long startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			// int startNum = (pageIndex - 1) * pageSize;
			// int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			// successCount += incrInventorySubmeterService.builkInsert(incrInventorys.subList(startNum, endNum));
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrInventory BulkInsert successfully,successCount = "
				+ successCount);
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
			String mhotelId = msRelationRepository.getValidMHotelId(shotelid);
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
		// request.setNeed_instant_confirm(isNeedInstantConfirm);
		// request.setOrder_from(orderFrom);
		request.setSearch_from(3);// 3：NBAPI
		request.setMhotel_attr(mhotel_attr);
		try {
			GetInvAndInstantConfirmResponse response = goodsMetaRepository.getInventory(request);
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
				throw new RuntimeException(response.getReturn_msg());
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
	private void filterShotelsIds(List<Map<String, Object>> productInventoryIncrementList) {
		long startTime = System.currentTimeMillis();
		Set<String> filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
		Iterator<Map<String, Object>> iter = productInventoryIncrementList.iterator();
		while (iter.hasNext()) {
			Map<String, Object> productInventoryIncrement = iter.next();
			if (productInventoryIncrement == null)
				continue;
			String hotelCode = (String) productInventoryIncrement.get("hotel_id");
			if (!filteredSHotelIds.contains(hotelCode))
				continue;
			iter.remove();
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime)
				+ ",after fillFilteredSHotelsIds,productInventoryIncrementList size = " + productInventoryIncrementList.size());
	}

}
