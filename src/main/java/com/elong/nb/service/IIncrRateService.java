/**   
 * @(#)IIncrRateService.java	2016年9月20日	下午4:42:08	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;

/**
 * IncrRate服务接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月20日 下午4:42:08   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IIncrRateService {

	/** 
	 * IncrRate同步到数据库 
	 *
	 */
	public void syncRatesToDB();
	
	/** 
	 * 删除价格增量
	 *
	 */
	public void delRatesFromDB();

}
