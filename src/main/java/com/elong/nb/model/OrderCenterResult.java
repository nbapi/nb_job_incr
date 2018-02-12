/**   
 * @(#)OrderCenterResult.java	2016年10月12日	下午4:27:10	   
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
 * 2016年10月12日 下午4:27:10   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class OrderCenterResult {

	private OrderCenterBody body;
	private int retcode;
	private String retdesc;
	private String serverIp;
	private int serverTicks;

	/**   
	 * 得到body的值   
	 *   
	 * @return body的值
	 */
	public OrderCenterBody getBody() {
		return body;
	}

	/**
	 * 设置body的值
	 *   
	 * @param body 被设置的值
	 */
	public void setBody(OrderCenterBody body) {
		this.body = body;
	}

	/**   
	 * 得到retcode的值   
	 *   
	 * @return retcode的值
	 */
	public int getRetcode() {
		return retcode;
	}

	/**
	 * 设置retcode的值
	 *   
	 * @param retcode 被设置的值
	 */
	public void setRetcode(int retcode) {
		this.retcode = retcode;
	}

	/**   
	 * 得到retdesc的值   
	 *   
	 * @return retdesc的值
	 */
	public String getRetdesc() {
		return retdesc;
	}

	/**
	 * 设置retdesc的值
	 *   
	 * @param retdesc 被设置的值
	 */
	public void setRetdesc(String retdesc) {
		this.retdesc = retdesc;
	}

	/**   
	 * 得到serverIp的值   
	 *   
	 * @return serverIp的值
	 */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * 设置serverIp的值
	 *   
	 * @param serverIp 被设置的值
	 */
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	/**   
	 * 得到serverTicks的值   
	 *   
	 * @return serverTicks的值
	 */
	public int getServerTicks() {
		return serverTicks;
	}

	/**
	 * 设置serverTicks的值
	 *   
	 * @param serverTicks 被设置的值
	 */
	public void setServerTicks(int serverTicks) {
		this.serverTicks = serverTicks;
	}

}
