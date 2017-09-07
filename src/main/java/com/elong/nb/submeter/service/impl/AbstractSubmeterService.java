/**   
 * @(#)AbstractSubmeterService.java	2017年4月18日	上午11:31:04	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.model.bean.Idable;
import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.submeter.service.ISubmeterService;

/**
 * 分表服务实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月18日 上午11:31:04   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */

public abstract class AbstractSubmeterService<T extends Idable> implements ISubmeterService<T> {

	protected static final Logger logger = Logger.getLogger("SubmeterLogger");

	private RedisManager redisManager = RedisManager.getInstance("redis_shared", "redis_shared");

	@Resource
	private IImpulseSenderService impulseSenderService;

	@Resource
	private SubmeterTableCalculate submeterTableCache;

	/** 
	 * 获取最后一张非空表名 
	 *
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#getLastTableName()    
	 */
	@Override
	public String getLastTableName() {
		String tablePrefix = getTablePrefix();
		long curId = impulseSenderService.curId(tablePrefix + "_ID");
		long tableNumber = submeterTableCache.getSelectedSubTableNumber(curId);
		return tablePrefix + "_" + tableNumber;
	}

	/** 
	 * 插入分表数据
	 *
	 * @param rowList
	 *
	 * @see com.elong.nb.service.ISubmeterService#builkInsert(java.util.List)    
	 */
	@Override
	public int builkInsert(List<T> rowList) {
		if (rowList == null || rowList.size() == 0)
			return 0;

		String tablePrefix = getTablePrefix();
		long incrVal = rowList.size();
		String configValue = CommonsUtil.CONFIG_PROVIDAR.getProperty("ImpulseSenderFromRedisTest");
		long endID = 0l;
		if (StringUtils.isEmpty(configValue)) {
			endID = impulseSenderService.getId(tablePrefix + "_ID", incrVal);
		} else {
			endID = redisManager.incrBy(RedisManager.getCacheKey(tablePrefix + "_ID"), incrVal);
		}
		long beginID = endID - incrVal + 1;

		List<String> subTableList = new ArrayList<String>();
		Map<String, List<T>> subTableDataMap = new HashMap<String, List<T>>();
		long startTime = System.currentTimeMillis();
		for (T row : rowList) {
			if (row == null)
				continue;
			long ID = beginID++;
			row.setID(ID);

			long tableNumber = submeterTableCache.getSelectedSubTableNumber(ID);
			String subTableName = tablePrefix + "_" + tableNumber;

			List<T> subRowList = subTableDataMap.get(subTableName);
			if (subRowList == null) {
				subRowList = new ArrayList<T>();
			}
			subRowList.add(row);
			subTableDataMap.put(subTableName, subRowList);
			if (!subTableList.contains(subTableName)) {
				subTableList.add(subTableName);
			}
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",subTableName and subRowList put to map,rowList size = "
				+ incrVal);

		int successCount = 0;
		for (String subTableName : subTableList) {
			List<T> subRowList = subTableDataMap.get(subTableName);
			logger.info("subTableName = " + subTableName + ",bulkInsert waitCount = " + subRowList.size());

			int recordCount = subRowList == null ? 0 : subRowList.size();
			String builkInsertSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("BuilkInsertSize");
			int pageSize = StringUtils.isEmpty(builkInsertSize) ? 50 : Integer.valueOf(builkInsertSize);
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			startTime = System.currentTimeMillis();
			int subSuccessCount = 0;
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				int startNum = (pageIndex - 1) * pageSize;
				int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
				subSuccessCount += bulkInsertSub(subTableName, subRowList.subList(startNum, endNum));
			}
			logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",subTableName = " + subTableName
					+ ",bulkInsert successCount = " + subSuccessCount);
			successCount += subSuccessCount;
		}
		return successCount;
	}

	/** 
	 * 获取大于指定lastId的maxRecordCount条增量
	 *
	 * @param lastId
	 * @param maxRecordCount
	 * @return 
	 *
	 * @see com.elong.nb.service.ISubmeterService#getIncrDataList(long, int)    
	 */
	@Override
	public List<T> getIncrDataList(long lastId, int maxRecordCount) {
		String tablePrefix = getTablePrefix();
		long maxId = impulseSenderService.curId(tablePrefix + "_ID");
		List<String> subTableNameList = submeterTableCache.querySubTableNameList(lastId, maxId, tablePrefix, false);
		if (subTableNameList == null || subTableNameList.size() == 0)
			return Collections.emptyList();

		List<T> resultList = new ArrayList<T>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ID", lastId);
		for (String subTableName : subTableNameList) {
			if (StringUtils.isEmpty(subTableName))
				continue;
			params.put("maxRecordCount", maxRecordCount);
			List<T> subList = getIncrDataList(subTableName, params);
			if (subList == null || subList.size() == 0)
				continue;
			resultList.addAll(subList);
			logger.info("subTableName = " + subTableName + ",getIncrDataList params = " + params + ",result size = " + subList.size());
			if (subList.size() >= maxRecordCount)
				break;
			maxRecordCount = maxRecordCount - subList.size();
		}
		return resultList;
	}

	/** 
	 * 获取等于指定trigger的最后一条增量（根据需要子类选择覆盖）
	 *
	 * @param trigger
	 * @return 
	 *
	 * @see com.elong.nb.service.ISubmeterService#getLastIncrData(java.lang.String)    
	 */
	@Override
	public T getLastIncrData(String trigger) {
		String tablePrefix = getTablePrefix();
		long maxId = impulseSenderService.curId(tablePrefix + "_ID");
		List<String> subTableNameList = submeterTableCache.querySubTableNameList(0, maxId, tablePrefix, true);
		if (subTableNameList == null || subTableNameList.size() == 0)
			return null;

		subTableNameList.remove(tablePrefix);
		for (String subTableName : subTableNameList) {
			if (StringUtils.isEmpty(subTableName))
				continue;
			T result = getLastIncrData(subTableName, trigger);
			logger.info("subTableName = " + subTableName + ",getLastIncrData trigger = " + trigger + ",result  = " + result);
			if (result == null)
				continue;
			return result;
		}
		return null;
	}

	/** 
	 * 插入分表数据 
	 *
	 * @param incrType
	 * @param subTableName
	 * @param subRowList
	 * @return
	 */
	protected abstract int bulkInsertSub(String subTableName, List<T> subRowList);

	/** 
	 * 获取分表数据（根据需要子类选择覆盖）
	 *
	 * @param subTableName
	 * @param params
	 * @return
	 */
	protected List<T> getIncrDataList(String subTableName, Map<String, Object> params) {
		return null;
	}

	/** 
	 * 获取分表指定trigger的最后一条增量 （根据需要子类选择覆盖）
	 *
	 * @param subTableName
	 * @param trigger
	 * @return
	 */
	protected T getLastIncrData(String subTableName, String trigger) {
		return null;
	}

}
