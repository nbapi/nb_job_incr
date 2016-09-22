/**   
 * @(#)IncrRateDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;
import java.util.Map;

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
@DataSource("dataSource_nbhotelincr")
public interface IncrRateDao {

	/** 
	 * 删除过期IncrRate
	 *
	 * @param expireDate
	 * @param limit
	 * @return 返回删除数量
	 */
	public int DeleteExpireIncrData(Map<String, Object> params);

	/** 
	 *
	 * @param changTime 获取大于指定changTime的最早发生变化的房价增量
	 * @param incrID 获取大于指定incrID的maxRecordCount条房价增量
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrRate> GetIncrRates(Map<String, Object> params);

	/** 
	 * 批量插入IncrRate
	 *
	 * @param incrRates
	 */
	public int BulkInsert(List<Map<String, Object>> incrRates);

}
