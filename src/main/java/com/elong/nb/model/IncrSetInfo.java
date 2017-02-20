/**   
 * @(#)IncrSetInfo.java	2017年2月17日	下午2:07:55	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

import java.util.Date;

/**
 * 增量配置model
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年2月17日 下午2:07:55   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class IncrSetInfo {

	private int id;

	private String setKey;

	private String setVal;

	private String setDesc;

	private Date timestamp;

	/**   
	 * 得到id的值   
	 *   
	 * @return id的值
	 */
	public int getId() {
		return id;
	}

	/**
	 * 设置id的值
	 *   
	 * @param id 被设置的值
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**   
	 * 得到setKey的值   
	 *   
	 * @return setKey的值
	 */
	public String getSetKey() {
		return setKey;
	}

	/**
	 * 设置setKey的值
	 *   
	 * @param setKey 被设置的值
	 */
	public void setSetKey(String setKey) {
		this.setKey = setKey;
	}

	/**   
	 * 得到setVal的值   
	 *   
	 * @return setVal的值
	 */
	public String getSetVal() {
		return setVal;
	}

	/**
	 * 设置setVal的值
	 *   
	 * @param setVal 被设置的值
	 */
	public void setSetVal(String setVal) {
		this.setVal = setVal;
	}

	/**   
	 * 得到setDesc的值   
	 *   
	 * @return setDesc的值
	 */
	public String getSetDesc() {
		return setDesc;
	}

	/**
	 * 设置setDesc的值
	 *   
	 * @param setDesc 被设置的值
	 */
	public void setSetDesc(String setDesc) {
		this.setDesc = setDesc;
	}

	/**   
	 * 得到timestamp的值   
	 *   
	 * @return timestamp的值
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * 设置timestamp的值
	 *   
	 * @param timestamp 被设置的值
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

}
