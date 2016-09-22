/**   
 * @(#)IncrStateDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;
import java.util.Map;

import com.elong.nb.db.DataSource;

/**
 * 状态增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午2:17:18   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@DataSource("dataSource_nbhotelincr")
public interface IncrStateDao {

	/** 
	 * 删除过期IncrState
	 *
	 * @param expireDate
	 * @param limit
	 * @return 返回删除数量
	 */
	public int DeleteExpireIncrData(Map<String, Object> params);

	/** 
	 * 批量插入IncrState
	 *
	 * @param incrRates
	 */
	public int BulkInsert(List<Map<String, Object>> incrStates);

}
