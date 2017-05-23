/**   
 * @(#)IncrRateDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.math.BigInteger;
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
public interface IncrRateDao {

	/** 
	 * 删除过期IncrRate
	 *
	 * @return 返回删除数量
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int deleteByIncrIdList(List<BigInteger> incrIdList);
	
	/** 
	 * 获取指定changeTime之前的IncrId集合
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public List<BigInteger> getIncrIdList(Map<String, Object> params);

	/** 
	 * changTime 获取大于指定changTime的最早发生变化的房价增量
	 * incrID 获取大于指定incrID的maxRecordCount条房价增量	
	 * maxRecordCount 返回记录数
	 * 
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public List<IncrRate> getIncrRates(Map<String, Object> params);

	/** 
	 * 批量插入IncrRate
	 *
	 * @param incrRates
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int bulkInsert(List<Map<String, Object>> incrRates);

}
