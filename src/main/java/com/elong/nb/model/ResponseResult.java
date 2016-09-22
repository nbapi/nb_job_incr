/**   
 * @(#)ResponseResult.java	2016年9月7日	下午4:34:32	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

/**
 * 增量同步返回结果模型
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月7日 下午4:34:32   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class ResponseResult {

	/** 
	 * 成功
	 *
	 * int ResponseResult.java SUCCESS
	 */
	public static final int SUCCESS = 0;

	/** 
	 * 失败
	 *
	 * int ResponseResult.java FAILURE
	 */
	public static final int FAILURE = -1;

	/** 
	 * 结果编码
	 *
	 * int ResponseResult.java code
	 */
	private int code = SUCCESS;

	/** 
	 * 描述信息
	 *
	 * String ResponseResult.java message
	 */
	private String message;

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
	 * 得到message的值   
	 *   
	 * @return message的值
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 设置message的值
	 *   
	 * @param message 被设置的值
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
