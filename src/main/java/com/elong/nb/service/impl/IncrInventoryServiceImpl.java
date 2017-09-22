/**   
 * @(#)IncrInventoryServiceImpl.java	2016年9月21日	下午2:19:20	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.dao.MySqlDataDao;
import com.elong.nb.repository.IncrInventoryRepository;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.util.ConfigUtils;

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
