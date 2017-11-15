/**   
 * @(#)SubmeterTableCalculate.java	2017年4月26日	下午5:13:49	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.RowRangeInfo;
import com.elong.nb.model.ShardingInfo;
import com.elong.nb.model.enums.SubmeterConst;
import com.elong.nb.util.ShardingUtils;

/**
 * 计算获取表名
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月26日 下午5:13:49   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class SubmeterTableCalculate {
	
	private static final Logger logger = Logger.getLogger("IncrCommonLogger");

	/** 
	 * 获取选定分片数据源 
	 *
	 * @param selectedShardId
	 * @return
	 */
	public String getSelectedDataSource(int selectedShardId) {
		// 获取命中分片信息
		Map<Integer, ShardingInfo> shardInfoMap = ShardingUtils.SHARDINFO_MAP;
		ShardingInfo shardingInfo = shardInfoMap.get(selectedShardId);
		if (shardingInfo == null) {
			throw new IllegalStateException("selectedShardId = " + selectedShardId + ",getSelectedShard is null!");
		}
		String dataSource = shardingInfo.getDataSource();
		if (StringUtils.isEmpty(dataSource)) {
			throw new IllegalStateException("selectedShardId = " + selectedShardId + ",getSelectedDataSource is null!");
		}
		return dataSource;
	}

	/** 
	 * 获取指定id对应的分片编号
	 *
	 * @param id
	 * @return
	 */
	public int getSelectedShardId(long id) {
		// 确定id属于的行范围
		RowRangeInfo rowRangeInfo = null;
		List<RowRangeInfo> rowRangeInfoList = ShardingUtils.ROWRANGEINFO_LIST;
		for (RowRangeInfo row : rowRangeInfoList) {
			if (id < row.getBeginId().longValue() || id > row.getEndId().longValue())
				continue;
			rowRangeInfo = row;
			break;
		}
		if (rowRangeInfo == null) {
			throw new IllegalStateException("id = " + id + ",getRowRangeInfo is null!");
		}
		// 计算所属分片编号
		String shardIdStr = rowRangeInfo.getShardIds();
		if (StringUtils.isEmpty(shardIdStr)) {
			throw new IllegalStateException("id = " + id + ",shardIds is null or empty!");
		}
		String[] shardIds = StringUtils.split(shardIdStr, ",", -1);
		int shardSize = shardIds.length;
		int selectedShardId = (int) ((id - 1) / 10) % shardSize + 1;
		logger.info("id = " + id + ",rowRangeInfo = " + JSON.toJSONString(rowRangeInfo) + ",selectedShardId = " + selectedShardId);
		return selectedShardId;
	}

	/** 
	 * 获取指定id对应的分表序号
	 *
	 * @param id
	 * @return
	 */
	public int getSelectedSubTableNumber(long id) {
		int submeterRowCount = SubmeterConst.PER_SUBMETER_ROW_COUNT;
		submeterRowCount = 600;
		return (int) Math.ceil(id * 1.0 / submeterRowCount);
	}

	/** 
	 * 获取末尾10张or实际张非空分表
	 *
	 * @param lastId
	 * @param maxId
	 * @param tablePrefix
	 * @param isAsc 是否升序
	 * @return
	 */
	public List<String> querySubTableNameList(long lastId, long maxId, String tablePrefix, boolean isAsc) {
		List<String> subTableNameList = new ArrayList<String>();
		long beginTableNumber = getSelectedSubTableNumber(lastId);
		long lastTableNumber = getSelectedSubTableNumber(maxId);
		long cycleCount = lastTableNumber - beginTableNumber + 1;
		cycleCount = cycleCount > 10 ? 10 : cycleCount;
		for (int i = 0; i < cycleCount; i++) {
			String subTableName = tablePrefix + "_" + (lastTableNumber--);
			subTableNameList.add(subTableName);
		}
		if (isAsc) {
			Collections.reverse(subTableNameList);
		}
		return subTableNameList;
	}

}
