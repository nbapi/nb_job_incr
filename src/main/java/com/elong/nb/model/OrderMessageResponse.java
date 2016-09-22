/**   
 * @(#)OrderMessageResponse.java	2016年5月20日	上午11:30:49	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

/**
 * 订单变化消息处理返回结果
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年5月20日 上午11:30:49   hongtao.su     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		hongtao.su  
 * @version		1.0  
 * @since		JDK1.7
 */
public class OrderMessageResponse {

	/** 
	 * 返回结果成功常量
	 *
	 * String OrderMessageResponse.java SUCCESS
	 */
	public static final String SUCCESS = "0";

	/** 
	 * 返回结果忽略常量
	 *
	 * String OrderMessageResponse.java IGNORE
	 */
	public static final String IGNORE = "-2";

	/** 
	 * 返回结果失败常量
	 *
	 * String OrderMessageResponse.java FAILURE
	 */
	public static final String FAILURE = "-1";

	private String responseCode;

	private String exceptionMessage;

	/**   
	 * 得到responseCode的值   
	 *   
	 * @return responseCode的值
	 */
	public String getResponseCode() {
		return responseCode;
	}

	/**
	 * 设置responseCode的值
	 *   
	 * @param responseCode 被设置的值
	 */
	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	/**   
	 * 得到exceptionMessage的值   
	 *   
	 * @return exceptionMessage的值
	 */
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	/**
	 * 设置exceptionMessage的值
	 *   
	 * @param exceptionMessage 被设置的值
	 */
	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

}
