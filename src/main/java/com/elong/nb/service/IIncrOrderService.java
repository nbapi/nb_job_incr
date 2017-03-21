/**   
 * @(#)IIncrOrderService.java	2016年9月19日	下午1:24:26	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;


/**
 * IncrOrder服务接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月19日 下午1:24:26   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IIncrOrderService {

	/** 
	 * 删除订单增量
	 *
	 */
	public void delOrderFromDB();

	/** 
	 * 同步订单增量（兜底：在订单组主动推送消息挂调时）
	 *
	 */
	public void syncOrderToDB();

}
