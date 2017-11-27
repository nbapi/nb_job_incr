/**   
 * @(#)IncrHotelDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.bean.IncrHotel;

/**
 * 酒店增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午2:26:21   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IncrHotelDao {

	/** 
	 * 获取trigger的最后一条IncrHotel
	 *
	 * @param trigger
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public IncrHotel getLastHotel(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("params") Map<String, Object> params);

	/** 
	 * 批量插入IncrInventory到指定分表subTableName
	 *
	 * @param incrInventories
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public int bulkInsertSub(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("list") List<IncrHotel> incrHotelList);

	/** 
	 * 创建分表tableName
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public int createSubTable(@DataSource("dataSource") String dataSource, @Param("tableName") String tableName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public IncrHotel getLastIncrFromWrite(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("triggerName") String triggerName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_read")
	public IncrHotel getLastIncrFromRead(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("triggerName") String triggerName);

	/** 
	 * 获取指定时间的记录数
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_read")
	public int getRecordCountFromRead(@DataSource("dataSource") String dataSource, @Param("subTableName") String subTableName,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("triggerName") String triggerName);

}
