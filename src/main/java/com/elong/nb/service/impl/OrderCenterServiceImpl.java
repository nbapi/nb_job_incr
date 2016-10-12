/**   
 * @(#)OrderCenterServiceImpl.java	2016年10月12日	下午4:19:11	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.service.INoticeService;
import com.elong.nb.service.OrderCenterService;
import com.elong.nb.util.HttpClientUtils;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

/**
 * 订单中心主动拉取方式（兜底）
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年10月12日 下午4:19:11   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class OrderCenterServiceImpl implements OrderCenterService {

	private static final Logger logger = Logger.getLogger("OrderCenterLogger");

	@Resource
	private INoticeService noticeService;

	/** 
	 * 获取订单基础数据
	 *
	 * @param startTimestamp
	 * @param endTimestamp
	 * @return 
	 *
	 * @see com.elong.nb.service.OrderCenterService#getBriefOrdersByTimestamp(java.lang.String, java.lang.String)    
	 */
	public String getBriefOrdersByTimestamp(String startTimestamp, String endTimestamp) {
		Map<String, Object> reqParams = new HashMap<String, Object>();
		reqParams.put("startTimestamp", startTimestamp);
		reqParams.put("endTimestamp", endTimestamp);

		String reqUrl = PropertiesHelper.getEnvProperties("GetBriefOrdersByTimestampUrl", "config").toString();
		return getOrderData(reqParams, reqUrl);
	}

	/** 
	 * 根据订单号获取订单
	 *
	 * @param orderId
	 * @return 
	 *
	 * @see com.elong.nb.service.OrderCenterService#getOrder(java.lang.Integer)    
	 */
	@Override
	public String getOrder(Integer orderId) {
		Map<String, Object> reqParams = new HashMap<String, Object>();
		reqParams.put("orderId", orderId);
		reqParams.put("fields", "sumPrice,status,roomCount,proxy,sourceOrderId,orderFrom,checkOutDate,checkInDate,cardNo");

		String reqUrl = PropertiesHelper.getEnvProperties("GetOrderUrlFromOrderCenter", "config").toString();
		return getOrderData(reqParams, reqUrl);
	}

	/** 
	 * 批量获取订单
	 *
	 * @param orderIds
	 * @return 
	 *
	 * @see com.elong.nb.service.OrderCenterService#getOrders(java.util.List)    
	 */
	@Override
	public String getOrders(List<Long> orderIds) {
		Map<String, Object> reqParams = new HashMap<String, Object>();
		reqParams.put("orderIds", orderIds);
		reqParams.put("fields", "sumPrice,status,roomCount,proxy,sourceOrderId,orderFrom,checkOutDate,checkInDate,cardNo");

		String reqUrl = PropertiesHelper.getEnvProperties("GetOrdersUrlFromOrderCenter", "config").toString();
		return getOrderData(reqParams, reqUrl);
	}

	/** 
	 * 订单中心获取数据
	 *
	 * @param reqParams
	 * @param reqUrl
	 * @return 
	 *
	 */
	private String getOrderData(Map<String, Object> reqParams, String reqUrl) {
		// 构建请求参数
		String reqData = JSON.toJSONString(reqParams);

		// 从订单中心获取订单数据
		long startTime = new Date().getTime();
		logger.info("httpPost getOrderData,reqUrl = " + reqUrl);
		logger.info("httpPost getOrderData,reqData = " + reqData);
		String result = null;
		try {
			result = HttpClientUtils.httpPost(reqUrl, reqData, "application/json;charset=utf8");
		} catch (Exception e) {
			throw new IllegalStateException("getOrderData from orderCenter error = " + e.getMessage());
		}
		// logger.info("httpPost getOrderData,result = " + result);
		long endTime = new Date().getTime();
		logger.info("use time = " + (endTime - startTime) + ",httpPost getOrderData");
		return result;
	}

}
