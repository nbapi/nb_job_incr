/**   
 * @(#)IncrRateServiceImpl.java	2016年9月20日	下午4:43:42	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.elong.nb.cache.RedisManager;
import com.elong.nb.consts.RedisKeyConst;
import com.elong.nb.repository.IncrRateRepository;
import com.elong.nb.service.IIncrRateService;
import com.elong.nb.util.DateUtils;

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

	private static final Logger logger = Logger.getLogger("syncIncrRateLogger");

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
	public void SyncRatesToDB() {
		// 删除过期数据
		int count = incrRateRepository.DeleteExpireIncrData("IncrRate", DateUtils.getDBExpireDate());
		logger.info("IncrRate delete successfully.count = " + count);

		String changIDStr = redisManager.getStr(RedisKeyConst.KEY_Rate_LastID_CacheKey);
		long changID = StringUtils.isEmpty(changIDStr) ? 0 : Long.valueOf(changIDStr);
		logger.info("get changID = " + changID + ",from redis key = " + RedisKeyConst.KEY_Rate_LastID_CacheKey.getKey());

		while (true) {
			long newChangID = incrRateRepository.SyncRatesToDB(changID);
			logger.info("SyncRatesToDB," + changID + " ====> " + newChangID);
			if (newChangID == changID)
				break;
			else {
				redisManager.put(RedisKeyConst.KEY_Rate_LastID_CacheKey, newChangID);
				logger.info("put newChangID = " + newChangID + ",to redis key = " + RedisKeyConst.KEY_Rate_LastID_CacheKey.getKey());
				changID = newChangID;
			}
		}
		logger.info("SyncRatesToDB,finished");
	}

}
