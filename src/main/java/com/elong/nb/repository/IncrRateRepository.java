/**   
 * @(#)IncrRateRepository.java	2016年9月21日	下午4:15:22	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbRequest;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbResponse;
import com.elong.hotel.goods.ds.thrift.HotelBasePriceRequest;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.dao.adataper.IncrRateAdapter;
import com.elong.nb.model.bean.IncrRate;
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
 * 2016年9月21日 下午4:15:22   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrRateRepository {

	private static final Logger logger = Logger.getLogger("IncrRateLogger");

	@Resource
	private GoodsMetaRepository goodsMetaRepository;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource(name = "incrRateSubmeterService")
	private ISubmeterService<IncrRate> incrRateSubmeterService;

	@Resource
	private MySqlDataDao mySqlDataDao;
	
	/** 
	 * IncrRate同步到数据库
	 *
	 * @param changID
	 * @return
	 */
	public long syncRatesToDB(long changID) {
		// 价格变化流水表获取数据
		List<Map<String, Object>> priceOperationIncrementList = getPriceOperationIncrement(changID);
		if (priceOperationIncrementList == null || priceOperationIncrementList.size() == 0)
			return changID;

		// 最大有效日期跨度
		int GoodsMetaPriceMaxDays = ConfigUtils.getIntConfigValue("GoodsMetaPriceMaxDays", 180);
		Date validDate = DateTime.now().plusDays(GoodsMetaPriceMaxDays).toDate();
		try {
			validDate = DateHandlerUtils.convertDateParttern(validDate, "yyyy-MM-dd");
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e.getMessage());
		}
		// 过滤掉最大有效日期之外无效数据
		Iterator<Map<String, Object>> iter = priceOperationIncrementList.iterator();
		while (iter.hasNext()) {
			Map<String, Object> priceOperationIncrement = iter.next();
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			if (startDate.compareTo(validDate) > 0) {
				iter.remove();
			}
		}
		final List<Map<String, Object>> filterPriceOperationIncrementList = priceOperationIncrementList;
		logger.info("after filter by validDate[" + validDate + "],PriceOperationIncrementList size = "
				+ filterPriceOperationIncrementList.size());

		// 分批次批量调用商品库价格元数据接口
		List<Callable<List<IncrRate>>> callableList = new ArrayList<Callable<List<IncrRate>>>();
		int recordCount = filterPriceOperationIncrementList.size();
		int batchSize = ConfigUtils.getIntConfigValue("GoodsRateBatchSize", 10);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / batchSize);
		long startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * batchSize;
			int endNum = pageIndex * batchSize > recordCount ? recordCount : pageIndex * batchSize;
			callableList.add(new GoodsPriceMetaThread(filterPriceOperationIncrementList.subList(startNum, endNum)));
		}
		List<IncrRate> incrRates = new ArrayList<IncrRate>();
		int goodsRateThreadCount = ConfigUtils.getIntConfigValue("GoodsRateThreadCount", 3);
		ExecutorService executorService = ExecutorUtils.newSelfThreadPool(goodsRateThreadCount, 300);
		try {
			List<Future<List<IncrRate>>> futureList = executorService.invokeAll(callableList);
			executorService.shutdown();
			for (Future<List<IncrRate>> future : futureList) {
				List<IncrRate> threadIncrRates = future.get();
				incrRates.addAll(threadIncrRates);
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime)
				+ ",getIncrRateList from Goods and doHandler,incrRates size = " + incrRates.size());

		// shotelid过滤及enddate处理
		filterAndHandler(incrRates);
		// 按照ChangeID排序
		sortIncrRatesByChangeID(incrRates);
		// 插入数据库
		builkInsert(incrRates);
		return (Long) priceOperationIncrementList.get(priceOperationIncrementList.size() - 1).get("id");
	}

	/** 
	 * 按照ChangeID排序
	 *
	 * @param afterIncrRates
	 */
	private void sortIncrRatesByChangeID(List<IncrRate> afterIncrRates) {
		long startTime = System.currentTimeMillis();
		Collections.sort(afterIncrRates, new Comparator<IncrRate>() {
			@Override
			public int compare(IncrRate o1, IncrRate o2) {
				return (int) ((long) (o1.getChangeID()) - (long) (o2.getChangeID()));
			}
		});
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",sortIncrRatesByChangeID");
	}

	/** 
	 * 批量调用价格元数据
	 *
	 * @param hotelBases
	 * @param startDate
	 * @param endDate
	 * @param roomTypeIDSet 过滤减少创建没用对象
	 * @param rateplanIDSet 过滤减少创建没用对象
	 * @return
	 */
	private List<IncrRate> getRatesFromGoods(List<HotelBasePriceRequest> hotelBases, Date startDate, Date endDate,
			Set<String> roomTypeIDSet, Set<Integer> rateplanIDSet) {
		List<IncrRate> incrRates = null;
		GetBasePrice4NbRequest request = new GetBasePrice4NbRequest();
		request.setBooking_channel(126);
		request.setSell_channel(65534);
		request.setMember_level(30);
		request.setTraceId(UUID.randomUUID().toString());
		request.setStart_date((int) (startDate.getTime() / 1000));
		request.setEnd_date((int) (endDate.getTime() / 1000));
		request.setHotel_base_price_request(hotelBases);

		GetBasePrice4NbResponse response = null;
		Exception exception = null;
		int reqCount = 0;
		while (++reqCount <= 3) {
			exception = null;
			try {
				response = goodsMetaRepository.getMetaPrice4Nb(request);
				break;
			} catch (Exception ex) {
				logger.error("ThriftUtils.getMetaPrice4Nb,reqCount = " + reqCount + "," + ex.getMessage());
				exception = ex;
			}
		}
		if (exception != null) {
			throw new RuntimeException("ThriftUtils.getMetaPrice4Nb:" + exception.getMessage(), exception);
		}

		try {
			if (response != null && response.return_code == 0) {
				IncrRateAdapter adapter = new IncrRateAdapter();
				incrRates = adapter.toNBObject(response, roomTypeIDSet, rateplanIDSet);
				if (incrRates == null || incrRates.size() == 0) {
					logger.error("ThriftUtils.getMetaPrice4Nb,response.return_code = 0,incrRates size = 0,request = "
							+ JSON.toJSONString(request) + ",response = " + JSON.toJSONString(response));
				}
			} else if (response.return_code > 0) {
				incrRates = new ArrayList<IncrRate>();
				logger.info("ThriftUtils.getMetaPrice4Nb, response.return_code > 0,request = " + JSON.toJSONString(request)
						+ ",response = " + JSON.toJSONString(response));
			} else {
				throw new RuntimeException("ThriftUtils.getMetaPrice4Nb, response.return_msg = " + response.getReturn_msg());
			}
		} catch (Exception ex) {
			throw new RuntimeException("getRatesFromGoods:" + ex.getMessage(), ex);
		}
		return incrRates;
	}

	/** 
	 * 获取incrrate集合
	 *
	 * @param priceOperationIncrementList
	 * @param validDate
	 * @return
	 */
	private List<IncrRate> getIncrRateList(List<Map<String, Object>> priceOperationIncrementList) {
		List<HotelBasePriceRequest> hotelBases = new LinkedList<HotelBasePriceRequest>();
		Date minStartDate = null;
		Date maxEndDate = null;
		List<String> hotelCodeList = new ArrayList<String>();
		Set<String> roomTypeIDSet = new HashSet<String>();
		Set<Integer> rateplanIDSet = new HashSet<Integer>();
		boolean isContinue = false;
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			String hotelCode = (String) priceOperationIncrement.get("hotel_id");
			String hotelId = msRelationRepository.getValidMHotelId(hotelCode);
			if (hotelId == null)
				continue;
			if (!hotelCodeList.contains(hotelCode)) {
				HotelBasePriceRequest hotelBase = new HotelBasePriceRequest();
				hotelBase.setMhotel_id(Integer.valueOf(hotelId));
				hotelBase.setShotel_id(Integer.valueOf(hotelCode));
				hotelBases.add(hotelBase);
				hotelCodeList.add(hotelCode);
			}
			isContinue = true;
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			if (minStartDate == null || startDate.before(minStartDate)) {
				minStartDate = startDate;
			}
			Timestamp end_date = (Timestamp) priceOperationIncrement.get("end_date");
			Date endDate = new Date(end_date.getTime());
			if (maxEndDate == null || endDate.after(maxEndDate)) {
				maxEndDate = endDate;
			}

			roomTypeIDSet.add((String) priceOperationIncrement.get("roomtype_id"));
			rateplanIDSet.add((Integer) priceOperationIncrement.get("rateplan_id"));
		}
		Map<String, List<IncrRate>> groupRateMap = new HashMap<String, List<IncrRate>>();
		List<IncrRate> goodsRateList = Collections.emptyList();
		if (isContinue) {
			goodsRateList = getRatesFromGoods(hotelBases, minStartDate, maxEndDate, roomTypeIDSet, rateplanIDSet);
		}
		for (IncrRate goodsRate : goodsRateList) {
			if (goodsRate == null)
				continue;
			String hotelCode = (String) goodsRate.getHotelCode();
			String roomTypeID = (String) goodsRate.getRoomTypeId();
			Integer rateplanID = (Integer) goodsRate.getRateplanId();
			String key = hotelCode + "|" + roomTypeID + "|" + rateplanID;
			List<IncrRate> groupList = groupRateMap.get(key);
			if (groupList == null) {
				groupList = new ArrayList<IncrRate>();
			}
			groupList.add(goodsRate);
			groupRateMap.put(key, groupList);
		}
		List<IncrRate> incrRates = new ArrayList<IncrRate>();
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			Long id = (Long) priceOperationIncrement.get("id");
			String hotelCode = (String) priceOperationIncrement.get("hotel_id");
			String roomTypeID = (String) priceOperationIncrement.get("roomtype_id");
			Integer rateplanID = (Integer) priceOperationIncrement.get("rateplan_id");
			String key = hotelCode + "|" + roomTypeID + "|" + rateplanID;
			List<IncrRate> groupList = groupRateMap.get(key);
			if (groupList == null || groupList.size() == 0)
				continue;

			Timestamp operate_time = (Timestamp) priceOperationIncrement.get("operate_time");
			Date changeTime = new Date(operate_time.getTime());
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			Timestamp end_date = (Timestamp) priceOperationIncrement.get("end_date");
			Date endDate = new Date(end_date.getTime());
			for (IncrRate goodsRate : groupList) {
				Date goodsStartDate = (Date) goodsRate.getStartDate();
				Date goodsEndDate = (Date) goodsRate.getEndDate();
				// 日期没匹配上，过滤掉
				if (startDate.after(goodsEndDate) || endDate.before(goodsStartDate))
					continue;
				Date finalStartDate = startDate.after(goodsStartDate) ? startDate : goodsStartDate;
				Date finalEndDate = endDate.before(goodsEndDate) ? endDate : goodsEndDate;
				goodsRate.setStartDate(finalStartDate);
				goodsRate.setEndDate(finalEndDate);
				goodsRate.setChangeTime(changeTime);
				goodsRate.setOperateTime(changeTime);
				goodsRate.setChangeID(id);
				incrRates.add(goodsRate);
			}
		}
		return incrRates;
	}

	/** 
	 * 价格变化流水表获取数据
	 *
	 * @param changID
	 * @return
	 */
	private List<Map<String, Object>> getPriceOperationIncrement(long changID) {
		Map<String, Object> params = new HashMap<String, Object>();
		// 延迟3分钟maxRecordCount
		int maxRecordCount = ConfigUtils.getIntConfigValue("MaxPriceOperationIncrementCount", 1000);
		params.put("maxRecordCount", maxRecordCount);
		params.put("delay_time", DateTime.now().minusMinutes(3).toString("yyyy-MM-dd HH:mm:ss"));
		if (changID > 0) {
			params.put("id", changID);
		} else {
			params.put("operate_time", DateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss"));
		}
		logger.info("getPriceOperationIncrement, params = " + params);
		long startTime = System.currentTimeMillis();
		List<Map<String, Object>> priceOperationIncrementList = mySqlDataDao.getPriceOperationIncrement(params);
		long endTime = System.currentTimeMillis();
		int incrementListSize = (priceOperationIncrementList == null) ? 0 : priceOperationIncrementList.size();
		logger.info("use time = " + (endTime - startTime) + ",getPriceOperationIncrement, priceOperationIncrementList size = "
				+ incrementListSize);
		return priceOperationIncrementList;
	}

	/** 
	 * shotelid过滤及enddate处理
	 *
	 * @param incrRateList
	 */
	private void filterAndHandler(List<IncrRate> incrRateList) {
		logger.info("before fillFilteredSHotelsIds, incrRates size = " + incrRateList.size());
		Date validDate = DateTime.now().plusYears(1).toDate();
		long startTime = System.currentTimeMillis();
		Iterator<IncrRate> iter = incrRateList.iterator();
		Set<String> hotelCodeList = new HashSet<String>();
		Set<String> ratePlanIdList = new HashSet<String>();
		while (iter.hasNext()) {
			IncrRate incrRate = iter.next();
			if (incrRate == null) {
				iter.remove();
				continue;
			}
			incrRate.setChannel(0);
			Date endDate = incrRate.getEndDate();
			endDate = (endDate.compareTo(validDate) > 0) ? validDate : endDate;
			incrRate.setEndDate(endDate);

			hotelCodeList.add(incrRate.getHotelCode());
			ratePlanIdList.add(incrRate.getRateplanId() + "");
		}
		Map<String, String> isStraintMap = HotelDataServiceAgent.getCooperationTypeByHotelCode(hotelCodeList.toArray(new String[0]));
		Map<String, String> sellChannelMap = HotelDataServiceAgent.getSellChannelByRatePlanId(ratePlanIdList.toArray(new String[0]));
		for (IncrRate incrRate : incrRateList) {
			String hotelCode = incrRate.getHotelCode();
			String rateplanId = incrRate.getRateplanId() + "";

			String isStraint = isStraintMap.get(hotelCode);
			isStraint = StringUtils.isEmpty(isStraint) ? "0" : isStraint;

			String sellChannel = sellChannelMap.get(rateplanId);
			sellChannel = StringUtils.isEmpty(sellChannel) ? "65534" : sellChannel;
			incrRate.setIsStraint(Integer.parseInt(isStraint));
			incrRate.setSellChannel(Integer.parseInt(sellChannel));
		}
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",after fillFilteredSHotelsIds, incrRates size = " + incrRateList.size());
	}

	/** 
	 * 批量插入数据库 
	 *
	 * @param afterIncrRates
	 */
	private void builkInsert(List<IncrRate> incrRates) {
		int recordCount = incrRates.size();
		if (recordCount == 0)
			return;
		logger.info("IncrRate BulkInsert start,recordCount = " + recordCount);
		String builkInsertSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrRateInsertSizePerTask");
		int pageSize = StringUtils.isEmpty(builkInsertSize) ? 5000 : Integer.valueOf(builkInsertSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		List<MysqlRateThread> callableList = new ArrayList<MysqlRateThread>();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			MysqlRateThread mysqlRateThread = new MysqlRateThread(incrRates.subList(startNum, endNum));
			callableList.add(mysqlRateThread);
		}
		int callableListSize = callableList.size();

		// 多线程插数据
		int mysqlInsertThreadCount = ConfigUtils.getIntConfigValue("MysqlInsertThreadCount", 10);
		mysqlInsertThreadCount = callableListSize < mysqlInsertThreadCount ? callableListSize : mysqlInsertThreadCount;
		ExecutorService executorService = Executors.newFixedThreadPool(mysqlInsertThreadCount);
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
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrRate BulkInsert successfully,successCount = "
				+ successCount + ",threadCount = " + mysqlInsertThreadCount);
	}

	/**
	 * 价格元数据处理内部类
	 *
	 * <p>
	 * 修改历史:											<br>  
	 * 修改日期    		修改人员   	版本	 		修改内容<br>  
	 * -------------------------------------------------<br>  
	 * 2017年6月21日 上午10:57:44   suht     1.0    	初始化创建<br>
	 * </p> 
	 *
	 * @author		suht  
	 * @version		1.0  
	 * @since		JDK1.7
	 */
	private class GoodsPriceMetaThread implements Callable<List<IncrRate>> {

		private List<Map<String, Object>> priceOperationIncrementList;

		public GoodsPriceMetaThread(List<Map<String, Object>> priceOperationIncrementList) {
			this.priceOperationIncrementList = priceOperationIncrementList;
		}

		@Override
		public List<IncrRate> call() throws Exception {
			return getIncrRateList(priceOperationIncrementList);
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
	private class MysqlRateThread implements Callable<Integer> {

		private List<IncrRate> incrRateList;

		public MysqlRateThread(List<IncrRate> incrRateList) {
			this.incrRateList = incrRateList;
		}

		@Override
		public Integer call() throws Exception {
			return incrRateSubmeterService.builkInsert(incrRateList);
		}

	}

}
