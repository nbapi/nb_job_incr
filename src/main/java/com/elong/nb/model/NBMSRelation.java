/**   
 * @(#)NB_M_SRelation.java	2016年9月18日	上午11:25:00	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model;

import org.joda.time.DateTime;

/**
 * M_S关系模型
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月18日 上午11:25:00   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class NBMSRelation {

	private String SHotelID;
	private String SStatus;
	private String SSupplierTypeID;
	private String MHotelID;
	private String MStatus;
	private int SSupplierID;
	private DateTime UpdatedAt;

	/**   
	 * 得到sHotelID的值   
	 *   
	 * @return sHotelID的值
	 */
	public String getSHotelID() {
		return SHotelID;
	}

	/**
	 * 设置sHotelID的值
	 *   
	 * @param sHotelID 被设置的值
	 */
	public void setSHotelID(String sHotelID) {
		SHotelID = sHotelID;
	}

	/**   
	 * 得到sStatus的值   
	 *   
	 * @return sStatus的值
	 */
	public String getSStatus() {
		return SStatus;
	}

	/**
	 * 设置sStatus的值
	 *   
	 * @param sStatus 被设置的值
	 */
	public void setSStatus(String sStatus) {
		SStatus = sStatus;
	}

	/**   
	 * 得到sSupplierTypeID的值   
	 *   
	 * @return sSupplierTypeID的值
	 */
	public String getSSupplierTypeID() {
		return SSupplierTypeID;
	}

	/**
	 * 设置sSupplierTypeID的值
	 *   
	 * @param sSupplierTypeID 被设置的值
	 */
	public void setSSupplierTypeID(String sSupplierTypeID) {
		SSupplierTypeID = sSupplierTypeID;
	}

	/**   
	 * 得到mHotelID的值   
	 *   
	 * @return mHotelID的值
	 */
	public String getMHotelID() {
		return MHotelID;
	}

	/**
	 * 设置mHotelID的值
	 *   
	 * @param mHotelID 被设置的值
	 */
	public void setMHotelID(String mHotelID) {
		MHotelID = mHotelID;
	}

	/**   
	 * 得到mStatus的值   
	 *   
	 * @return mStatus的值
	 */
	public String getMStatus() {
		return MStatus;
	}

	/**
	 * 设置mStatus的值
	 *   
	 * @param mStatus 被设置的值
	 */
	public void setMStatus(String mStatus) {
		MStatus = mStatus;
	}

	/**   
	 * 得到sSupplierID的值   
	 *   
	 * @return sSupplierID的值
	 */
	public int getSSupplierID() {
		return SSupplierID;
	}

	/**
	 * 设置sSupplierID的值
	 *   
	 * @param sSupplierID 被设置的值
	 */
	public void setSSupplierID(int sSupplierID) {
		SSupplierID = sSupplierID;
	}

	/**   
	 * 得到updatedAt的值   
	 *   
	 * @return updatedAt的值
	 */
	public DateTime getUpdatedAt() {
		return UpdatedAt;
	}

	/**
	 * 设置updatedAt的值
	 *   
	 * @param updatedAt 被设置的值
	 */
	public void setUpdatedAt(DateTime updatedAt) {
		UpdatedAt = updatedAt;
	}

}
