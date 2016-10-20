/**   
 * @(#)IncrRateServiceImpl.java	2016年9月20日	下午4:43:42	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.repository.IncrRateRepository;
import com.elong.nb.service.IIncrRateService;
import com.elong.nb.util.DateHandlerUtils;

/**
 * IncrRate服务接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月20日 下午4:43:42   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class IncrRateServiceImpl implements IIncrRateService {

	private static final Logger logger = Logger.getLogger("IncrRateLogger");

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@Resource
	private IncrRateRepository incrRateRepository;

	/** 
	 * IncrRate同步到数据库 
	 * 
	 *
	 * @see com.elong.nb.service.IIncrRateService#SyncRatesToDB()    
	 */
	@Override
	public void syncRatesToDB() {
		// 删除过期数据
		long startTime = new Date().getTime();
		int count = incrRateRepository.deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
		long endTime = new Date().getTime();
		logger.info("use time = " + (endTime - startTime) + ",IncrRate delete successfully.count = " + count);

		startTime = new Date().getTime();
		String changIDStr = redisManager.getStr(RedisKeyConst.CacheKey_KEY_Rate_LastID);
		long changID = StringUtils.isEmpty(changIDStr) ? 0 : Long.valueOf(changIDStr);
		endTime = new Date().getTime();
		logger.info("use time = " + (endTime - startTime) + ",get changID = " + changID + ",from redis key = "
				+ RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey());

		while (true) {
			startTime = new Date().getTime();
			long newChangID = incrRateRepository.syncRatesToDB(changID);
			endTime = new Date().getTime();
			logger.info("use time = " + (endTime - startTime) + ", from " + changID + " to " + newChangID);
			if (newChangID == changID)
				break;
			else {
				startTime = new Date().getTime();
				redisManager.put(RedisKeyConst.CacheKey_KEY_Rate_LastID, newChangID);
				endTime = new Date().getTime();
				logger.info("use time = " + (endTime - startTime) + ",put newChangID = " + newChangID + ",to redis key = "
						+ RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey());
				changID = newChangID;
			}
		}
	}

}
