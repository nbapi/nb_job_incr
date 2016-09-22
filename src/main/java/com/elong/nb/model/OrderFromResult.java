/**   
 * @(#)OrderFromResult.java	2016年9月14日	下午6:25:02	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月14日 下午6:25:02   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class OrderFromResult {

	/** 
	* 200:成功，-1：orderFromId有误  -2：代理不存在 其他：失败
	*
	* int OrderFromResult.java code
	*/
	private int code;

	private OrderFromData data;

	private String msg;

	/**   
	 * 得到code的值   
	 *   
	 * @return code的值
	 */
	public int getCode() {
		return code;
	}

	/**
	 * 设置code的值
	 *   
	 * @param code 被设置的值
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**   
	 * 得到data的值   
	 *   
	 * @return data的值
	 */
	public OrderFromData getData() {
		return data;
	}

	/**
	 * 设置data的值
	 *   
	 * @param data 被设置的值
	 */
	public void setData(OrderFromData data) {
		this.data = data;
	}

	/**   
	 * 得到msg的值   
	 *   
	 * @return msg的值
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * 设置msg的值
	 *   
	 * @param msg 被设置的值
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

}
