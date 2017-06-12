/**   
 * @(#)IncrRateRepository.java	2016年9月21日	下午4:15:22	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.dao.adataper.IncrRateAdapter;
import com.elong.nb.service.IFilterService;
import com.elong.nb.util.DateHandlerUtils;

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

	@Resource
	private IncrRateDao incrRateDao;

	@Resource
	private MySqlDataDao mySqlDataDao;

	@Resource
	private IFilterService filterService;

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
		String goodsMetaPriceMaxDaysStr = CommonsUtil.CONFIG_PROVIDAR.getProperty("GoodsMetaPriceMaxDays");
		int GoodsMetaPriceMaxDays = StringUtils.isEmpty(goodsMetaPriceMaxDaysStr) ? 180 : Integer.valueOf(goodsMetaPriceMaxDaysStr);
		Date validDate = DateTime.now().plusDays(GoodsMetaPriceMaxDays).toDate();
		try {
			validDate = DateHandlerUtils.convertDateParttern(validDate, "yyyy-MM-dd");
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(e.getMessage());
		}
		// 过滤掉最大有效日期之外数据
		List<Map<String, Object>> filterPriceOperationIncrementList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			if (startDate.compareTo(validDate) > 0)
				continue;
			filterPriceOperationIncrementList.add(priceOperationIncrement);
		}
		logger.info("after filter by validDate[" + validDate + "],PriceOperationIncrementList size = "
				+ filterPriceOperationIncrementList.size());

		// 分批次批量调用商品库价格元数据接口
		List<Map<String, Object>> beforeIncrRates = new ArrayList<Map<String, Object>>();
		String goodsRateBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("GoodsRateBatchSize");
		int recordCount = filterPriceOperationIncrementList.size();
		int batchSize = StringUtils.isEmpty(goodsRateBatchSize) ? 10 : Integer.valueOf(goodsRateBatchSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / batchSize);
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			long startTime = System.currentTimeMillis();
			int startNum = (pageIndex - 1) * batchSize;
			int endNum = pageIndex * batchSize > recordCount ? recordCount : pageIndex * batchSize;
			List<Map<String, Object>> incrRates = getIncrRateList(filterPriceOperationIncrementList.subList(startNum, endNum), validDate);
			beforeIncrRates.addAll(incrRates);
			long endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",startNum = " + startNum + ",endNum = " + endNum + ",incrRates size = "
					+ incrRates.size());
		}
		// shotelid过滤及enddate处理
		List<Map<String, Object>> afterIncrRates = filterAndHandler(beforeIncrRates);
		// 插入数据库
		builkInsert(afterIncrRates);
		return (Long) priceOperationIncrementList.get(priceOperationIncrementList.size() - 1).get("id");
	}

	/** 
	 * 批量调用价格元数据
	 *
	 * @param hotelBases
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private List<Map<String, Object>> getRatesFromGoods(List<HotelBasePriceRequest> hotelBases, Date startDate, Date endDate) {
		List<Map<String, Object>> incrRates = null;
		GetBasePrice4NbRequest request = new GetBasePrice4NbRequest();
		request.setBooking_channel(126);
		request.setSell_channel(65534);
		request.setMember_level(30);
		request.setTraceId(UUID.randomUUID().toString());
		request.setStart_date((int) (startDate.getTime() / 1000));
		request.setEnd_date((int) (endDate.getTime() / 1000));
		request.setHotel_base_price_request(hotelBases);
		try {
			GetBasePrice4NbResponse response = goodsMetaRepository.getMetaPrice4Nb(request);
			if (response != null && response.return_code == 0) {
				IncrRateAdapter adapter = new IncrRateAdapter();
				incrRates = adapter.toNBObject(response);
				if (incrRates == null || incrRates.size() == 0) {
					logger.error("ThriftUtils.getMetaPrice4Nb,response.return_code = 0,incrRates size = 0,request = "
							+ JSON.toJSONString(request) + ",response = " + JSON.toJSONString(response));
				}
			} else if (response.return_code > 0) {
				incrRates = new ArrayList<Map<String, Object>>();
				logger.info("ThriftUtils.getMetaPrice4Nb, response.return_code > 0,request = " + JSON.toJSONString(request)
						+ ",response = " + JSON.toJSONString(response));
			} else {
				throw new RuntimeException(response.getReturn_msg());
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
	private List<Map<String, Object>> getIncrRateList(List<Map<String, Object>> priceOperationIncrementList, Date validDate) {
		List<HotelBasePriceRequest> hotelBases = new LinkedList<HotelBasePriceRequest>();
		Date minStartDate = null;
		Date maxEndDate = null;
		List<String> hotelCodeList = new ArrayList<String>();
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			String hotelCode = (String) priceOperationIncrement.get("hotel_id");
			String hotelId = msRelationRepository.getMHotelId(hotelCode);
			if(!hotelCodeList.contains(hotelCode)){
				HotelBasePriceRequest hotelBase = new HotelBasePriceRequest();
				hotelBase.setMhotel_id(Integer.valueOf(hotelId));
				hotelBase.setShotel_id(Integer.valueOf(hotelCode));
				hotelBases.add(hotelBase);
				hotelCodeList.add(hotelCode);
			}
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
		}
		Map<String, List<Map<String, Object>>> groupRateMap = new HashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> goodsRateList = getRatesFromGoods(hotelBases, minStartDate, maxEndDate);
		for (Map<String, Object> goodsRate : goodsRateList) {
			if (goodsRate == null || goodsRate.size() == 0)
				continue;
			String hotelCode = (String) goodsRate.get("HotelCode");
			String roomTypeID = (String) goodsRate.get("RoomTypeID");
			Integer rateplanID = (Integer) goodsRate.get("RateplanID");
			String key = hotelCode + "|" + roomTypeID + "|" + rateplanID;
			List<Map<String, Object>> groupList = groupRateMap.get(key);
			if (groupList == null) {
				groupList = new ArrayList<Map<String, Object>>();
			}
			groupList.add(goodsRate);
			groupRateMap.put(key, groupList);
		}
		List<Map<String, Object>> incrRates = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			Long id = (Long) priceOperationIncrement.get("id");
			String hotelCode = (String) priceOperationIncrement.get("hotel_id");
			String roomTypeID = (String) priceOperationIncrement.get("roomtype_id");
			Integer rateplanID = (Integer) priceOperationIncrement.get("rateplan_id");
			String key = hotelCode + "|" + roomTypeID + "|" + rateplanID;
			List<Map<String, Object>> groupList = groupRateMap.get(key);
			if (groupList == null || groupList.size() == 0)
				continue;
			
			Timestamp operate_time = (Timestamp) priceOperationIncrement.get("operate_time");
			Date changeTime = new Date(operate_time.getTime());
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			Timestamp end_date = (Timestamp) priceOperationIncrement.get("end_date");
			Date endDate = new Date(end_date.getTime());
			for (Map<String, Object> goodsRate : groupList) {
				Date goodsStartDate = (Date) goodsRate.get("StartDate");
				Date goodsEndDate = (Date) goodsRate.get("EndDate");
				// 日期没匹配上，过滤掉
				if (startDate.after(goodsEndDate) || endDate.before(goodsStartDate))
					continue;
				Date finalStartDate = startDate.after(goodsStartDate) ? startDate : goodsStartDate;
				Date finalEndDate = endDate.before(goodsEndDate) ? endDate : goodsEndDate;
				goodsRate.put("StartDate", finalStartDate);
				goodsRate.put("EndDate", finalEndDate);
				goodsRate.put("ChangeTime", changeTime);
				goodsRate.put("OperateTime", changeTime);
				goodsRate.put("ChangeID", id);
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
		// 延迟3分钟
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
	 * @return
	 */
	private List<Map<String, Object>> filterAndHandler(List<Map<String, Object>> incrRateList) {
		logger.info("before fillFilteredSHotelsIds, incrRates size = " + incrRateList.size());
		List<Map<String, Object>> incrRates = new ArrayList<Map<String, Object>>();
		Date validDate = DateTime.now().plusYears(1).toDate();
		long startTime = System.currentTimeMillis();
		Set<String> filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
		for (Map<String, Object> rowMap : incrRateList) {
			if (rowMap == null)
				continue;

			String shotelId = (String) rowMap.get("HotelCode");
			if (filteredSHotelIds.contains(shotelId)) {
				continue;
			}
			Date endDate = (Date) rowMap.get("EndDate");
			endDate = (endDate.compareTo(validDate) > 0) ? validDate : endDate;
			rowMap.put("EndDate", endDate);

			incrRates.add(rowMap);
		}
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",after fillFilteredSHotelsIds, incrRates size = " + incrRates.size());
		return incrRates;
	}
	
	/** 
	 * 批量插入数据库 
	 *
	 * @param afterIncrRates
	 */
	private void builkInsert(List<Map<String, Object>> afterIncrRates) {
		int recordCount = afterIncrRates.size();
		if (recordCount == 0)
			return;
		int successCount = 0;
		logger.info("IncrRate BulkInsert start,recordCount = " + recordCount);
		String incrRateBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrRateBatchSize");
		int pageSize = StringUtils.isEmpty(incrRateBatchSize) ? 2000 : Integer.valueOf(incrRateBatchSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		long startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			 int startNum = (pageIndex - 1) * pageSize;
			 int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			 successCount += incrRateDao.bulkInsert(afterIncrRates.subList(startNum, endNum));
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrRate BulkInsert successfully,successCount = "
				+ successCount);
	}

}
