/**   
 * @(#)IncrOrderServiceImpl.java	2016年9月19日	下午1:35:43	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.model.OrderCenterResult;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.model.OrderMessageResponse;
import com.elong.nb.model.bean.IncrOrder;
import com.elong.nb.model.enums.OrderChangeStatusEnum;
import com.elong.nb.repository.CommonRepository;
import com.elong.nb.repository.IncrOrderRepository;
import com.elong.nb.service.AbstractDeleteService;
import com.elong.nb.service.IIncrOrderService;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.service.OrderCenterService;
import com.elong.nb.util.DateHandlerUtils;

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
public class IncrOrderServiceImpl extends AbstractDeleteService implements IIncrOrderService {

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

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	/** 
	 * 删除订单增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#delOrderFromDB()    
	 */
	@Override
	public void delOrderFromDB() {
		// 删除30小时以前的数据
		deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
	}

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
		Map<String, Object> messageMap = JSON.parseObject(message);
		Integer orderId = (Integer) messageMap.get("orderId");
		String result = orderCenterService.getOrder(orderId);
		if (StringUtils.isEmpty(result)) {
			logger.error("getOrder from orderCenter error:result is null or empty. ");
			return;
		}
		JSONObject jsonObj = null;
		try {
			jsonObj = JSON.parseObject(result);
		} catch (Exception e) {
			logger.error("getOrder result doesn't parse by JSON.parseObject,result = " + result);
			noticeService.sendMessage("getOrder result doesn't parse by JSON.parseObject",
					"getOrder result doesn't parse by JSON.parseObject,result = " + result);
			return;
		}
		int retcode = (int) jsonObj.get("retcode");
		if (retcode != 0) {
			logger.error("getOrder from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
			noticeService.sendMessage("getOrder from orderCenter error",
					"getOrder from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
			return;
		}
		JSONObject bodyJsonObj = jsonObj.getJSONObject("body");
		Map<String, Object> sourceMap = new HashMap<String, Object>();
		sourceMap.putAll(bodyJsonObj);
		sourceMap.putAll(messageMap);

		// 转换为IncrOrder需要格式
		Map<String, Object> incrOrderMap = convertMap(sourceMap);

		// 判断是否推送V状态
		if (!isPullVStatus(incrOrderMap))
			return;

		// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
		handlerMap(incrOrderMap);

		// 保存到IncrOrder表
		// logger.info("insert incrOrder = " + incrOrderMap);
		long startTime = System.currentTimeMillis();
		incrOrderDao.insert(incrOrderMap);
		long endTime = System.currentTimeMillis();
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
		long startTime = System.currentTimeMillis();
		String rediskey = "Incr.Order.Time";
		String jsonStr = incrSetInfoService.get(rediskey);
		Date startTimeDate = null;
		try {
			startTimeDate = JSON.parseObject(jsonStr, Date.class);
		} catch (Exception e) {
		}
		Date nowDate = new Date();
		if (startTimeDate == null) {
			startTimeDate = DateHandlerUtils.getOffsetDate(nowDate, Calendar.MINUTE, -30);
		}
		jobLogger.info("syncOrderToDB,get time = " + startTimeDate + ",from redis key = " + rediskey);
		Date endTimeDate = DateHandlerUtils.getOffsetDate(nowDate, Calendar.MINUTE, -2);
		if (startTimeDate.after(endTimeDate) || startTimeDate.equals(endTimeDate)) {
			jobLogger.info("startTimeDate after or equals endTimeDate,ignore it this time");
			return;
		}

		// 查询上次截止时间至前2分钟
		String startTimestamp = DateHandlerUtils.formatDate(startTimeDate, "yyyy-MM-dd HH:mm:ss");
		String endTimestamp = DateHandlerUtils.formatDate(endTimeDate, "yyyy-MM-dd HH:mm:ss");
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
		long endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",orderCenterService.getBriefOrdersByTimestamp and parseResult.");

		// 未查到数据，跳过
		if (orderCenterResult == null || orderCenterResult.getRetcode() != 0 || orderCenterResult.getBody() == null) {
			jobLogger.warn("syncOrderToDB ignore,due to retDesc = " + (orderCenterResult == null ? null : orderCenterResult.getRetdesc())
					+ " from getBriefOrdersByTimestamp,endTimestamp = " + endTimestamp);
			return;
		}
		List<Map<String, Object>> orders = orderCenterResult.getBody().getOrders();
		// 未查到数据，跳过
		if (orders == null || orders.size() == 0) {
			jobLogger.warn("syncOrderToDB ignore,due to orders is null or empfy from getBriefOrdersByTimestamp,endTimestamp = "
					+ endTimestamp);
			return;
		}
		jobLogger.info("syncOrderToDB briefOrders size = " + orders.size() + ",endTimestamp = " + endTimestamp);

		Set<Object> orderIds = findOrderIds(orders);
		// 没有需要主动查询的订单号，跳过
		if (orderIds == null || orderIds.size() == 0) {
			jobLogger.warn("syncOrderToDB ignore,due to orderIdList is null or empfy which not be found in OrderMessage,endTimestamp = "
					+ endTimestamp);
			return;
		}
		jobLogger.info("syncOrderToDB needOrderIds size = " + orderIds.size() + ",endTimestamp = " + endTimestamp);

		List<Object> orderIdList = new ArrayList<Object>(orderIds);
		JSONArray bodyJsonArray = new JSONArray();
		int recordCount = orderIds.size();
		int pageSize = 100;
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		startTime = System.currentTimeMillis();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			String getOrderResult = orderCenterService.getOrders(orderIdList.subList(startNum, endNum));
			if (StringUtils.isEmpty(getOrderResult)) {
				jobLogger.warn("getOrders from orderCenter error:getOrderResult is null or empty. ");
				return;
			}
			JSONObject jsonObj = JSON.parseObject(getOrderResult);
			int retcode = (int) jsonObj.get("retcode");
			// 批量获取订单失败，跳过
			if (retcode != 0) {
				jobLogger.warn("getOrders from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc") + ",endTimestamp = "
						+ endTimestamp);
				noticeService.sendMessage(
						"getOrders from orderCenter error:" + DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"),
						"getOrders from orderCenter has been failured,retdesc = " + jsonObj.get("retdesc"));
				return;
			}
			bodyJsonArray.addAll(jsonObj.getJSONArray("body"));
		}
		endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",orderCenterService.getOrders and parseResult");

		if (bodyJsonArray == null || bodyJsonArray.size() == 0) {
			jobLogger.warn("syncOrderToDB ignore,due to bodyJsonArray is null or empfy from getOrders,endTimestamp = " + endTimestamp);
			return;
		}
		jobLogger.info("syncOrderToDB bodyJsonArray size = " + bodyJsonArray.size() + ",endTimestamp = " + endTimestamp);

		Map<Object, Map<String, Object>> tempMap = new HashMap<Object, Map<String, Object>>();
		for (int i = 0; i < bodyJsonArray.size(); i++) {
			Map<String, Object> jsonOrderMap = bodyJsonArray.getJSONObject(i);
			Object orderId = jsonOrderMap.get("orderId");
			tempMap.put(orderId, jsonOrderMap);
		}
		jobLogger.info("syncOrderToDB tempMap size = " + tempMap.size() + ",endTimestamp = " + endTimestamp);

		startTime = System.currentTimeMillis();
		List<Map<String, Object>> incrOrders = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> briefOrderMap : orders) {
			if (briefOrderMap == null || briefOrderMap.size() == 0)
				continue;
			Map<String, Object> sourceMap = new HashMap<String, Object>();
			Object orderId = briefOrderMap.get("orderId");
			Map<String, Object> jsonOrderMap = tempMap.get(orderId);
			if (jsonOrderMap == null || jsonOrderMap.size() == 0)
				continue;
			sourceMap.putAll(jsonOrderMap);
			sourceMap.putAll(briefOrderMap);
			// 转换为IncrOrder需要格式
			Map<String, Object> incrOrderMap = convertMap(sourceMap);
			// 判断是否推送V状态
			if (!isPullVStatus(incrOrderMap))
				continue;
			// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
			handlerMap(incrOrderMap);
			// 保存到IncrOrder表
			incrOrders.add(incrOrderMap);
			jobLogger.info("incrOrderMap = " + JSON.toJSONString(incrOrderMap));
		}
		endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",convertMap,vStatus,HandlerMap and so on");

		// 批量插入IncrOrder
		int incrOrderCount = incrOrders.size();
		int successCount = 0;
		jobLogger.info("IncrOrder BulkInsert start,recordCount = " + incrOrderCount);
		String incrOrderBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrOrderBatchSize");
		int batchSize = StringUtils.isEmpty(incrOrderBatchSize) ? 50 : Integer.valueOf(incrOrderBatchSize);
		int batchCount = (int) Math.ceil(incrOrderCount * 1.0 / pageSize);
		startTime = System.currentTimeMillis();
		for (int batchIndex = 1; batchIndex <= batchCount; batchIndex++) {
			int startNum = (batchIndex - 1) * batchSize;
			int endNum = batchIndex * batchSize > incrOrderCount ? incrOrderCount : batchIndex * batchSize;
			successCount += incrOrderDao.bulkInsert(incrOrders.subList(startNum, endNum));
		}
		endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",IncrOrder BulkInsert,successCount = " + successCount);

		incrSetInfoService.put(rediskey, endTimeDate);
		jobLogger.info("syncOrderToDB,put to redis successfully.key = " + rediskey + ",value = " + endTimeDate);
	}

	/** 
	 * 查找需要批量查询的数据
	 *
	 * @param orders
	 * @return
	 */
	private Set<Object> findOrderIds(List<Map<String, Object>> orders) {
		long startTime = System.currentTimeMillis();
		Set<Object> orderIdList = new HashSet<Object>();
		for (Map<String, Object> orderMap : orders) {
			if (orderMap == null || orderMap.size() == 0)
				continue;

			String status = (String) orderMap.get("status");
			if (StringUtils.isEmpty(status) || !OrderChangeStatusEnum.containCode(status))
				continue;

			Object orderId = orderMap.get("orderId");
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("orderId", orderId);
			params.put("status", status);
			IncrOrder incrOrder = incrOrderDao.getLastIncrOrder(params);
			// 工作库不存在该订单时，兜底查询
			if (incrOrder == null) {
				orderIdList.add(orderId);
				continue;
			}
			Date orderTimestamp = null;
			try {
				String orderTimestampStr = (String) orderMap.get("orderTimestamp");
				orderTimestamp = DateUtils.parseDate(orderTimestampStr, new String[] { "yyyy-MM-dd HH:mm:ss.SSS",
						"yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
				orderTimestampStr = DateHandlerUtils.formatDate(orderTimestamp, "yyyy-MM-dd HH:mm:ss");
				orderTimestamp = DateUtils.parseDate(orderTimestampStr, new String[] { "yyyy-MM-dd HH:mm:ss" });
			} catch (ParseException e) {
				jobLogger.error("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
			}
			// 工作库时间小于订单时间戳时,兜底查询
			if (orderTimestamp != null && incrOrder.getChangeTime().before(orderTimestamp)) {
				orderIdList.add(orderId);
			}
		}
		long endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",findOrderIds,orderIdList size = " + orderIdList.size());
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

		try {
			String affiliateConfirmationId = StringUtils.EMPTY;
			Object nbGuid = sourceMap.get("nbGuid");
			String nbGuidStr = (nbGuid == null) ? StringUtils.EMPTY : (String) nbGuid;
			if (StringUtils.isNotEmpty(nbGuidStr)) {
				String[] values = StringUtils.split(nbGuidStr, "|", -1);
				if (values.length > 0) {
					affiliateConfirmationId = values[0].trim();
				}
				// 下次数据库申请增加长度
				if (StringUtils.length(affiliateConfirmationId) > 50) {
					affiliateConfirmationId = StringUtils.substring(affiliateConfirmationId, 0, 50);
				}
			}
			targetMap.put("AffiliateConfirmationId", affiliateConfirmationId);
		} catch (Exception e) {
			logger.error("AffiliateConfirmationId doHandler error " + e.getMessage(), e);
		}
		targetMap.put("Status", sourceMap.get("status"));
		targetMap.put("payStatus", sourceMap.get("payStatus") == null ? -1 : sourceMap.get("payStatus"));
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
		String filterOrderFromStrV = CommonsUtil.CONFIG_PROVIDAR.getProperty("FilterOrderFromStrV");
		if (StringUtils.isNotEmpty(filterOrderFromStrV)) {
			// long startTime = System.currentTimeMillis();
			String[] orderFroms = StringUtils.split(filterOrderFromStrV, ",", -1);
			String currentOrderFrom = String.valueOf(incrOrderMap.get("OrderFrom"));
			String status = incrOrderMap.get("Status").toString();
			// long endTime = System.currentTimeMillis();
			// logger.info("use time = " + (endTime - startTime) + ",FilterOrderFromStrV");
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
			// long startTime = System.currentTimeMillis();
			OrderFromResult orderProxy = commonRepository.getProxyInfoByOrderFrom((int) incrOrderMap.get("OrderFrom"));
			if (orderProxy != null && orderProxy.getData() != null && !StringUtils.isEmpty(orderProxy.getData().getProxyId())) {
				incrOrderMap.put("ProxyId", orderProxy.getData().getProxyId());
				incrOrderMap.put("CardNo", orderProxy.getData().getCardNo());
				incrOrderMap.put("Status", "D");
			}
		}
	}

	@Override
	protected List<BigInteger> getIncrIdList(Map<String, Object> params) {
		return incrOrderDao.getIncrIdList(params);
	}

	@Override
	protected int deleteByIncrIdList(List<BigInteger> incrIdList) {
		return incrOrderDao.deleteByIncrIdList(incrIdList);
	}

	@Override
	protected void logger(String message) {
		logger.info(message);
	}

}
