/**   
 * @(#)IncrStateServiceImpl.java	2016年9月21日	下午2:50:24	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.dao.IncrStateDao;
import com.elong.nb.repository.IncrStateRepository;
import com.elong.nb.service.AbstractDeleteService;
import com.elong.nb.service.IIncrSetInfoService;
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
public class IncrStateServiceImpl extends AbstractDeleteService implements IIncrStateService {

	private static final Logger logger = Logger.getLogger("IncrStateLogger");

	@Resource
	private IncrStateRepository incrStateRepository;
	
	@Resource
	private IncrStateDao incrStateDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;
	
	/** 
	 * 删除状态增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrStateService#delStateFromDB()    
	 */
	@Override
	public void delStateFromDB() {
		if (DateTime.now().getHourOfDay() == 1 && DateTime.now().getMinuteOfHour() < 10) {
			// 删除过期数据
			deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
		}
	}

	/** 
	 * 同步状态增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrStateService#SyncStateToDB()    
	 */
	@Override
	public void syncStateToDB() {
		String jsonStr = incrSetInfoService.get(RedisKeyConst.CacheKey_StateSyncTimeKey.getKey());
		Date startTime = null;
		try {
			startTime = DateTime.parse(jsonStr).toDate();
		} catch (Exception e) {
			startTime = JSON.parseObject(jsonStr, Date.class);
		}
		logger.info("get startTime = " + startTime + ",from redis key = " + RedisKeyConst.CacheKey_StateSyncTimeKey.getKey());
		startTime = (startTime == null) ? DateHandlerUtils.getOffsetDate(Calendar.MINUTE, -500) : startTime;
		Date endTime = DateHandlerUtils.getOffsetDate(Calendar.MINUTE, -5);
		logger.info("SyncRatesToDB,startTime = " + DateHandlerUtils.formatDate(startTime, "yyyy-MM-dd HH:mm:ss") + ",endTime = "
				+ DateHandlerUtils.formatDate(endTime, "yyyy-MM-dd HH:mm:ss"));
		if (endTime.compareTo(startTime) > 0) {
			syncStateToDB(startTime, endTime);
			incrSetInfoService.put(RedisKeyConst.CacheKey_StateSyncTimeKey.getKey(), endTime);
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

	@Override
	protected List<BigInteger> getIncrIdList(Map<String, Object> params) {
		return incrStateDao.getIncrIdList(params);
	}

	@Override
	protected int deleteByIncrIdList(List<BigInteger> incrIdList) {
		return incrStateDao.deleteByIncrIdList(incrIdList);
	}

	@Override
	protected void logger(String message) {
		logger.info(message);
	}

}
