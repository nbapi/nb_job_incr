/**   
 * @(#)IncrInventoryServiceImpl.java	2016年9月21日	下午2:19:20	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.util.DateHandlerUtils;

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

	/** 
	 * 同步库存增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#SyncInventoryToDB()    
	 */
	@Override
	public void syncInventoryToDB() {
		// 删除30小时以前的数据
		long startTime = System.currentTimeMillis();
		int count = incrInventoryRepository.deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
		logger.info("IncrInventory delete successfully.count = " + count);
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",IncrInventory delete successfully.count = " + count);
		// 递归同步数据
		syncInventoryToDB(0);
	}

	/** 
	 * 递归，根据changeID同步库存增量
	 *
	 * @param changeID 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#SyncInventoryToDB(long)    
	 */
	@Override
	public void syncInventoryToDB(long changeID) {
		if (changeID == 0) {
			long startTime = System.currentTimeMillis();
			changeID = Long.valueOf(redisManager.getStr(RedisKeyConst.CacheKey_KEY_Inventory_LastID));
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
			redisManager.put(RedisKeyConst.CacheKey_KEY_Inventory_LastID, newLastChgID);
			logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",put to redis key" + ",incred = " + incred + ",key = "
					+ RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey() + ",value = " + newLastChgID);			
			if (incred > 100) {
				// 继续执行
				syncInventoryToDB(newLastChgID);
			}
		}
	}

}
