/**   
 * @(#)IncrOrderDao.java	2016年8月19日	下午5:22:50	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.bean.IncrOrder;

/**
 * 订单增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午5:22:50   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IncrOrderDao {

	/** 
	 * 删除过期IncrOrder
	 *
	 * @param expireDate
	 * @param limit
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
	 * 获取最大IncrID的订单增量
	 *
	 * @param paramMap
	 * @return
	 */
	@DataSource("dataSource_nbhotelincr_read")
	public IncrOrder getLastIncrOrder(Map<String, Object> paramMap);

	/** 
	 * 插入IncrOrder
	 *
	 * @param incrOrderMap
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int insert(Map<String, Object> incrOrderMap);

	/** 
	 * 批量插入IncrOrder
	 *
	 * @param incrOrders
	 */
	@DataSource("dataSource_nbhotelincr_write")
	public int bulkInsert(List<Map<String, Object>> incrOrders);

}
