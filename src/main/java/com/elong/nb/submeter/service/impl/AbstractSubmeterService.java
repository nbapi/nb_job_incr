/**   
 * @(#)AbstractSubmeterService.java	2017年4月18日	上午11:31:04	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.model.bean.Idable;
import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.submeter.util.SubmeterTableCalculate;

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

	@Resource
	private IImpulseSenderService impulseSenderService;

	/** 
	 * 获取最后数据所在分片数据源 
	 *
	 * @return
	 */
	public String getLastShardDataSource() {
		String tablePrefix = getTablePrefix();
		long curId = impulseSenderService.curId(tablePrefix + "_ID");
		return SubmeterTableCalculate.getSelectedDataSource(curId);
	}

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
		return SubmeterTableCalculate.getSelectedSubTable(tablePrefix, curId);
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
		long endID = impulseSenderService.getId(tablePrefix + "_ID", incrVal);
		long beginID = endID - incrVal + 1;

		// 发号器取出的号进行分配，相同分片相同分表数据分组
		List<String> shardSubTableNameList = new ArrayList<String>();
		Map<String, List<T>> subTableDataMap = new HashMap<String, List<T>>();
		long startTime = System.currentTimeMillis();
		for (T row : rowList) {
			if (row == null)
				continue;
			long ID = beginID++;
			row.setID(ID);

			String dataSource = SubmeterTableCalculate.getSelectedDataSource(ID);
			String subTableName = SubmeterTableCalculate.getSelectedSubTable(tablePrefix, ID);
			String shardSubTableName = dataSource + "-" + subTableName;

			List<T> subRowList = subTableDataMap.get(shardSubTableName);
			if (subRowList == null) {
				subRowList = new ArrayList<T>();
			}
			subRowList.add(row);
			subTableDataMap.put(shardSubTableName, subRowList);
			if (!shardSubTableNameList.contains(shardSubTableName)) {
				shardSubTableNameList.add(shardSubTableName);
			}
		}

		// 数据入库
		int successCount = 0;
		for (String shardSubTableName : shardSubTableNameList) {
			List<T> subRowList = subTableDataMap.get(shardSubTableName);
			String dataSource = StringUtils.substringBefore(shardSubTableName, "-");
			String subTableName = StringUtils.substringAfter(shardSubTableName, "-");
			logger.info("dataSource = " + dataSource + ",subTableName = " + subTableName + ",bulkInsert waitCount = " + subRowList.size());

			int recordCount = subRowList == null ? 0 : subRowList.size();
			String builkInsertSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("BuilkInsertSize");
			int pageSize = StringUtils.isEmpty(builkInsertSize) ? 50 : Integer.valueOf(builkInsertSize);
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			startTime = System.currentTimeMillis();
			int subSuccessCount = 0;
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				int startNum = (pageIndex - 1) * pageSize;
				int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
				subSuccessCount += bulkInsertSub(dataSource, subTableName, subRowList.subList(startNum, endNum));
			}
			logger.info("use time = " + (System.currentTimeMillis() - startTime) + "ms,dataSource = " + dataSource + ",subTableName = "
					+ subTableName + ",bulkInsert successCount = " + subSuccessCount);
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
		// 获取id所在分片datasource
		String dataSource = SubmeterTableCalculate.getSelectedDataSource(lastId);
		// 获取id所在分表表名
		String tablePrefix = getTablePrefix();
		String subTableName = SubmeterTableCalculate.getSelectedSubTable(tablePrefix, lastId);
		// 获取id所在段段尾id
		long segmentEndId = SubmeterTableCalculate.getSegmentEndId(lastId);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ID", lastId);
		params.put("segmentEndId", segmentEndId);
		params.put("maxRecordCount", maxRecordCount);
		List<T> subList = getIncrDataList(dataSource, subTableName, params);
		logger.info("getIncrDataList,dataSource = " + dataSource + ",subTableName = " + subTableName + ",params = " + params
				+ ",resultListSize = " + subList.size());

		List<T> resultList = new ArrayList<T>();
		resultList.addAll(subList);
		long maxId = impulseSenderService.curId(tablePrefix + "_ID");
		long nextSegmentBeginId = segmentEndId + 1;
		// id所在段返回数据不够，并且后面段有数据，则继续查下一个段
		if (maxRecordCount > subList.size() && nextSegmentBeginId < maxId) {
			maxRecordCount = maxRecordCount - subList.size();
			List<T> remainList = getIncrDataList(nextSegmentBeginId, maxRecordCount);
			resultList.addAll(remainList);
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
		return getLastIncrData(trigger, maxId);
	}

	/** 
	 * 按段递归查询trigger最后一条数据 
	 *
	 * @param trigger
	 * @param maxId
	 * @return
	 */
	private T getLastIncrData(String trigger, long maxId) {
		String tablePrefix = getTablePrefix();
		// 获取id所在分片datasource
		String dataSource = SubmeterTableCalculate.getSelectedDataSource(maxId);
		// 获取id所在分表表名
		String subTableName = SubmeterTableCalculate.getSelectedSubTable(tablePrefix, maxId);
		// 获取id所在段段尾id
		long segmentBeginId = SubmeterTableCalculate.getSegmentBeginId(maxId);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("trigerName", trigger);
		params.put("segmentBeginId", segmentBeginId);
		T result = getLastIncrData(dataSource, subTableName, params);
		logger.info("getLastIncrData,dataSource = " + dataSource + ",subTableName = " + subTableName + ",params= " + params + ",result  = "
				+ result);
		if (result == null && segmentBeginId > 1) {
			long previousSegmentEndId = segmentBeginId - 1;
			return getLastIncrData(trigger, previousSegmentEndId);
		}
		return result;
	}

	/** 
	 * 插入分表数据
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param subRowList
	 * @return
	 */
	protected abstract int bulkInsertSub(String dataSource, String subTableName, List<T> subRowList);

	/** 
	 * 获取分表数据（根据需要子类选择覆盖）
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param params
	 * @return
	 */
	protected List<T> getIncrDataList(String dataSource, String subTableName, Map<String, Object> params) {
		return null;
	}

	/** 
	 * 获取分表指定trigger的最后一条增量 （根据需要子类选择覆盖）
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param trigger
	 * @return
	 */
	protected T getLastIncrData(String dataSource, String subTableName, Map<String, Object> params) {
		return null;
	}

}
