/**   
 * @(#)IncrInventoryServiceImpl.java	2016年9月21日	下午2:19:20	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.IProductForPartnerServiceContract;
import com.elong.nb.agent.ProductForPartnerServiceContract.ResourceInventoryState;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.model.GetInvLimitDataRequest;
import com.elong.nb.model.GetInvLimitResponse;
import com.elong.nb.model.RequestBase;
import com.elong.nb.model.ResponseBase;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.domain.InvLimitBlackListVo;
import com.elong.nb.repository.CommonRepository;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.repository.MSRelationRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.DateHandlerUtils;
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
	private IncrInventoryRepository incrInventoryRepository;

	@Resource
	private IProductForPartnerServiceContract productForPartnerServiceContract;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

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
		startTime = System.currentTimeMillis();
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
	public void syncInventoryDueToBlack() {
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

		RequestBase<GetInvLimitDataRequest> requestBase = new RequestBase<GetInvLimitDataRequest>();
		requestBase.setFrom("nb_job_incr");
		requestBase.setLogId(UUID.randomUUID().toString());
		GetInvLimitDataRequest realRequest = new GetInvLimitDataRequest();
		realRequest.setPageSize(1000);
		realRequest.setTimestamp(blackStartTime);

		Set<String> filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
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
				if (vo == null || filteredSHotelIds.contains(vo.getHotelId()))
					continue;
				String key = vo.getHotelId() + "#" + vo.getStayBeginDate() + "#" + vo.getStayEndDate();
				sourceMap.put(key, vo);
			}
		}
		logger.info("syncInventoryDueToBlack,invLimitList after norepeat size = " + sourceMap.size());
		blackStartTime = new Date();
		if (sourceMap.size() == 0)
			return;

		// // 最大支持300线程并行
		long startTime = System.currentTimeMillis();
		List<IncrInventory> rows = new ArrayList<IncrInventory>();
		for (Map.Entry<String, InvLimitBlackListVo> entry : sourceMap.entrySet()) {
			try {
				syncInventoryToDB(entry.getValue(), rows);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		long endTime = System.currentTimeMillis();
		logger.info("syncInventoryDueToBlack,use time = " + (endTime - startTime) + ",syncInventoryToDB");

		// 存数据库
		int recordCount = rows.size();
		if (recordCount == 0)
			return;

		startTime = System.currentTimeMillis();
		Collections.sort(rows, new Comparator<IncrInventory>() {
			@Override
			public int compare(IncrInventory o1, IncrInventory o2) {
				if (o1 == null && o2 == null)
					return 0;
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				Date o1ChangeTimeObj = o1.getChangeTime();
				Date o2ChangeTimeObj = o2.getChangeTime();
				if (o1ChangeTimeObj == null && o2ChangeTimeObj == null)
					return 0;
				if (o1ChangeTimeObj == null)
					return -1;
				if (o2ChangeTimeObj == null)
					return 1;
				Date o1ChangeTime = (Date) o1ChangeTimeObj;
				Date o2ChangeTime = (Date) o2ChangeTimeObj;
				return o1ChangeTime.equals(o2ChangeTime) ? 0 : (o1ChangeTime.before(o2ChangeTime) ? -1 : 1);
			}
		});
		endTime = System.currentTimeMillis();
		logger.info("syncInventoryDueToBlack，use time = " + (endTime - startTime) + ",sort rowMap by ChangeID");

		logger.info("syncInventoryDueToBlack,IncrInventory BulkInsert start,recordCount = " + rows.size());
		startTime = System.currentTimeMillis();
		int successCount = incrInventorySubmeterService.builkInsert(rows);
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrInventory BulkInsert,successCount = " + successCount);
		endTime = System.currentTimeMillis();
		logger.info("syncInventoryDueToBlack,use time = " + (endTime - startTime) + ",IncrInventory BulkInsert,successCount = "
				+ successCount);

		incrSetInfoService.put(rediskey, blackStartTime);
		logger.info("syncInventoryDueToBlack,put to redis successfully.key = " + rediskey + ",value = " + blackStartTime);
	}

	private void syncInventoryToDB(InvLimitBlackListVo vo, List<IncrInventory> rows) {
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
			Date changeTime = detail.getOperateTime() == null ? null : detail.getOperateTime().toDate();
			if (changeTime == null || DateHandlerUtils.getDBExpireDate(-10).after(changeTime))
				continue;
			IncrInventory row = new IncrInventory();
			row.setHotelID(mHotelId);
			row.setRoomTypeID(detail.getRoomTypeID().length() > 50 ? detail.getRoomTypeID().substring(0, 50) : detail.getRoomTypeID());
			row.setHotelCode(detail.getHotelID());
			row.setStatus(detail.getStatus() == 0);
			row.setAvailableDate(detail.getAvailableTime() == null ? null : detail.getAvailableTime().toDate());
			row.setAvailableAmount(detail.getAvailableAmount());
			row.setOverBooking(detail.getIsOverBooking());
			row.setStartDate(detail.getBeginDate() == null ? null : detail.getBeginDate().toDate());
			row.setEndDate(detail.getEndDate() == null ? null : detail.getEndDate().toDate());
			row.setStartTime(detail.getBeginTime());
			row.setEndTime(detail.getEndTime());
			row.setOperateTime(detail.getOperateTime() == null ? null : detail.getOperateTime().toDate());
			row.setInsertTime(DateTime.now().toDate());
			row.setChangeID(DateTime.now().toDate().getTime());
			row.setChangeTime(changeTime);
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
