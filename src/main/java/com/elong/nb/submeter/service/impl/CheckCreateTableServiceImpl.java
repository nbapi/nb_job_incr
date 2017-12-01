/**   
 * @(#)CheckCreateTableServiceImpl.java	2017年4月11日	上午11:16:48	   
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
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.dao.SubmeterTableDao;
import com.elong.nb.model.ShardingInfo;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.model.enums.SubmeterConst;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.ICheckCreateTableService;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.ShardingUtils;

/**
 * 检查、创建分表服务实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月11日 上午11:16:48   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class CheckCreateTableServiceImpl implements ICheckCreateTableService {

	private static final Logger logger = Logger.getLogger("CheckCreateTableLogger");

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource(name = "incrRateSubmeterService")
	private ISubmeterService<IncrRate> incrRateSubmeterService;

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	@Resource
	private SubmeterTableDao submeterTableDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	/** 
	 * 检查分表数量是否够，返回待创建分表表名 
	 *
	 * @param incrType
	 * @return 
	 *
	 * @see com.elong.nb.service.ICheckCreateTableService#checkSubTable(com.elong.nb.model.enums.EnumIncrType)    
	 */
	@Override
	public Map<String, List<String>> checkSubTable(EnumIncrType incrType) {
		ISubmeterService<?> submeterCommonService = getSubmeterService(incrType);
		String tablePrefix = submeterCommonService.getTablePrefix();

		Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
		Map<Integer, ShardingInfo> shardingMap = ShardingUtils.SHARDINFO_MAP;
		for (Map.Entry<Integer, ShardingInfo> entry : shardingMap.entrySet()) {
			ShardingInfo shardingInfo = entry.getValue();
			String dataSource = shardingInfo.getDataSource();
			List<String> tableNameList = getTableNameList(dataSource, tablePrefix);
			resultMap.put(dataSource, tableNameList);
		}
		return resultMap;
	}

	/** 
	 * 获取指定数据源待创建表名 
	 *
	 * @param tablePrefix
	 * @param tableMapList
	 * @return
	 */
	private List<String> getTableNameList(String dataSource, String tablePrefix) {
		List<Map<String, Object>> tableMapList = submeterTableDao.queryAllSubTableList(dataSource, tablePrefix + "%",
				SubmeterConst.EMPTY_SUBMETER_COUNT_IN_DB);
		logger.info("tableMapList = " + (tableMapList == null ? 0 : tableMapList.size()));
		// 查找末尾连续空表
		List<String> emptyTableNameList = new ArrayList<String>();
		if (tableMapList != null && tableMapList.size() > 0) {
			for (Map<String, Object> tableMap : tableMapList) {
				Number tableRows = (Number) tableMap.get("table_rows");
				String tableName = (String) tableMap.get("table_name");
				logger.info("tableName = " + tableName + ",tableRows = " + tableRows.longValue());
				if (tableRows.longValue() > 0)
					break;
				emptyTableNameList.add(tableName);
			}
		}

		// 末尾连续空表数量
		int currentEmptyCount = emptyTableNameList == null ? 0 : emptyTableNameList.size();
		logger.info("dataSource = " + dataSource + ",currentEmptyCount = " + currentEmptyCount);
		logger.info("dataSource = " + dataSource + ",currentEmptyTable = " + JSON.toJSONString(emptyTableNameList));
		if (currentEmptyCount >= SubmeterConst.EMPTY_SUBMETER_COUNT_IN_DB)
			return Collections.emptyList();

		// 需要创建分表
		String lastNumberStr = null;
		if (tableMapList != null && tableMapList.size() > 0) {
			String lastExistTableName = (String) tableMapList.get(0).get("table_name");
			lastNumberStr = StringUtils.substringAfter(lastExistTableName, "_");
		}
		// 首次创建分表，序号读取数据库设置序号
		lastNumberStr = StringUtils.isEmpty(lastNumberStr) ? incrSetInfoService.get(tablePrefix + ".SubTable.Number") : lastNumberStr;
		// 数据库未设置分表序号，直接异常
		if (StringUtils.isEmpty(lastNumberStr)) {
			throw new IllegalStateException("dataSource = " + dataSource + "," + tablePrefix
					+ " createSubTable first,please set value whose key is " + tablePrefix + ".SubTable.Number!!!");
		}
		int lastNumber = 0;
		try {
			lastNumber = Integer.valueOf(lastNumberStr);
		} catch (NumberFormatException e) {
			// 数据库未设置分表序号设置非数字，直接异常
			throw new IllegalStateException("dataSource = " + dataSource + "," + tablePrefix
					+ " createSubTable first,please set Integer value whose key is " + tablePrefix + ".SubTable.Number!!!");
		}
		int needCreateCount = SubmeterConst.EMPTY_SUBMETER_COUNT_IN_DB - currentEmptyCount;
		List<String> needCreateTableList = new ArrayList<String>();
		for (int i = 1; i <= needCreateCount; i++) {
			needCreateTableList.add(tablePrefix + "_" + (lastNumber + i));
		}
		logger.info("needCreateCount = " + needCreateCount);
		logger.info("needCreateTable = " + JSON.toJSONString(needCreateTableList));
		return needCreateTableList;
	}

	/** 
	 * 创建新分表 
	 *
	 * @param incrType
	 * @param tableNameList
	 * @return 
	 *
	 * @see com.elong.nb.service.ICheckCreateTableService#createSubTable(com.elong.nb.model.enums.EnumIncrType, java.util.List)    
	 */
	@Override
	public void createSubTable(EnumIncrType incrType, Map<String, List<String>> dataSourceTableNameMap) {
		if (dataSourceTableNameMap == null || dataSourceTableNameMap.size() == 0)
			return;
		ISubmeterService<?> submeterCommonService = getSubmeterService(incrType);
		for (Map.Entry<String, List<String>> entry : dataSourceTableNameMap.entrySet()) {
			String dataSource = entry.getKey();
			List<String> tableNameList = entry.getValue();

			List<String> successTableList = new ArrayList<String>();
			for (String newTableName : tableNameList) {
				if (StringUtils.isEmpty(newTableName))
					continue;
				submeterCommonService.createSubTable(dataSource, newTableName);
				successTableList.add(newTableName);
				logger.info("dataSource = " + dataSource + ",successCreateCount = " + tableNameList.size());
				logger.info("successCreateTable = " + JSON.toJSONString(successTableList));
			}
		}
	}

	/** 
	 * 获取分表服务
	 *
	 * @param incrType
	 * @return
	 */
	private ISubmeterService<?> getSubmeterService(EnumIncrType incrType) {
		if (incrType == EnumIncrType.Inventory) {
			return incrInventorySubmeterService;
		}
		if (incrType == EnumIncrType.Data) {
			return incrHotelSubmeterService;
		}
		if (incrType == EnumIncrType.Rate) {
			return incrRateSubmeterService;
		}
		throw new IllegalStateException("EnumIncrType = " + incrType + " doesn't support submeter!!!");
	}

}
