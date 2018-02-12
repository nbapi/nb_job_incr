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

import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.repository.IncrRateRepository;
import com.elong.nb.service.IIncrRateService;
import com.elong.nb.service.IIncrSetInfoService;

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

	@Resource
	private IncrRateRepository incrRateRepository;

	@Resource
	private IncrRateDao incrRateDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	/** 
	 * IncrRate同步到数据库 
	 * 
	 *
	 * @see com.elong.nb.service.IIncrRateService#SyncRatesToDB()    
	 */
	@Override
	public void syncRatesToDB() {
		long jobStartTime = System.currentTimeMillis();
		long startTime = jobStartTime;
		String changIDStr = incrSetInfoService.get(RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey());
		long changID = StringUtils.isEmpty(changIDStr) ? 0 : Long.valueOf(changIDStr);
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",get changID = " + changID + ",from redis key = "
				+ RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey());

		while (true) {
			startTime = System.currentTimeMillis();
			long newChangID = incrRateRepository.syncRatesToDB(changID);
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ", from " + changID + " to " + newChangID);
			if (newChangID == changID || (endTime - jobStartTime) > 10 * 60 * 1000) {
				break;
			} else {
				startTime = System.currentTimeMillis();
				incrSetInfoService.put(RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey(), newChangID);
				endTime = System.currentTimeMillis();
				logger.info("use time = " + (endTime - startTime) + ",put newChangID = " + newChangID + ",to redis key = "
						+ RedisKeyConst.CacheKey_KEY_Rate_LastID.getKey());
				changID = newChangID;
			}
		}
	}

}
