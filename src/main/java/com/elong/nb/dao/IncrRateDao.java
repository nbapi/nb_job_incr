/**   
 * @(#)IncrRateDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.bean.IncrRate;

/**
 * 房价增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午2:06:17   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IncrRateDao {

	/** 
	 * changTime 获取大于指定changeTime的最早发生变化的库存增量
	 * incrID 获取大于指定incrID的maxRecordCount条库存增量
	 *  	
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public List<IncrRate> getIncrRates(@Param("subTableName") String subTableName, @Param("params") Map<String, Object> params);

	/** 
	 * 批量插入IncrRate到指定分表subTableName
	 *
	 * @param incrInventories
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public int bulkInsertSub(@Param("subTableName") String subTableName, @Param("list") List<IncrRate> incrRates);

	/** 
	 * 创建分表
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public int createSubTable(@Param("tableName") String tableName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_write")
	public IncrRate getLastIncrFromWrite(@Param("subTableName") String subTableName);

	/** 
	 * 获取最后一条记录 
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_read")
	public IncrRate getLastIncrFromRead(@Param("subTableName") String subTableName);

	/** 
	 * 获取指定时间的记录数
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@DataSource("dataSource_nbsubmeter_read")
	public int getRecordCountFromRead(@Param("subTableName") String subTableName, @Param("startTime") Date startTime,
			@Param("endTime") Date endTime);

}
