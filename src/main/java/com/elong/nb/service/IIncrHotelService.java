/**   
 * @(#)IIncrHotelService.java	2016年9月21日	下午2:36:19	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;


/**
 * IncrHotel服务接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:36:19   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IIncrHotelService {

	/** 
	 * 同步酒店增量
	 *
	 */
	public void syncHotelToDB();
	
	/** 
	 * 删除酒店增量
	 *
	 */
	public void delHotelFromDB();

}
