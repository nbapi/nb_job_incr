/**   
 * @(#)ManualController.java	2017年4月21日	下午5:30:18	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbRequest;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbResponse;
import com.elong.hotel.goods.ds.thrift.HotelBasePriceRequest;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.dao.adataper.IncrRateAdapter;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.repository.GoodsMetaRepository;
import com.elong.nb.repository.IncrRateRepository;
import com.elong.nb.repository.MSRelationRepository;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.submeter.service.ISubmeterService;

/**
 * 手动修改某些值
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 下午5:30:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class ManualController {

	private static final Logger incrRateLogger = Logger.getLogger("IncrRateLogger");

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@Resource
	private IImpulseSenderService impulseSenderService;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	@Resource
	private IncrRateRepository incrRateRepository;

	@Resource
	private GoodsMetaRepository goodsMetaRepository;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private MySqlDataDao mySqlDataDao;

	@RequestMapping(value = "/test/goodsMetaPrice/{id}")
	public @ResponseBody String goodsMetaPrice(@PathVariable("id") Long id) {
		Map<String, Object> priceOperationIncrement = mySqlDataDao.getPriceOperationIncrementByid(id);
		Timestamp operate_time = (Timestamp) priceOperationIncrement.get("operate_time");
		Date changeTime = new Date(operate_time.getTime());
		String hotelCode = (String) priceOperationIncrement.get("hotel_id");
		String roomtype_id = (String) priceOperationIncrement.get("roomtype_id");
		Integer rateplan_id = (Integer) priceOperationIncrement.get("rateplan_id");
		Timestamp begin_date = (Timestamp) priceOperationIncrement.get("begin_date");
		Date startDate = new Date(begin_date.getTime());
		Timestamp end_date = (Timestamp) priceOperationIncrement.get("end_date");
		Date endDate = new Date(end_date.getTime());

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
		GetBasePrice4NbResponse response = null;
		try {
			incrRateLogger.info("request = " + JSON.toJSONString(request));
			response = goodsMetaRepository.getMetaPrice4Nb(request);
			incrRateLogger.info("response = " + JSON.toJSONString(response));
			if (response != null && response.return_code == 0) {
				IncrRateAdapter adapter = new IncrRateAdapter();
				incrRates = adapter.toNBObject(response);

			} else if (response.return_code > 0) {
				incrRates = new ArrayList<Map<String, Object>>();
			} else {
				throw new RuntimeException(response.getReturn_msg());
			}
		} catch (Exception ex) {
			throw new RuntimeException("IncrRate:" + ex.getMessage(), ex);
		}
		Map<String, Object> result = null;
		for (Map<String, Object> incrRate : incrRates) {
			if (incrRate == null)
				continue;
			String RoomTypeId = (String) incrRate.get("RoomTypeID");
			Integer RateplanId = (Integer) incrRate.get("RateplanID");
			if (StringUtils.isEmpty(RoomTypeId) || RateplanId == null)
				continue;
			if (StringUtils.equals(roomtype_id, RoomTypeId) && rateplan_id.intValue() == RateplanId.intValue()) {
				result = incrRate;
			}
		}
		if (result != null) {
			result.put("ChangeTime", changeTime);
			result.put("OperateTime", changeTime);
			result.put("ChangeID", id);
		}
		return JSON.toJSONString(result);
	}

	/** 
	 * 设置分表开始序号(分表上线时初始化分表开始序号)
	 *
	 * @param tablePrefix
	 * @param subTableNumber (比如分表序号要从100开始，则subTableNumber设置为99)
	 * @return
	 */
	@RequestMapping(value = "/test/putSubTableNumber/{tablePrefix}/{subTableNumber}")
	public @ResponseBody String putSubTableNumber(@PathVariable("tablePrefix") String tablePrefix,
			@PathVariable("subTableNumber") String subTableNumber) {
		try {
			incrSetInfoService.put(tablePrefix + ".SubTable.Number", Long.valueOf(subTableNumber));
		} catch (Exception e) {
			return "putSubTableNumber error = " + e.getMessage() + ",tablePrefix = " + tablePrefix + ",subTableNumber = " + subTableNumber;
		}
		return "putSubTableNumber success.tablePrefix = " + tablePrefix + ",subTableNumber = " + subTableNumber;
	}

	/** 
	 * 设置增量配置信息
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	@RequestMapping(value = "/test/putIncrSetInfo/{key}/{value}")
	public @ResponseBody String putIncrSetInfo(@PathVariable("key") String key, @PathVariable("value") String value) {
		try {
			incrSetInfoService.put(key, Long.valueOf(value));
		} catch (Exception e) {
			return "putIncrSetInfo error = " + e.getMessage() + ",key = " + key + ",value = " + value;
		}
		return "putIncrSetInfo success.key = " + key + ",value = " + value;
	}

	/** 
	 * 设置发号器id(分表上线时初始化发号器id)
	 *
	 * @param tablePrefix
	 * @param idVal
	 * @return
	 */
	@RequestMapping(value = "/test/initImpulseSender/{tablePrefix}/{idVal}")
	public @ResponseBody String initImpulseSender(@PathVariable("tablePrefix") String tablePrefix, @PathVariable("idVal") String idVal) {
		String key = tablePrefix + "_ID";
		try {
			Long idLong = Long.valueOf(idVal);
			impulseSenderService.putId(tablePrefix + "_ID", idLong);
		} catch (Exception e) {
			return "initImpulseSender error = " + e.getMessage();
		}
		return "initImpulseSender success.key = " + key + ",value = " + idVal;
	}

	/** 
	 * 查看发号器当前id
	 *
	 * @param tablePrefix
	 * @return
	 */
	@RequestMapping(value = "/test/getImpulseSenderID/{tablePrefix}")
	public @ResponseBody String getImpulseSenderID(@PathVariable("tablePrefix") String tablePrefix) {
		String key = tablePrefix + "_ID";
		try {
			long impulseSenderId = impulseSenderService.curId(tablePrefix + "_ID");
			return "getImpulseSenderID success.key = " + key + ",value = " + impulseSenderId;
		} catch (Exception e) {
			return "getImpulseSenderID error = " + e.getMessage() + ",key = " + key;
		}
	}

	/** 
	 * 清除缓存分表表名数据 
	 *
	 * @param tablePrefix
	 * @return
	 */
	@RequestMapping(value = "/test/delSubmeterTableCache/{tablePrefix}")
	public @ResponseBody String delSubmeterTableCache(@PathVariable("tablePrefix") String tablePrefix) {
		ICacheKey cacheKey = RedisManager.getCacheKey(tablePrefix + ".Submeter.TableNames");
		try {
			// 清除老数据
			redisManager.del(cacheKey);
		} catch (Exception e) {
			return "delSubmeterTableCache error = " + e.getMessage() + ",tablePrefix = " + tablePrefix;
		}
		return "delSubmeterTableCache success.tablePrefix = " + tablePrefix;
	}

}
