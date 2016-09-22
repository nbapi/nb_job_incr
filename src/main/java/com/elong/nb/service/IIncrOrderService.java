/**   
 * @(#)IIncrOrderService.java	2016年9月19日	下午1:24:26	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;

import java.util.Map;

import com.elong.nb.model.OrderMessageResponse;

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
	 * 检查订单中心消息 
	 *
	 * @param message
	 * @return
	 */
	public OrderMessageResponse checkMessage(String message);

	/** 
	 * 处理订单中心消息
	 *
	 * @param message
	 */
	public void handlerMessage(String message);

	/** 
	 * 获取订单数据转换为IncrOrder需要格式
	 *
	 * @param sourceMap
	 * @return
	 */
	public Map<String, Object> convertMap(Map<String, Object> sourceMap);

}
