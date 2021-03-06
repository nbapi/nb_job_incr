/**   
 * @(#)OrderCenterServiceImpl.java	2016年10月12日	下午4:19:11	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.common.model.NbapiHttpRequest;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.common.util.HttpClientUtil;
import com.elong.nb.service.OrderCenterService;

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

		String reqUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetBriefOrdersByTimestampUrl");
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
		reqParams.put("fields", "sumPrice,payStatus,proxy,nbGuid,orderFrom,cardNo");

		String reqUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetOrderUrlFromOrderCenter");
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
	public String getOrders(List<Object> orderIds) {
		Map<String, Object> reqParams = new HashMap<String, Object>();
		reqParams.put("orderIds", orderIds);
		reqParams.put("fields", "sumPrice,status,payStatus,roomCount,proxy,nbGuid,orderFrom,checkOutDate,checkInDate,cardNo");

		String reqUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetOrdersUrlFromOrderCenter");
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
		NbapiHttpRequest nbapiHttpRequest = new NbapiHttpRequest();
		nbapiHttpRequest.setUrl(reqUrl);
		String reqData = JSON.toJSONString(reqParams);
		nbapiHttpRequest.setParamStr(reqData);
		nbapiHttpRequest.setContentType("application/json;charset=utf8");
		String result = null;
		int reqCount = 0;
		while (StringUtils.isEmpty(result) && ++reqCount <= 2) {
			try {
				// 从订单中心获取订单数据
				result = HttpClientUtil.httpJsonPost(nbapiHttpRequest);
			} catch (Exception e) {
				logger.error("getOrderData from orderCenter error = " + e.getMessage(), e);
			}
		}
		return result;
	}

}
