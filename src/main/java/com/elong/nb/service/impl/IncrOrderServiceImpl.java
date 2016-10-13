/**   
 * @(#)IncrOrderServiceImpl.java	2016年9月19日	下午1:35:43	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.model.BriefOrder;
import com.elong.nb.model.OrderCenterResult;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.model.OrderMessageResponse;
import com.elong.nb.model.bean.IncrOrder;
import com.elong.nb.model.enums.OrderChangeStatusEnum;
import com.elong.nb.repository.CommonRepository;
import com.elong.nb.repository.IncrOrderRepository;
import com.elong.nb.service.IIncrOrderService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.service.OrderCenterService;
import com.elong.nb.util.DateHandlerUtils;
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
	private static final Logger jobLogger = Logger.getLogger("IncrOrderJobLogger");

	@Resource
	private IncrOrderDao incrOrderDao;

	@Resource
	private IncrOrderRepository incrOrderRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private OrderCenterService orderCenterService;

	@Resource
	private INoticeService noticeService;

	/** 
	 * 处理订单中心消息
	 *
	 * @param message 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#handlerMessage(java.lang.String)    
	 */
	@Override
	public void handlerMessage(final String message) {
		// 订单中心获取订单
		Map<String, Object> map = JSON.parseObject(message);
		Integer orderId = (Integer) map.get("orderId");
		String result = orderCenterService.getOrder(orderId);
		if (StringUtils.isEmpty(result)) {
			logger.error("getOrder from orderCenter error:result is null or empty. ");
			return;
		}
		JSONObject jsonObj = JSON.parseObject(result);
		int retcode = (int) jsonObj.get("retcode");
		if (retcode != 0) {
			logger.error("getOrder from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
			noticeService.sendMessage("getOrder from orderCenter error:" + DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"),
					"getOrder from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
			return;
		}
		JSONObject bodyJsonObj = jsonObj.getJSONObject("body");

		// 转换为IncrOrder需要格式
		Map<String, Object> incrOrderMap = convertMap(bodyJsonObj);

		// 判断是否推送V状态
		if (!isPullVStatus(incrOrderMap))
			return;

		// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
		handlerMap(incrOrderMap);

		// 保存到IncrOrder表
		logger.info("insert incrOrder = " + incrOrderMap);
		long startTime = new Date().getTime();
		incrOrderDao.insert(incrOrderMap);
		long endTime = new Date().getTime();
		logger.info("use time = " + (endTime - startTime) + ",insert incrOrder successfully.");
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
	 * 同步订单增量（兜底：在订单组主动推送消息挂调时）
	 * 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#syncOrderToDB()    
	 */
	@Override
	public void syncOrderToDB() {
		// 删除30小时以前的数据
		long startTime = new Date().getTime();
		int count = incrOrderRepository.deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
		long endTime = new Date().getTime();
		jobLogger.info("use time = " + (endTime - startTime) + ",IncrOrder delete successfully,count = " + count);

		// 查询前3分钟至前2分钟
		Date endTimeDate = new Date();
		String startTimestamp = DateHandlerUtils.getOffsetDateStr(endTimeDate, Calendar.MINUTE, -3, "yyyy-MM-dd HH:mm:ss");
		String endTimestamp = DateHandlerUtils.getOffsetDateStr(endTimeDate, Calendar.MINUTE, -2, "yyyy-MM-dd HH:mm:ss");
		String getBriefOrdersResult = orderCenterService.getBriefOrdersByTimestamp(startTimestamp, endTimestamp);
		OrderCenterResult orderCenterResult = null;
		try {
			orderCenterResult = JSON.parseObject(getBriefOrdersResult, OrderCenterResult.class);
		} catch (Exception e) {
			jobLogger.error("JSON.parseObject error = " + e.getMessage() + ",getBriefOrdersResult = " + getBriefOrdersResult);
			noticeService.sendMessage("JSON.parseObject error = " + e.getMessage() + ",getBriefOrdersResult = " + getBriefOrdersResult,
					ExceptionUtils.getFullStackTrace(e));
			return;
		}

		// 未查到数据，跳过
		if (orderCenterResult == null || orderCenterResult.getRetcode() != 0 || orderCenterResult.getBody() == null) {
			jobLogger.info("syncOrderToDB ignore,due to retDesc = " + orderCenterResult.getRetdesc()
					+ " from getBriefOrdersByTimestamp,endTimestamp = " + endTimestamp);
			return;
		}
		List<BriefOrder> orders = orderCenterResult.getBody().getOrders();
		// 未查到数据，跳过
		if (orders == null || orders.size() == 0) {
			jobLogger.info("syncOrderToDB ignore,due to orders is null or empfy from getBriefOrdersByTimestamp,endTimestamp = " + endTimestamp);
			return;
		}

		List<Long> orderIds = findOrderIds(orders);
		// 没有需要主动查询的订单号，跳过
		if (orderIds == null || orderIds.size() == 0) {
			jobLogger.info("syncOrderToDB ignore,due to orderIdList is null or empfy which not be found in OrderMessage,endTimestamp = "
					+ endTimestamp);
			return;
		}

		jobLogger.info("syncOrderToDB,orderIds size = " + orderIds.size() + ",endTimestamp = " + endTimestamp);
		String getOrderResult = orderCenterService.getOrders(orderIds);
		if (StringUtils.isEmpty(getOrderResult)) {
			jobLogger.error("getOrders from orderCenter error:getOrderResult is null or empty. ");
			return;
		}
		JSONObject jsonObj = JSON.parseObject(getOrderResult);
		int retcode = (int) jsonObj.get("retcode");
		// 批量获取订单失败，跳过
		if (retcode != 0) {
			jobLogger.info("getOrders from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc") + ",endTimestamp = "
					+ endTimestamp);
			noticeService.sendMessage("getOrders from orderCenter error:" + DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"),
					"getOrders from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
			return;
		}
		JSONArray bodyJsonArray = jsonObj.getJSONArray("body");
		if (bodyJsonArray == null || bodyJsonArray.size() == 0) {
			jobLogger.info("syncOrderToDB ignore,due to bodyJsonArray is null or empfy from getOrders,endTimestamp = " + endTimestamp);
			return;
		}
		int successCount = 0;
		for (int i = 0; i < bodyJsonArray.size(); i++) {
			Map<String, Object> sourceMap = bodyJsonArray.getJSONObject(i);
			// 转换为IncrOrder需要格式
			Map<String, Object> incrOrderMap = convertMap(sourceMap);
			// 判断是否推送V状态
			if (!isPullVStatus(incrOrderMap))
				continue;
			// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
			handlerMap(incrOrderMap);
			// 保存到IncrOrder表
			successCount += incrOrderDao.insert(incrOrderMap);
		}
		jobLogger.info("syncOrderToDB,Insert incrOrder successfully.successCount = " + successCount);
	}

	/** 
	 * 查找需要批量查询的数据
	 *
	 * @param orders
	 * @return
	 */
	private List<Long> findOrderIds(List<BriefOrder> orders) {
		List<Long> orderIdList = new ArrayList<Long>();
		for (BriefOrder order : orders) {
			if (order == null || StringUtils.isEmpty(order.getStatus()))
				continue;
			if (!OrderChangeStatusEnum.containCode(order.getStatus()))
				continue;

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("orderId", order.getOrderId());
			IncrOrder incrOrder = incrOrderDao.getLastIncrOrder(params);
			// 工作库不存在该订单时，兜底查询
			if (incrOrder == null) {
				orderIdList.add(order.getOrderId());
				continue;
			}
			Date orderTimestamp = null;
			try {
				orderTimestamp = DateUtils.parseDate(order.getOrderTimestamp(), new String[] { "yyyy-MM-dd HH:mm:ss.SSS",
						"yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			} catch (ParseException e) {
				logger.error("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
			}
			// 工作库时间小于订单时间戳时,兜底查询
			if (orderTimestamp != null && incrOrder.getChangeTime().before(orderTimestamp)) {
				orderIdList.add(order.getOrderId());
			}
		}
		return orderIdList;
	}

	/** 
	 * 获取订单数据转换为IncrOrder需要格式
	 *
	 * @param sourceMap
	 * @return 
	 *
	 */
	private Map<String, Object> convertMap(Map<String, Object> sourceMap) {
		Map<String, Object> targetMap = new HashMap<String, Object>();

		try {
			String orderTimestamp = (String) sourceMap.get("orderTimestamp");
			Date changeTime = DateUtils.parseDate(orderTimestamp, new String[] { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss:SSS",
					"yyyy-MM-dd HH:mm:ss" });
			targetMap.put("ChangeTime", changeTime);
		} catch (ParseException e) {
			logger.error("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}

		targetMap.put("OrderId", sourceMap.get("orderId"));
		targetMap.put("AffiliateConfirmationId",
				sourceMap.get("sourceOrderId") == null ? StringUtils.EMPTY : sourceMap.get("sourceOrderId"));
		targetMap.put("Status", sourceMap.get("status"));
		try {
			String checkInDate = (String) sourceMap.get("checkInDate");
			Date arrivalDate = DateUtils.parseDate(checkInDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("ArrivalDate", arrivalDate);
		} catch (ParseException e) {
			logger.error("checkInDate is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}
		try {
			String checkOutDate = (String) sourceMap.get("checkOutDate");
			Date departureDate = DateUtils.parseDate(checkOutDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("DepartureDate", departureDate);
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

	/** 
	 * 是否推送V状态
	 *
	 * @param incrOrderMap
	 * @return
	 */
	public boolean isPullVStatus(Map<String, Object> incrOrderMap) {
		String filterOrderFromStrV = PropertiesHelper.getEnvProperties("FilterOrderFromStrV", "config").toString();
		if (StringUtils.isNotEmpty(filterOrderFromStrV)) {
//			long startTime = new Date().getTime();
			String[] orderFroms = StringUtils.split(filterOrderFromStrV, ",", -1);
			String currentOrderFrom = String.valueOf(incrOrderMap.get("OrderFrom"));
			String status = incrOrderMap.get("Status").toString();
//			long endTime = new Date().getTime();
//			logger.info("use time = " + (endTime - startTime) + ",FilterOrderFromStrV");
			if (!ArrayUtils.contains(orderFroms, currentOrderFrom) && StringUtils.equals(OrderChangeStatusEnum.V.toString(), status)) {
				logger.info("status = " + status + ",orderFrom = " + currentOrderFrom
						+ "ignore sync to incrOrder, due to no in value whose key is 'FilterOrderFromStrV' of 'config.properties'");
				return false;
			}
		}
		return true;
	}

	/** 
	 * 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
	 *
	 * @param incrOrderMap
	 */
	private void handlerMap(Map<String, Object> incrOrderMap) {
		String cardNo = (incrOrderMap.get("CardNo") == null) ? StringUtils.EMPTY : incrOrderMap.get("CardNo").toString();
		if (StringUtils.equals("49", cardNo)) {
//			long startTime = new Date().getTime();
			OrderFromResult orderProxy = commonRepository.getProxyInfoByOrderFrom((int) incrOrderMap.get("OrderFrom"));
			if (orderProxy != null && orderProxy.getData() != null && !StringUtils.isEmpty(orderProxy.getData().getProxyId())) {
				incrOrderMap.put("ProxyId", orderProxy.getData().getProxyId());
				incrOrderMap.put("CardNo", orderProxy.getData().getCardNo());
				incrOrderMap.put("Status", "D");
			}
//			long endTime = new Date().getTime();
//			logger.info("use time = " + (endTime - startTime) + ",commonRepository.getProxyInfoByOrderFrom");
		}
	}

}
