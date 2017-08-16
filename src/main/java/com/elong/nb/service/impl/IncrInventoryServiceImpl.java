/**   
 * @(#)IncrInventoryServiceImpl.java	2016年9月21日	下午2:19:20	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.model.GetInvLimitDataRequest;
import com.elong.nb.model.GetInvLimitResponse;
import com.elong.nb.model.RequestBase;
import com.elong.nb.model.ResponseBase;
import com.elong.nb.model.domain.InvLimitBlackListVo;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.util.ConfigUtils;
import com.elong.nb.util.HttpClientUtils;

/**
 * IncrInventory服务接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:19:20   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class IncrInventoryServiceImpl implements IIncrInventoryService {

	private static final Logger logger = Logger.getLogger("IncrInventoryLogger");

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	@Resource
	private IncrInventoryRepository incrInventoryRepository;

	@Resource
	private MySqlDataDao mySqlDataDao;

	/** 
	 * 同步库存增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#SyncInventoryToDB()    
	 */
	@Override
	public void syncInventoryToDB() {
		// 递归同步数据
		syncInventoryToDB(0, System.currentTimeMillis());
	}

	/** 
	 * 递归，根据changeID同步库存增量
	 *
	 * @param changeID 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#SyncInventoryToDB(long)    
	 */
	private void syncInventoryToDB(long changeID, long beginTime) {
		long startTime = System.currentTimeMillis();
		if (changeID == 0) {
			String setValue = incrSetInfoService.get(RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey());
			changeID = StringUtils.isEmpty(setValue) ? 0 : Long.valueOf(setValue);
			logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",get value from redis key = "
					+ RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey() + ",changeID = " + changeID);
		}
		// 库存变化流水表获取数据
		List<Map<String, Object>> productInventoryIncrementList = getProductInventoryIncrement(changeID);
		startTime = System.currentTimeMillis();
		long newLastChgID = incrInventoryRepository.syncInventoryToDB(productInventoryIncrementList);
		newLastChgID = (newLastChgID == -1) ? changeID : newLastChgID;
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",syncInventoryToDB,change: from " + changeID + " to "
				+ newLastChgID);

		long incred = newLastChgID - changeID;
		if (incred > 0) {
			// 更新LastID
			startTime = System.currentTimeMillis();
			incrSetInfoService.put(RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey(), newLastChgID);
			long endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",put to redis key" + ",incred = " + incred + ",key = "
					+ RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey() + ",value = " + newLastChgID);
			if (incred > 100 && (endTime - beginTime) < 10 * 60 * 1000) {
				// 继续执行
				syncInventoryToDB(newLastChgID, beginTime);
			}
		}
	}

	/** 
	 * 库存关房增量同步
	 * 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#syncInventoryDueToBlack()    
	 */
	@Override
	public void syncInventoryDueToBlack() {
		// 获取上次同步时间，作为此次开始时间
		String rediskey = "Submeter.Incr.Inventory.Time";
		String jsonStr = incrSetInfoService.get(rediskey);
		Date blackStartTime = null;
		try {
			blackStartTime = JSON.parseObject(jsonStr, Date.class);
		} catch (Exception e) {
		}
		if (blackStartTime == null) {
			blackStartTime = DateUtils.addDays(new Date(), -30);
		}
		logger.info("syncInventoryDueToBlack,get startTime = " + blackStartTime + ",from redis key = " + rediskey);

		long startTime = System.currentTimeMillis();
		RequestBase<GetInvLimitDataRequest> requestBase = new RequestBase<GetInvLimitDataRequest>();
		requestBase.setFrom("nb_job_incr");
		requestBase.setLogId(UUID.randomUUID().toString());
		GetInvLimitDataRequest realRequest = new GetInvLimitDataRequest();
		realRequest.setPageSize(1000);
		realRequest.setTimestamp(blackStartTime);
		// 分页获取InvLimitBlackList数据
		int i = 0;
		List<InvLimitBlackListVo> allInvLimitList = new ArrayList<InvLimitBlackListVo>();
		List<InvLimitBlackListVo> invLimitList = null;
		while (i == 0 || !CollectionUtils.isEmpty(invLimitList)) {
			realRequest.setStartIndex(1000 * (i++));
			requestBase.setRealRequest(realRequest);
			invLimitList = getInvLimitBlackList(requestBase);
			if (invLimitList == null || invLimitList.size() == 0)
				continue;
			allInvLimitList.addAll(invLimitList);
		}
		logger.info("syncInventoryDueToBlack,use time = " + (System.currentTimeMillis() - startTime)
				+ ",get allInvLimitList from nb_web_rule,size = " + allInvLimitList.size());
		blackStartTime = new Date();
		// 同步库存增量
		List<Map<String, Object>> productInventoryIncrementList = getProductInventoryIncrement(allInvLimitList);
		startTime = System.currentTimeMillis();
		incrInventoryRepository.syncInventoryToDB(productInventoryIncrementList);
		logger.info("syncInventoryDueToBlack,use time = " + (System.currentTimeMillis() - startTime)
				+ ",incrInventoryRepository.syncInventoryToDB");
		// 同步时间存入redis
		incrSetInfoService.put(rediskey, blackStartTime);
		logger.info("syncInventoryDueToBlack,put to redis successfully.key = " + rediskey + ",value = " + blackStartTime);
	}

	/** 
	 * 构建库存变化数据参数
	 *
	 * @param invLimitList
	 * @return
	 */
	private List<Map<String, Object>> getProductInventoryIncrement(List<InvLimitBlackListVo> invLimitList) {
		if (invLimitList == null || invLimitList.size() == 0)
			return Collections.emptyList();
		List<Map<String, Object>> productInventoryIncrementList = new ArrayList<Map<String, Object>>();
		for (InvLimitBlackListVo invLimitBlackListVo : invLimitList) {
			if (invLimitBlackListVo == null)
				continue;
			Map<String, Object> productInventoryIncrement = new HashMap<String, Object>();
			productInventoryIncrement.put("id", invLimitBlackListVo.getId());
			productInventoryIncrement.put("mhotel_id", invLimitBlackListVo.getmHotelId());
			productInventoryIncrement.put("hotel_id", invLimitBlackListVo.getHotelId());
			productInventoryIncrement.put("room_type_id", invLimitBlackListVo.getmRoomTypeId());
			productInventoryIncrement.put("begin_date", invLimitBlackListVo.getStayBeginDate());
			productInventoryIncrement.put("end_date", invLimitBlackListVo.getStayEndDate());
			productInventoryIncrement.put("op_date", invLimitBlackListVo.getOperateTime());
			productInventoryIncrementList.add(productInventoryIncrement);
		}
		logger.info("syncInventoryDueToBlack,getProductInventoryIncrement,productInventoryIncrementList size = "
				+ productInventoryIncrementList.size());
		return productInventoryIncrementList;
	}

	/** 
	 * 获取InvLimitBlackList数据from nb_web_rule
	 *
	 * @param requestBase
	 * @return
	 */
	private List<InvLimitBlackListVo> getInvLimitBlackList(RequestBase<GetInvLimitDataRequest> requestBase) {
		String ruleUrl = ConfigUtils.getStringConfigValue("GetInvLimitDataUrl", "http://192.168.233.40:9014/api/Hotel/GetInvLimitData");
		String result = null;
		try {
			String reqData = JSON.toJSONString(requestBase);
			result = HttpClientUtils.httpPost(ruleUrl, reqData, "application/json");
		} catch (Exception e) {
			logger.error("syncInventoryDueToBlack,GetInvLimitData,httpPost error = " + e.getMessage(), e);
			throw new IllegalStateException("GetInvLimitData,httpPost error = " + e.getMessage());
		}
		if (StringUtils.isEmpty(result))
			return Collections.emptyList();
		ResponseBase<?> responseBase = JSON.parseObject(result, ResponseBase.class);
		if (!StringUtils.equals("0", responseBase.getResponseCode()))
			return Collections.emptyList();
		JSONObject jsonObj = (JSONObject) responseBase.getRealResponse();
		GetInvLimitResponse realResponse = JSONObject.parseObject(jsonObj.toJSONString(), GetInvLimitResponse.class);
		return realResponse.getInvLimitList();
	}

	/** 
	 * 库存变化流水表获取数据from产品数据库
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

}
