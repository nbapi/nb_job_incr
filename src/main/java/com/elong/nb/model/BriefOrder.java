/**   
 * @(#)BriefOrder.java	2016年10月12日	下午4:14:46	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

/**
 * 订单基础数据from OrderCenter
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年10月12日 下午4:14:46   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class BriefOrder {

	/** 
	 * 订单号
	 *
	 * long BriefOrder.java orderId
	 */
	private long orderId;
	
	/** 
	 * 订单时间戳
	 *
	 * String BriefOrder.java orderTimestamp
	 */
	private String orderTimestamp;
	
	/** 
	 * 订单状态
	 *
	 * String BriefOrder.java status
	 */
	private String status;

	/**   
	 * 得到orderId的值   
	 *   
	 * @return orderId的值
	 */
	public long getOrderId() {
		return orderId;
	}

	/**
	 * 设置orderId的值
	 *   
	 * @param orderId 被设置的值
	 */
	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	/**   
	 * 得到orderTimestamp的值   
	 *   
	 * @return orderTimestamp的值
	 */
	public String getOrderTimestamp() {
		return orderTimestamp;
	}

	/**
	 * 设置orderTimestamp的值
	 *   
	 * @param orderTimestamp 被设置的值
	 */
	public void setOrderTimestamp(String orderTimestamp) {
		this.orderTimestamp = orderTimestamp;
	}

	/**   
	 * 得到status的值   
	 *   
	 * @return status的值
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * 设置status的值
	 *   
	 * @param status 被设置的值
	 */
	public void setStatus(String status) {
		this.status = status;
	}

}
