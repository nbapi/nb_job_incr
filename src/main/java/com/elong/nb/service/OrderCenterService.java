/**   
 * @(#)OrderCenterService.java	2016年10月12日	下午4:13:08	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;

import java.util.List;

/**
 * 订单中心主动拉取方式（兜底）
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年10月12日 下午4:13:08   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface OrderCenterService {

	/** 
	 * 根据订单号获取订单
	 *
	 * @param orderId
	 * @return
	 */
	public String getOrder(Integer orderId);

	/** 
	 * 获取订单基础数据
	 *
	 * @param startTimestamp
	 * @param endTimestamp
	 * @return
	 */
	public String getBriefOrdersByTimestamp(String startTimestamp, String endTimestamp);

	/** 
	 * 批量获取订单
	 *
	 * @param orderId
	 * @return
	 */
	public String getOrders(List<Object> orderIds);

}
