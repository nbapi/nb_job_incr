/**   
 * @(#)IncrInventorySubmeterService.java	2017年4月21日	上午10:19:58	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
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
	 * @param subTableName
	 * @param subRowList
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#bulkInsertSub(java.lang.String, java.util.List)    
	 */
	@Override
	protected int bulkInsertSub(String shardIdSubTableName, final List<IncrInventory> subRowList) {
		String[] strs = StringUtils.split(shardIdSubTableName, "-", -1);
		int selectedShardId = Integer.valueOf(strs[0]);
		String dataSource = submeterTableCache.getSelectedDataSource(selectedShardId);
		String subTableName = strs[1];
		if("1-IncrInventory_1".equals(shardIdSubTableName)){
			incrInventoryDao.bulkInsertSub(dataSource, subTableName, subRowList);
		}
		return 0;
	}

	/** 
	 * 获取分表数据 
	 *
	 * @param subTableName
	 * @param params
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getIncrDataList(java.lang.String, java.util.Map)    
	 */
	@Override
	protected List<IncrInventory> getIncrDataList(String subTableName, Map<String, Object> params) {
		// return incrInventoryDao.getIncrInventories(subTableName, params);
		return null;
	}

	/** 
	 * 创建分表
	 *
	 * @param newTableName 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#createSubTable(java.lang.String)    
	 */
	@Override
	public void createSubTable(String newTableName) {
		// incrInventoryDao.createSubTable(newTableName);
	}

	/** 
	 * 获取最后一条记录
	 *
	 * @param subTableName
	 * @param trigger
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getLastIncrData(java.lang.String, java.lang.String)    
	 */
	@Override
	protected IncrInventory getLastIncrData(String subTableName, String trigger) {
		// return incrInventoryDao.getLastIncrFromWrite(subTableName);
		return null;
	}

}
