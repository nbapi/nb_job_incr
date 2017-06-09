/**   
 * @(#)SqlServerDataDao.java	2016年9月13日	下午3:04:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.elong.nb.db.DataSource;

/**
 * PriceInfo_track数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月13日 下午3:04:27   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface MySqlDataDao {

	/** 
	 * 查询增量价格待同步数据
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_mysql_product")
	public List<Map<String, Object>> getPriceOperationIncrement(Map<String, Object> params);
	
	/** 
	 * 根据id查询价格变化流水 
	 *
	 * @param id
	 * @return
	 */
	@DataSource("dataSource_mysql_product")
	public Map<String, Object> getPriceOperationIncrementByid(@Param("id") Long id);

}
