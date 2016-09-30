/**   
 * @(#)IncrOrderServiceImpl.java	2016年9月19日	下午1:35:43	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.model.OrderMessageResponse;
import com.elong.nb.model.enums.OrderChangeStatusEnum;
import com.elong.nb.repository.CommonRepository;
import com.elong.nb.repository.IncrOrderRepository;
import com.elong.nb.service.IIncrOrderService;
import com.elong.nb.util.HttpClientUtils;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

/**
 * IncrOrder服务接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月19日 下午1:35:43   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class IncrOrderServiceImpl implements IIncrOrderService {

	private static final Logger logger = Logger.getLogger("IncrOrderLogger");

	@Resource
	private IncrOrderDao incrOrderDao;

	@Resource
	private IncrOrderRepository incrOrderRepository;

	@Resource
	private CommonRepository commonRepository;

	/** 
	 * 处理订单中心消息
	 *
	 * @param message 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#handlerMessage(java.lang.String)    
	 */
	@Override
	public void handlerMessage(final String message) {
		// 删除30小时以前的数据
		int count = incrOrderRepository.deleteExpireIncrData(com.elong.nb.util.DateUtils.getDBExpireDate());
		logger.info("IncrOrder delete successfully,count = " + count);

		// 构建请求参数
		Map<String, Object> map = JSON.parseObject(message);
		Integer orderId = (Integer) map.get("orderId");
		String reqData = "{ \"orderId\":%d,\"fields\":\"sumPrice,status,roomCount,proxy,sourceOrderId,orderFrom,checkOutDate,checkInDate,cardNo\" }";
		reqData = String.format(reqData, orderId);

		// 从订单中心获取订单数据
		String reqUrl = PropertiesHelper.getEnvProperties("GetOrderUrlFromOrderCenter", "config").toString();
		logger.info("httpPost,reqUrl = " + reqUrl);
		logger.info("httpPost,reqData = " + reqData);
		String result = null;
		try {
			result = HttpClientUtils.httpPost(reqUrl, reqData, "application/json;charset=utf8");
		} catch (Exception e) {
			throw new IllegalStateException("getOrder from orderCenter error = " + e.getMessage());
		}
		logger.info("httpPost,result = " + result);
		JSONObject jsonObj = JSON.parseObject(result);
		int retcode = (int) jsonObj.get("retcode");
		if (retcode != 0) {
			throw new IllegalStateException("getOrder from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
		}
		JSONObject bodyJsonObj = jsonObj.getJSONObject("body");

		// 转换为IncrOrder需要格式
		Map<String, Object> incrOrderMap = convertMap(bodyJsonObj);

		// 判断是否推送V状态
		String FilterOrderFromStrV = PropertiesHelper.getEnvProperties("FilterOrderFromStrV", "config").toString();
		if (StringUtils.isNotEmpty(FilterOrderFromStrV)) {
			String[] orderFroms = StringUtils.split(FilterOrderFromStrV, ",", -1);
			String currentOrderFrom = String.valueOf(incrOrderMap.get("OrderFrom"));
			String status = incrOrderMap.get("Status").toString();
			if (!ArrayUtils.contains(orderFroms, currentOrderFrom) && StringUtils.equals(OrderChangeStatusEnum.V.toString(), status)) {
				logger.info("status = " + status + ",orderFrom = " + currentOrderFrom
						+ "ignore sync to incrOrder, due to no in value whose key is 'FilterOrderFromStrV' of 'config.properties'");
				return;
			}
		}

		// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
		if (incrOrderMap.get("CardNo").toString() == "49") {
			OrderFromResult orderProxy = commonRepository.getProxyInfoByOrderFrom((int) incrOrderMap.get("OrderFrom"));
			if (orderProxy != null && orderProxy.getData() != null && !StringUtils.isEmpty(orderProxy.getData().getProxyId())) {
				incrOrderMap.put("ProxyId", orderProxy.getData().getProxyId());
				incrOrderMap.put("CardNo", orderProxy.getData().getCardNo());
				incrOrderMap.put("Status", "D");
			}
		}

		// 保存到IncrOrder表
		logger.info("insert incrOrder = " + incrOrderMap);
		incrOrderDao.insert(incrOrderMap);
		logger.info("insert incrOrder successfully.");
	}

	/** 
	 * 检查订单中心消息 
	 *
	 * @param message
	 * @return 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#checkMessage(java.lang.String)    
	 */
	@Override
	public OrderMessageResponse checkMessage(final String message) {
		OrderMessageResponse result = new OrderMessageResponse();
		// json字符串为空，直接返回错误
		if (StringUtils.isEmpty(message)) {
			result.setExceptionMessage("message is null or empty");
			result.setResponseCode(OrderMessageResponse.FAILURE);
			return result;
		}

		// json转换的map为空或者不包含必须字段
		Map<String, Object> map = JSON.parseObject(message);
		if (MapUtils.isEmpty(map)) {
			result.setExceptionMessage("the map which is converted from message is null or empty.");
		} else if (!map.containsKey("status") || !map.containsKey("orderTimestamp") || !map.containsKey("orderId")) {
			result.setExceptionMessage("the items in['status','orderTimestamp','orderId'] doesn't exist at least.");
		}
		if (StringUtils.isNotEmpty(result.getExceptionMessage())) {
			result.setResponseCode(OrderMessageResponse.FAILURE);
			return result;
		}

		// 非订单增量过滤状态数据直接忽略
		String status = (String) map.get("status");
		if (StringUtils.isEmpty(status)) {
			logger.info("status is null or empty.");
			result.setResponseCode(OrderMessageResponse.IGNORE);
			return result;
		} else if (!OrderChangeStatusEnum.containCode(status)) {
			logger.info("status = " + status + ",no in[" + OrderChangeStatusEnum.toAllCode() + "]");
			result.setResponseCode(OrderMessageResponse.IGNORE);
			return result;
		}

		// 具体字段校验
		List<String> errorList = new ArrayList<String>();
		String orderTimestamp = (String) map.get("orderTimestamp");
		Integer orderId = (Integer) map.get("orderId");

		if (StringUtils.isEmpty(orderTimestamp)) {
			errorList.add("orderTimestamp is null or empty.");
		} else {
			try {
				DateUtils.parseDate(orderTimestamp, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss",
						"yyyy-MM-dd HH:mm:ss.SSS" });
			} catch (ParseException e) {
				errorList
						.add("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss','yyyy-MM-dd HH:mm:ss.SSS']");
			}
		}
		if (orderId == null) {
			errorList.add("orderId is null or empty.");
		}

		if (CollectionUtils.isEmpty(errorList)) {
			result.setResponseCode(OrderMessageResponse.SUCCESS);
		} else {
			result.setResponseCode(OrderMessageResponse.FAILURE);
			result.setExceptionMessage(errorList.toString());
		}
		return result;
	}

	/** 
	 * 获取订单数据转换为IncrOrder需要格式
	 *
	 * @param sourceMap
	 * @return 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#convertMap(java.util.Map)    
	 */
	@Override
	public Map<String, Object> convertMap(Map<String, Object> sourceMap) {
		Map<String, Object> targetMap = new HashMap<String, Object>();

		try {
			String orderTimestamp = (String) sourceMap.get("orderTimestamp");
			Date ChangeTime = DateUtils.parseDate(orderTimestamp, new String[] { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss:SSS",
					"yyyy-MM-dd HH:mm:ss" });
			targetMap.put("ChangeTime", ChangeTime);
		} catch (ParseException e) {
			logger.error("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}

		targetMap.put("OrderId", sourceMap.get("orderId"));
		targetMap.put("AffiliateConfirmationId",
				sourceMap.get("sourceOrderId") == null ? StringUtils.EMPTY : sourceMap.get("sourceOrderId"));
		targetMap.put("Status", sourceMap.get("status"));
		try {
			String checkInDate = (String) sourceMap.get("checkInDate");
			Date ArrivalDate = DateUtils.parseDate(checkInDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("ArrivalDate", ArrivalDate);
		} catch (ParseException e) {
			logger.error("checkInDate is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}
		try {
			String checkOutDate = (String) sourceMap.get("checkOutDate");
			Date DepartureDate = DateUtils.parseDate(checkOutDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("DepartureDate", DepartureDate);
		} catch (ParseException e) {
			logger.error("checkOutDate is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}
		targetMap.put("TotalPrice", sourceMap.get("sumPrice"));
		targetMap.put("NumberOfRooms", sourceMap.get("roomCount"));
		targetMap.put("CardNo", sourceMap.get("cardNo"));
		targetMap.put("OrderFrom", sourceMap.get("orderFrom"));
		targetMap.put("ProxyId", sourceMap.get("proxy"));
		targetMap.put("InsertTime", new Date());
		return targetMap;
	}

}
