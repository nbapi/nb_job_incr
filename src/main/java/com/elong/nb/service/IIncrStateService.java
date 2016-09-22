/**   
 * @(#)IIncrStateService.java	2016年9月21日	下午2:49:38	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;

/**
 * IncrState服务接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:49:38   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IIncrStateService {

	/** 
	 * 同步状态增量
	 *
	 */
	public void SyncStateToDB();

}
