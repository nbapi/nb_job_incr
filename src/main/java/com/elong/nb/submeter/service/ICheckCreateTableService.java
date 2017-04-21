/**   
 * @(#)ICheckCreateTableService.java	2017年4月11日	上午10:58:15	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service;

import java.util.List;

import com.elong.nb.model.enums.EnumIncrType;

/**
 * 检查、创建分表服务
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月11日 上午10:58:15   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface ICheckCreateTableService {

	/** 
	 * 检查分表数量是否够，返回待创建分表表名 
	 *
	 * @param incrType
	 * @return
	 */
	public List<String> checkSubTable(EnumIncrType incrType);

	/** 
	 * 创建新分表 
	 *
	 * @param incrType
	 * @param tableNameList
	 * @return
	 */
	public List<String> createSubTable(EnumIncrType incrType, List<String> tableNameList);

}
