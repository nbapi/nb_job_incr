/**   
 * @(#)IFilterService.java	2017年2月21日	上午11:07:30	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.service;


/**
 * 过滤接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年2月21日 上午11:07:30   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IFilterService {

	/** 
	 * 判断是否过滤掉
	 *
	 * @param hotelCode 
	 * @return true:过滤；false：不过滤
	 */
	public boolean doFilter(String hotelCode);

}
