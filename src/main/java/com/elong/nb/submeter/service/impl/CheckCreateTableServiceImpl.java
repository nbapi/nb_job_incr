/**   
 * @(#)CheckCreateTableServiceImpl.java	2017年4月11日	上午11:16:48	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.dao.SubmeterTableDao;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.ICheckCreateTableService;
import com.elong.nb.submeter.service.ISubmeterService;

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

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	@Resource
	private SubmeterTableDao submeterTableDao;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	// TODO 改成从配置文件读取
	private static final int SUBMETER_COUNT = 30;

	/** 
	 * 检查分表数量是否够，返回待创建分表表名 
	 *
	 * @param incrType
	 * @return 
	 *
	 * @see com.elong.nb.service.ICheckCreateTableService#checkSubTable(com.elong.nb.model.enums.EnumIncrType)    
	 */
	@Override
	public List<String> checkSubTable(EnumIncrType incrType) {
		ISubmeterService<?> submeterCommonService = getSubmeterService(incrType);
		String tablePrefix = submeterCommonService.getTablePrefix();
		List<Map<String, Object>> allTableMap = submeterTableDao.queryAllSubTableList(tablePrefix + "%", SUBMETER_COUNT);
		if (allTableMap == null || allTableMap.size() == 0) {
			throw new IllegalStateException("EnumIncrType = " + incrType + " has no submeter!!!");
		}
		// 查找末尾连续空表
		List<String> emptyTableNameList = new ArrayList<String>();
		for (Map<String, Object> tableMap : allTableMap) {
			Integer tableRows = (Integer) tableMap.get("table_rows");
			if (tableRows > 0)
				break;
			String tableName = (String) tableMap.get("table_name");
			emptyTableNameList.add(tableName);
		}

		// 末尾连续空表数量
		int currentEmptyCount = emptyTableNameList == null ? 0 : emptyTableNameList.size();
		logger.info("currentEmptyCount = " + currentEmptyCount);
		logger.info("currentEmptyTable = " + JSON.toJSONString(emptyTableNameList));
		if (currentEmptyCount >= SUBMETER_COUNT)
			return Collections.emptyList();

		// 需要创建分表
		List<String> needCreateTableList = new ArrayList<String>();
		String lastNumberStr = incrSetInfoService.get(tablePrefix + ".SubTable.Number");
		lastNumberStr = StringUtils.isEmpty(lastNumberStr) ? "0" : lastNumberStr;
		int lastNumber = Integer.valueOf(lastNumberStr);
		int needCreateCount = SUBMETER_COUNT - currentEmptyCount;
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
	public List<String> createSubTable(EnumIncrType incrType, List<String> tableNameList) {
		if (tableNameList == null || tableNameList.size() == 0)
			return Collections.emptyList();

		ISubmeterService<?> submeterCommonService = getSubmeterService(incrType);
		String tablePrefix = submeterCommonService.getTablePrefix();
		List<String> successTableList = new ArrayList<String>();
		for (String newTableName : tableNameList) {
			if (StringUtils.isEmpty(newTableName))
				continue;
			submeterCommonService.createSubTable(newTableName);
			String currentNumber = StringUtils.substringAfter(newTableName, "_");
			incrSetInfoService.put(tablePrefix + ".SubTable.Number", Integer.valueOf(currentNumber));
			successTableList.add(newTableName);
		}
		logger.info("successCreateCount = " + tableNameList.size());
		logger.info("successCreateTable = " + JSON.toJSONString(successTableList));
		return successTableList;
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
		throw new IllegalStateException("EnumIncrType = " + incrType + " doesn't support submeter!!!");
	}

}
