/**   
 * @(#)IncrInventorySubmeterService.java	2017年4月21日	上午10:19:58	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.model.bean.IncrInventory;

/**
 * IncrInventory分表实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 上午10:19:58   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service(value = "incrInventorySubmeterService")
public class IncrInventorySubmeterService extends AbstractSubmeterService<IncrInventory> {

	@Resource
	private IncrInventoryDao incrInventoryDao;

	/** 
	 * 获取分表前缀  
	 *
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#getTablePrefix()    
	 */
	@Override
	public String getTablePrefix() {
		return "IncrInventory";
	}

	/** 
	 * 插入分表数据 
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param subRowList
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#bulkInsertSub(java.lang.String, java.lang.String, java.util.List)    
	 */
	@Override
	protected int bulkInsertSub(String dataSource, String subTableName, List<IncrInventory> subRowList) {
		return incrInventoryDao.bulkInsertSub(dataSource, subTableName, subRowList);
	}

	/** 
	 * 获取分表数据  
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param params
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getIncrDataList(java.lang.String, java.lang.String, java.util.Map)    
	 */
	@Override
	protected List<IncrInventory> getIncrDataList(String dataSource, String subTableName, Map<String, Object> params) {
		return incrInventoryDao.getIncrInventories(dataSource, subTableName, params);
	}

	/** 
	 * 创建分表
	 *
	 * @param dataSource
	 * @param newTableName 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#createSubTable(java.lang.String, java.lang.String)    
	 */
	@Override
	public void createSubTable(String dataSource, String newTableName) {
		incrInventoryDao.createSubTable(dataSource, newTableName);
	}

	/** 
	 * 获取最后一条记录
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param params
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getLastIncrData(java.lang.String, java.lang.String, java.util.Map)    
	 */
	@Override
	protected IncrInventory getLastIncrData(String dataSource, String subTableName, Map<String, Object> params) {
		return incrInventoryDao.getLastIncrFromWrite(dataSource, subTableName);
	}

}
