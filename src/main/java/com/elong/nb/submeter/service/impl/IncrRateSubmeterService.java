/**   
 * @(#)IncrRateSubmeterService.java	2017年8月22日	上午10:52:50	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.model.bean.IncrRate;

/**
 * IncrRate分表实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年8月22日 上午10:52:50   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service(value = "incrRateSubmeterService")
public class IncrRateSubmeterService extends AbstractSubmeterService<IncrRate> {

	@Resource
	private IncrRateDao incrRateDao;

	/** 
	 * 获取分表前缀 
	 *
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#getTablePrefix()    
	 */
	@Override
	public String getTablePrefix() {
		return "IncrRate";
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
		incrRateDao.createSubTable(dataSource, newTableName);
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
	protected int bulkInsertSub(String dataSource, String subTableName, List<IncrRate> subRowList) {
		return incrRateDao.bulkInsertSub(dataSource, subTableName, subRowList);
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
	protected List<IncrRate> getIncrDataList(String dataSource, String subTableName, Map<String, Object> params) {
		return incrRateDao.getIncrRates(dataSource, subTableName, params);
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
	protected IncrRate getLastIncrData(String dataSource, String subTableName, Map<String, Object> params) {
		return incrRateDao.getLastIncrFromWrite(dataSource, subTableName);
	}

}
