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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.IProductForPartnerServiceContract;
import com.elong.nb.agent.ProductForPartnerServiceContract.ResourceInventoryState;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.model.GetInvLimitDataRequest;
import com.elong.nb.model.GetInvLimitResponse;
import com.elong.nb.model.RequestBase;
import com.elong.nb.model.ResponseBase;
import com.elong.nb.model.domain.InvLimitBlackListVo;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.repository.MSRelationRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.util.DateHandlerUtils;
import com.elong.nb.util.ExecutorUtils;
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

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@Resource
	private IncrInventoryRepository incrInventoryRepository;

	@Resource
	private IProductForPartnerServiceContract productForPartnerServiceContract;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

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
	@Override
	public void syncInventoryToDB(long changeID, long beginTime) {
		if (changeID == 0) {
			long startTime = System.currentTimeMillis();
			String setValue = incrSetInfoService.get(RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey());
			setValue = StringUtils.isEmpty(setValue) ? redisManager.getStr(RedisKeyConst.CacheKey_KEY_Inventory_LastID) : setValue;
			changeID = StringUtils.isEmpty(setValue) ? 0 : Long.valueOf(setValue);
			long endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",get value from redis key = "
					+ RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey() + ",changeID = " + changeID);
		}
		if (changeID == 0) {
			long startTime = System.currentTimeMillis();
			changeID = incrInventoryRepository.getInventoryChangeMinID(DateHandlerUtils.getCacheExpireDate());
			logger.info("use time = " + (System.currentTimeMillis() - startTime)
					+ ",incrInventoryRepository.getInventoryChangeMinID,changeID = " + changeID);
		}
		long startTime = System.currentTimeMillis();
		long newLastChgID = incrInventoryRepository.syncInventoryToDB(changeID);
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

	@Override
	public void delInventoryFromDB() {
		// 删除30小时以前的数据
		long startTime = System.currentTimeMillis();
		int count = incrInventoryRepository.deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
		logger.info("IncrInventory delete successfully.count = " + count);
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",IncrInventory delete successfully.count = " + count);
	}

	@Override
	public void syncInventoryDueToBlack() {
		String rediskey = "Incr.Inventory.Time";
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

		RequestBase<GetInvLimitDataRequest> requestBase = new RequestBase<GetInvLimitDataRequest>();
		requestBase.setFrom("nb_job_incr");
		requestBase.setLogId(UUID.randomUUID().toString());
		GetInvLimitDataRequest realRequest = new GetInvLimitDataRequest();
		realRequest.setPageSize(1000);
		realRequest.setTimestamp(blackStartTime);

		Map<String, InvLimitBlackListVo> sourceMap = new HashMap<String, InvLimitBlackListVo>();
		int i = 0;
		List<InvLimitBlackListVo> invLimitList = null;
		while (i == 0 || !CollectionUtils.isEmpty(invLimitList)) {
			realRequest.setStartIndex(1000 * (i++));
			requestBase.setRealRequest(realRequest);

			invLimitList = getInvLimitBlackList(requestBase);
			if (invLimitList == null || invLimitList.size() == 0)
				continue;

			for (InvLimitBlackListVo vo : invLimitList) {
				if (vo == null)
					continue;
				String key = vo.getHotelId() + "#" + vo.getStayBeginDate() + "#" + vo.getStayEndDate();
				sourceMap.put(key, vo);
			}
		}
		logger.info("syncInventoryDueToBlack,invLimitList after norepeat size = " + sourceMap.size());
		if (sourceMap.size() == 0)
			return;

		// // 最大支持300线程并行
		int maximumPoolSize = sourceMap.size() < 300 ? sourceMap.size() : 300;
		logger.info("syncInventoryDueToBlack,maximumPoolSize = " + maximumPoolSize);
		long startTime = System.currentTimeMillis();
		ExecutorService executorService = ExecutorUtils.newSelfThreadPool(maximumPoolSize, 400);
		final List<Map<String, Object>> rows = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
		for (final Map.Entry<String, InvLimitBlackListVo> entry : sourceMap.entrySet()) {
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						syncInventoryToDB(entry.getValue(), rows);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			});
		}
		long endTime = System.currentTimeMillis();
		logger.info("syncInventoryDueToBlack,use time = " + (endTime - startTime) + ",executorService submit task");
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}

		// 存数据库
		int recordCount = rows.size();
		if (recordCount == 0)
			return;
		int successCount = 0;
		logger.info("syncInventoryDueToBlack,IncrInventory BulkInsert start,recordCount = " + rows.size());
		String incrInventoryBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrInventoryBatchSize");
		int pageSize = StringUtils.isEmpty(incrInventoryBatchSize) ? 2000 : Integer.valueOf(incrInventoryBatchSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			successCount += incrInventoryDao.bulkInsert(rows.subList(startNum, endNum));
		}
		endTime = System.currentTimeMillis();
		logger.info("syncInventoryDueToBlack,use time = " + (endTime - startTime) + ",IncrInventory BulkInsert,successCount = "
				+ successCount);

		blackStartTime = new Date();
		incrSetInfoService.put(rediskey, blackStartTime);
		logger.info("syncInventoryDueToBlack,put to redis successfully.key = " + rediskey + ",value = " + blackStartTime);
	}

	private void syncInventoryToDB(InvLimitBlackListVo vo, List<Map<String, Object>> rows) {
		if (vo == null || StringUtils.isEmpty(vo.getHotelId()) || vo.getStayBeginDate() == null || vo.getStayEndDate() == null)
			return;
		String mHotelId = this.msRelationRepository.getMHotelId(vo.getHotelId());
		GetInventoryChangeDetailRequest request = new GetInventoryChangeDetailRequest();
		request.setHotelID(vo.getHotelId());
		request.setBeginTime(new DateTime(vo.getStayBeginDate().getTime()));
		request.setEndTime(new DateTime(vo.getStayEndDate()));
		long startTime = System.currentTimeMillis();
		GetInventoryChangeDetailResponse response = null;
		try {
			response = productForPartnerServiceContract.getInventoryChangeDetail(request);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
		long endTime = System.currentTimeMillis();
		String threadName = Thread.currentThread().getName();
		logger.info("syncInventoryDueToBlack,use time [" + threadName + "] = " + (endTime - startTime)
				+ ",productForPartnerServiceContract.getInventoryChangeDetail2");

		List<ResourceInventoryState> resourceInventoryStateList = null;
		if (response != null && response.getResourceInventoryStateList() != null) {
			resourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
		}
		if (resourceInventoryStateList == null || resourceInventoryStateList.size() == 0)
			return;

		for (ResourceInventoryState detail : resourceInventoryStateList) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("HotelID", mHotelId);
			row.put("RoomTypeID", detail.getRoomTypeID().length() > 50 ? detail.getRoomTypeID().substring(0, 50) : detail.getRoomTypeID());
			row.put("HotelCode", detail.getHotelID());
			row.put("Status", detail.getStatus() == 0);
			row.put("AvailableDate", detail.getAvailableTime() == null ? null : detail.getAvailableTime().toDate());
			row.put("AvailableAmount", detail.getAvailableAmount());
			row.put("OverBooking", detail.getIsOverBooking());
			row.put("StartDate", detail.getBeginDate() == null ? null : detail.getBeginDate().toDate());
			row.put("EndDate", detail.getEndDate() == null ? null : detail.getEndDate().toDate());
			row.put("StartTime", detail.getBeginTime());
			row.put("EndTime", detail.getEndTime());
			row.put("OperateTime", detail.getOperateTime() == null ? null : detail.getOperateTime().toDate());
			row.put("InsertTime", DateTime.now().toDate());
			row.put("ChangeID", DateTime.now().toDate().getTime());
			row.put("ChangeTime", detail.getOperateTime() == null ? null : detail.getOperateTime().toDate());
			rows.add(row);
		}
	}

	private List<InvLimitBlackListVo> getInvLimitBlackList(RequestBase<GetInvLimitDataRequest> requestBase) {
		String ruleUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetInvLimitDataUrl");
		ruleUrl = StringUtils.isEmpty(ruleUrl) ? "http://192.168.233.40:9014/api/Hotel/GetInvLimitData" : ruleUrl;
		String reqData = JSON.toJSONString(requestBase);
		String result = null;
		try {
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

}
