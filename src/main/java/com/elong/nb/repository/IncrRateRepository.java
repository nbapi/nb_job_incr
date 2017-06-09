/**   
 * @(#)IncrRateRepository.java	2016年9月21日	下午4:15:22	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.sql.Timestamp;
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
		if (priceOperationIncrementList == null || priceOperationIncrementList.size() == 0)
			return changID;

		List<Long> emptyIncrRateIDList = new LinkedList<Long>();
		List<Map<String, Object>> incrRates = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> priceOperationIncrement : priceOperationIncrementList) {
			Long id = (Long) priceOperationIncrement.get("id");
			Timestamp operate_time = (Timestamp) priceOperationIncrement.get("operate_time");
			Date changeTime = new Date(operate_time.getTime());
			String hotel_id = (String) priceOperationIncrement.get("hotel_id");
			String roomtype_id = (String) priceOperationIncrement.get("roomtype_id");
			Integer rateplan_id = (Integer) priceOperationIncrement.get("rateplan_id");
			Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
			Date startDate = new Date(begin_date.getTime());
			Timestamp end_date = (Timestamp) priceOperationIncrement.get("end_date");
			Date endDate = new Date(end_date.getTime());

			Map<String, Object> incrRate = getIncrRate(id, hotel_id, startDate, endDate, roomtype_id, rateplan_id);
			if (incrRate == null) {
				emptyIncrRateIDList.add(id);
				continue;
			}
			incrRate.put("ChangeTime", changeTime);
			incrRate.put("OperateTime", changeTime);
			incrRate.put("ChangeID", id);
			incrRates.add(incrRate);
			changID = id;
		}
		logger.info("emptyIncrRateCount = " + emptyIncrRateIDList.size() + ",emptyIncrRateIDList = " + emptyIncrRateIDList);
		changID = (Long) priceOperationIncrementList.get(incrementListSize - 1).get("id");

		incrRates = filterAndHandler(incrRates);
		// 插入数据库
		int recordCount = incrRates.size();
		if (recordCount > 0) {
			int successCount = 0;
			logger.info("IncrRate BulkInsert start,recordCount = " + recordCount);
			String incrRateBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrRateBatchSize");
			int pageSize = StringUtils.isEmpty(incrRateBatchSize) ? 2000 : Integer.valueOf(incrRateBatchSize);
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			startTime = System.currentTimeMillis();
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				// int startNum = (pageIndex - 1) * pageSize;
				// int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
				// successCount += incrRateDao.bulkInsert(incrRates.subList(startNum, endNum));
			}
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",IncrRate BulkInsert successfully,successCount = " + successCount);
		}
		return changID;
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
	 * 商品库获取价格接口
	 *
	 * @param hotelCode
	 * @param startDate
	 * @param endDate
	 * @param roomtype_id
	 * @param rateplan_id
	 * @return
	 */
	private Map<String, Object> getIncrRate(Long id, String hotelCode, Date startDate, Date endDate, String roomtype_id, Integer rateplan_id) {
		List<Map<String, Object>> incrRates = null;
		GetBasePrice4NbRequest request = new GetBasePrice4NbRequest();
		request.setBooking_channel(126);
		request.setSell_channel(65534);
		request.setMember_level(30);
		request.setTraceId(UUID.randomUUID().toString() + "_" + id);
		request.setStart_date((int) (startDate.getTime() / 1000));
		request.setEnd_date((int) (endDate.getTime() / 1000));
		List<HotelBasePriceRequest> hotelBases = new LinkedList<HotelBasePriceRequest>();
		HotelBasePriceRequest hotelBase = new HotelBasePriceRequest();
		String hotelId = msRelationRepository.getMHotelId(hotelCode);
		hotelBase.setMhotel_id(Integer.valueOf(hotelId));
		hotelBase.setShotel_id(Integer.valueOf(hotelCode));
		hotelBases.add(hotelBase);
		request.setHotel_base_price_request(hotelBases);
		try {
			GetBasePrice4NbResponse response = goodsMetaRepository.getMetaPrice4Nb(request);
			if (response != null && response.return_code == 0) {
				IncrRateAdapter adapter = new IncrRateAdapter();
				incrRates = adapter.toNBObject(response);
			} else if (response.return_code > 0) {
				incrRates = new ArrayList<Map<String, Object>>();
				logger.info("ThriftUtils.getMetaPrice4Nb, response.return_code > 0,request = " + JSON.toJSONString(request) + ",response = " + JSON.toJSONString(response));
			} else {
				throw new RuntimeException(response.getReturn_msg());
			}
		} catch (Exception ex) {
			throw new RuntimeException("IncrRate:" + ex.getMessage(), ex);
		}

		for (Map<String, Object> incrRate : incrRates) {
			if (incrRate == null)
				continue;
			String RoomTypeId = (String) incrRate.get("RoomTypeID");
			Integer RateplanId = (Integer) incrRate.get("RateplanID");
			if (StringUtils.isEmpty(RoomTypeId) || RateplanId == null)
				continue;
			if (StringUtils.equals(roomtype_id, RoomTypeId) && rateplan_id.intValue() == RateplanId.intValue()) {
				return incrRate;
			}
		}
		return null;
	}

}
