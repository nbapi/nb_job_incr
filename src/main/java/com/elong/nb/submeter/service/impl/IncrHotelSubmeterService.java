/**   
 * @(#)IncrHotelSubmeterService.java	2017年4月21日	上午10:53:57	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.elong.nb.dao.IncrHotelDao;
import com.elong.nb.model.bean.IncrHotel;

/**
 * IncrHotel分表实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 上午10:53:57   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service(value = "incrHotelSubmeterService")
public class IncrHotelSubmeterService extends AbstractSubmeterService<IncrHotel> {

	@Resource
	private IncrHotelDao incrHotelDao;

	/** 
	 * 获取分表前缀 
	 *
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#getTablePrefix()    
	 */
	@Override
	public String getTablePrefix() {
		return "IncrHotel";
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
	protected int bulkInsertSub(String dataSource, String subTableName, List<IncrHotel> subRowList) {
		return incrHotelDao.bulkInsertSub(dataSource, subTableName, subRowList);
	}

	/** 
	 * 创建分表
	 *
	 * @param newTableName 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#createSubTable(java.lang.String)    
	 */
	@Override
	public void createSubTable(String dataSource, String newTableName) {
		incrHotelDao.createSubTable(dataSource, newTableName);
	}

	/** 
	 * 获取分表指定trigger的最后一条增量
	 *
	 * @param dataSource
	 * @param subTableName
	 * @param trigger
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getLastIncrData(java.lang.String, java.lang.String, java.lang.String)    
	 */
	@Override
	protected IncrHotel getLastIncrData(String dataSource, String subTableName, Map<String, Object> params) {
		return incrHotelDao.getLastHotel(dataSource, subTableName, params);
	}

}
