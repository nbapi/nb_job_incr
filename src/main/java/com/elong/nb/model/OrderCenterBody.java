/**   
 * @(#)OrderCenterBody.java	2016年10月12日	下午4:28:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

import java.util.List;
import java.util.Map;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年10月12日 下午4:28:27   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class OrderCenterBody {

	private boolean hasNext;
	
	private List<Map<String,Object>> orders;

	/**   
	 * 得到hasNext的值   
	 *   
	 * @return hasNext的值
	 */
	public boolean isHasNext() {
		return hasNext;
	}

	/**
	 * 设置hasNext的值
	 *   
	 * @param hasNext 被设置的值
	 */
	public void setHasNext(boolean hasNext) {
		this.hasNext = hasNext;
	}

	/**   
	 * 得到orders的值   
	 *   
	 * @return orders的值
	 */
	public List<Map<String,Object>> getOrders() {
		return orders;
	}

	/**
	 * 设置orders的值
	 *   
	 * @param orders 被设置的值
	 */
	public void setOrders(List<Map<String,Object>> orders) {
		this.orders = orders;
	}

}
