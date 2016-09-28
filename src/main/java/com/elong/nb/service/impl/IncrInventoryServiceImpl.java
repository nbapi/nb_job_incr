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
import com.elong.nb.consts.RedisKeyConst;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.util.DateUtils;

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

	private static final Logger logger = Logger.getLogger("syncIncrInventoryLogger");

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
	public void SyncInventoryToDB() {
		// 删除30小时以前的数据
		int count = incrInventoryRepository.DeleteExpireIncrData("IncrInventory", DateUtils.getDBExpireDate());
		logger.info("IncrInventory delete successfully.count = " + count);

		SyncInventoryToDB(0);
	}

	/** 
	 * 递归，根据changeID同步库存增量
	 *
	 * @param changeID 
	 *
	 * @see com.elong.nb.service.IIncrInventoryService#SyncInventoryToDB(long)    
	 */
	@Override
	public void SyncInventoryToDB(long changeID) {
		if (changeID == 0) {
			changeID = Long.valueOf(redisManager.getStr(RedisKeyConst.KEY_Inventory_LastID_CacheKey));
			logger.info("get changeID = " + changeID + ",from redis key = " + RedisKeyConst.KEY_Inventory_LastID_CacheKey.getKey());
		}
		if (changeID == 0) {
			changeID = incrInventoryRepository.GetInventoryChangeMinID(DateUtils.getCacheExpireDate());
			logger.info("get changeID = " + changeID + ",from wcf [ProductForPartnerServiceContract.getInventoryChangeMinID]");
		}
		long newLastChgID = incrInventoryRepository.SyncInventoryToDB(changeID);
		logger.info("SyncInventoryToDB,change: " + changeID + " ===> " + newLastChgID);

		long incred = newLastChgID - changeID;
		if (incred > 0) {
			// 更新LastID
			redisManager.put(RedisKeyConst.KEY_Inventory_LastID_CacheKey, newLastChgID);
			logger.info("put to redis key = " + RedisKeyConst.KEY_Inventory_LastID_CacheKey.getKey() + ",value = " + newLastChgID);
			if (incred > 100) {
				// 继续执行
				SyncInventoryToDB(newLastChgID);
			}
		}
	}

}
