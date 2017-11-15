/**   
 * @(#)IncrInventoryDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.bean.IncrInventory;

/**
 * 库存增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午2:06:51   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IncrInventoryDao {

	/** 
	 * changTime 获取大于指定changeTime的最早发生变化的库存增量
	 * incrID 获取大于指定incrID的maxRecordCount条库存增量
	 *  	
	 * @param params
	 * @return
	 */
	public List<IncrInventory> getIncrInventories(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("params") Map<String, Object> params);

	/** 
	 * 批量插入IncrInventory到指定分表subTableName
	 *
	 * @param incrInventories
	 */
	public int bulkInsertSub(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("list") List<IncrInventory> incrInventories);

	/** 
	 * 创建分表
	 *
	 * @param tableName
	 * @return
	 */
	public int createSubTable(@DataSource("dataSource") String dataSource, @Param("tableName") String tableName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	public IncrInventory getLastIncrFromWrite(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	public IncrInventory getLastIncrFromRead(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName);

	/** 
	 * 获取指定时间的记录数
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public int getRecordCountFromRead(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

}
