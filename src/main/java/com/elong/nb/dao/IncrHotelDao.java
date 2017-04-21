/**   
 * @(#)IncrHotelDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;

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
	@DataSource("dataSource_nbhotelincr_read")
	public IncrHotel getLastHotel(@Param("subTableName")String subTableName, @Param("_triger") String triger);

	/** 
	 * 批量插入IncrInventory到指定分表subTableName
	 *
	 * @param incrInventories
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int bulkInsertSub(@Param("subTableName") String subTableName, @Param("list") List<IncrHotel> incrHotelList);

	/** 
	 * 创建分表tableName
	 *
	 * @param tableName
	 * @return
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int createSubTable(@Param("tableName") String tableName);

}
