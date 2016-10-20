/**   
 * @(#)IncrStateServiceImpl.java	2016年9月21日	下午2:50:24	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.repository.IncrStateRepository;
import com.elong.nb.service.IIncrStateService;
import com.elong.nb.util.DateHandlerUtils;

/**
 * IncrState服务接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:50:24   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class IncrStateServiceImpl implements IIncrStateService {

	private static final Logger logger = Logger.getLogger("IncrStateLogger");

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@Resource
	private IncrStateRepository incrStateRepository;

	/** 
	 * 同步状态增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrStateService#SyncStateToDB()    
	 */
	@Override
	public void syncStateToDB() {
		if (DateTime.now().getHourOfDay() == 1 && DateTime.now().getMinuteOfHour() < 10) {
			// 删除过期数据
			int count = incrStateRepository.deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
			logger.info("IncrState delete successfully, count = " + count);
		}

		String jsonStr = redisManager.getStr(RedisKeyConst.CacheKey_StateSyncTimeKey);
		Date startTime = null;
		try {
			startTime = DateTime.parse(jsonStr).toDate();
		} catch (Exception e) {
			startTime = JSON.parseObject(jsonStr, Date.class);
		}
		logger.info("get startTime = " + startTime + ",from redis key = " + RedisKeyConst.CacheKey_StateSyncTimeKey.getKey());
		startTime = (startTime == null) ? new Date() : startTime;
		Date endTime = DateHandlerUtils.getOffsetDate(Calendar.MINUTE, -5);
		logger.info("SyncRatesToDB,startTime = " + DateHandlerUtils.formatDate(startTime, "yyyy-MM-dd HH:mm:ss") + ",endTime = "
				+ DateHandlerUtils.formatDate(endTime, "yyyy-MM-dd HH:mm:ss"));
		if (endTime.compareTo(startTime) > 0) {
			syncStateToDB(startTime, endTime);
			redisManager.put(RedisKeyConst.CacheKey_StateSyncTimeKey, endTime);
			logger.info("put to redis successfully.key = " + RedisKeyConst.CacheKey_StateSyncTimeKey + ",value = " + endTime);
		} else {
			logger.info("SyncRatesToDB, ignore this time ,due to startTime < endTime");
		}
	}

	/** 
	 * 同步状态增量，数据来源包含(HotelId HotelCode RoomId RoomTypeId RatePlanId RatePlanPolicy)
	 *	
	 * @param startTime
	 * @param endTime
	 */
	private void syncStateToDB(Date startTime, Date endTime) {
		String startTimeStr = DateHandlerUtils.formatDate(startTime, "yyyy-MM-dd HH:mm:ss");
		String endTimeStr = DateHandlerUtils.formatDate(endTime, "yyyy-MM-dd HH:mm:ss");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "HotelId");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "HotelCode");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "RoomId");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "RoomTypeId");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "RatePlanId");
		incrStateRepository.syncStateToDB(startTimeStr, endTimeStr, "RatePlanPolicy");
	}

}
