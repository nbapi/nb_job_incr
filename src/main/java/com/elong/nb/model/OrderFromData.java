/**   
 * @(#)OrderFromData.java	2016年9月14日	下午6:25:18	   
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
 * 2016年9月14日 下午6:25:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class OrderFromData {

	private String projectName;

	/** 
	 * 代理编号
	 *
	 * String OrderFromData.java proxyId
	 */
	private String proxyId;

	/** 
	 * 会员卡号
	 *
	 * long OrderFromData.java cardNo
	 */
	private long cardNo;

	/**   
	 * 得到projectName的值   
	 *   
	 * @return projectName的值
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * 设置projectName的值
	 *   
	 * @param projectName 被设置的值
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**   
	 * 得到proxyId的值   
	 *   
	 * @return proxyId的值
	 */
	public String getProxyId() {
		return proxyId;
	}

	/**
	 * 设置proxyId的值
	 *   
	 * @param proxyId 被设置的值
	 */
	public void setProxyId(String proxyId) {
		this.proxyId = proxyId;
	}

	/**   
	 * 得到cardNo的值   
	 *   
	 * @return cardNo的值
	 */
	public long getCardNo() {
		return cardNo;
	}

	/**
	 * 设置cardNo的值
	 *   
	 * @param cardNo 被设置的值
	 */
	public void setCardNo(long cardNo) {
		this.cardNo = cardNo;
	}

}
