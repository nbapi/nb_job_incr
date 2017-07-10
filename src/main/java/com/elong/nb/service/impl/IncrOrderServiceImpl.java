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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.IncrInsertStatistic;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.model.OrderCenterResult;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.model.bean.IncrOrder;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.model.enums.EnumPayStatus;
import com.elong.nb.model.enums.OrderChangeStatusEnum;
import com.elong.nb.repository.CommonRepository;
import com.elong.nb.service.AbstractDeleteService;
import com.elong.nb.service.IIncrOrderService;
import com.elong.nb.service.IIncrSetInfoService;
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

	private static final Logger jobLogger = Logger.getLogger("IncrOrderJobLogger");

	protected static final Logger minitorLogger = Logger.getLogger("minitorLogger");

	protected ExecutorService executorService = Executors.newFixedThreadPool(1);

	@Resource
	private IncrOrderDao incrOrderDao;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private OrderCenterService orderCenterService;

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
		String keepHours = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrOrder.delete.keepHours");
		keepHours = StringUtils.isEmpty(keepHours) ? "-360" : StringUtils.trim(keepHours);
		int offset = -1;
		try {
			offset = Integer.valueOf(keepHours);
		} catch (NumberFormatException e) {
			offset = -360;
		}
		deleteExpireIncrData(DateHandlerUtils.getDBExpireDate(offset));
	}

	/** 
	 * 同步订单增量（兜底：在订单组主动推送消息挂调时）
	 * 
	 *
	 * @see com.elong.nb.service.IIncrOrderService#syncOrderToDB()    
	 */
	@Override
	public void syncOrderToDB() {
		String rediskey = "Incr.Order.Time";
		String jsonStr = incrSetInfoService.get(rediskey);
		Date startTimeDate = null;
		try {
			startTimeDate = JSON.parseObject(jsonStr, Date.class);
		} catch (Exception e) {
		}
		Date nowDate = new Date();
		if (startTimeDate == null) {
			startTimeDate = DateHandlerUtils.getOffsetDate(nowDate, Calendar.MINUTE, -3);
		}
		jobLogger.info("syncOrderToDB,get time = " + startTimeDate + ",from redis key = " + rediskey);
		Date endTimeDate = DateHandlerUtils.getOffsetDate(nowDate, Calendar.MINUTE, -2);
		if (startTimeDate.after(endTimeDate) || startTimeDate.equals(endTimeDate)) {
			jobLogger.info("startTimeDate after or equals endTimeDate,ignore it this time");
			return;
		}
		syncIncrOrder(startTimeDate, endTimeDate);

		incrSetInfoService.put(rediskey, endTimeDate);
		jobLogger.info("syncOrderToDB,put to redis successfully.key = " + rediskey + ",value = " + endTimeDate);
	}

	/** 
	 * 指定时间范围的订单增量捡漏
	 *
	 * @param startTimeDate
	 * @param endTimeDate
	 */
	private void syncIncrOrder(Date startTimeDate, Date endTimeDate) {
		long startTime = System.currentTimeMillis();
		// 查询上次截止时间至前1分钟
		List<Map<String, Object>> briefOrderList = getBriefOrders(startTimeDate, endTimeDate);
		jobLogger.info("syncOrderToDB briefOrders size = " + briefOrderList.size() + ",endTimestamp = " + endTimeDate);

		List<Map<String, Object>> missBriefOrderList = findMissBriefOrderList(briefOrderList);
		// 没有需要主动查询的订单号，跳过
		if (missBriefOrderList == null || missBriefOrderList.size() == 0) {
			jobLogger.warn("ignore,due to missBriefOrderList is null or empfy which not be found in OrderMessage,endTimestamp = "
					+ endTimeDate);
			return;
		}

		Set<Object> missBriefOrderIdSet = new HashSet<Object>();
		for (Map<String, Object> missBriefOrder : missBriefOrderList) {
			if (missBriefOrder == null || missBriefOrder.get("orderId") == null)
				continue;
			missBriefOrderIdSet.add(missBriefOrder.get("orderId"));
		}
		jobLogger.info("syncOrderToDB missBriefOrderIdSet size = " + missBriefOrderIdSet.size() + ",endTimestamp = " + endTimeDate);

		List<Object> orderIdList = new ArrayList<Object>(missBriefOrderIdSet);
		JSONArray bodyJsonArray = new JSONArray();
		int recordCount = missBriefOrderIdSet.size();
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
						+ endTimeDate);
				return;
			}
			bodyJsonArray.addAll(jsonObj.getJSONArray("body"));
		}
		long endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",orderCenterService.getOrders and parseResult");

		if (bodyJsonArray == null || bodyJsonArray.size() == 0) {
			jobLogger.warn("syncOrderToDB ignore,due to bodyJsonArray is null or empfy from getOrders,endTimestamp = " + endTimeDate);
			return;
		}
		jobLogger.info("syncOrderToDB bodyJsonArray size = " + bodyJsonArray.size() + ",endTimestamp = " + endTimeDate);

		Map<Object, Map<String, Object>> tempMap = new HashMap<Object, Map<String, Object>>();
		for (int i = 0; i < bodyJsonArray.size(); i++) {
			Map<String, Object> jsonOrderMap = bodyJsonArray.getJSONObject(i);
			Object orderId = jsonOrderMap.get("orderId");
			tempMap.put(orderId, jsonOrderMap);
		}
		jobLogger.info("syncOrderToDB tempMap size = " + tempMap.size() + ",endTimestamp = " + endTimeDate);

		startTime = System.currentTimeMillis();
		int testSize1 = 0;
		int testSize2 = 0;
		int testSize3 = 0;
		List<Map<String, Object>> incrOrders = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> briefOrderMap : missBriefOrderList) {
			if (briefOrderMap == null || briefOrderMap.size() == 0) {
				testSize1++;
				continue;
			}
			Map<String, Object> sourceMap = new HashMap<String, Object>();
			Object orderId = briefOrderMap.get("orderId");
			Map<String, Object> jsonOrderMap = tempMap.get(orderId);
			if (jsonOrderMap == null || jsonOrderMap.size() == 0) {
				testSize2++;
				continue;
			}
			sourceMap.putAll(jsonOrderMap);
			sourceMap.putAll(briefOrderMap);
			// 转换为IncrOrder需要格式
			Map<String, Object> incrOrderMap = convertMap(sourceMap);
			// 判断是否推送V状态
			if (!isPullVStatus(incrOrderMap)) {
				testSize3++;
				continue;
			}
			// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
			handlerMap(incrOrderMap);
			// 保存到IncrOrder表
			incrOrders.add(incrOrderMap);
			jobLogger.info("incrOrderMap = " + JSON.toJSONString(incrOrderMap));
		}
		jobLogger.info("testSize1 = " + testSize1 + ",testSize2 = " + testSize2 + ",testSize3 = " + testSize3);
		endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",convertMap,vStatus,HandlerMap and so on");

		// 批量插入IncrOrder
		builkInsert(incrOrders);
	}

	private void builkInsert(final List<Map<String, Object>> incrOrders) {
		int recordCount = incrOrders.size();
		String incrOrderBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrOrderBatchSize");
		int pageSize = StringUtils.isEmpty(incrOrderBatchSize) ? 50 : Integer.valueOf(incrOrderBatchSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		jobLogger.info("IncrOrder BulkInsert start,recordCount = " + recordCount + ",batchCount = " + pageCount + ",batchSize = "
				+ pageSize);
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			int count = 0;//incrOrderDao.bulkInsert(incrOrders.subList(startNum, endNum));
			jobLogger.info("IncrOrder BulkInsert,count = " + count + ",pageIndex = " + pageIndex);
			successCount += count;
		}
		jobLogger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrOrder BulkInsert,successCount = " + successCount);

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String businessType = "nbincrinsert";
					for (Map<String, Object> incrOrderMap : incrOrders) {
						IncrInsertStatistic statisticModel = new IncrInsertStatistic();
						statisticModel.setBusiness_type(businessType);
						statisticModel.setIncrType(EnumIncrType.Order.name());
						statisticModel.setProxyId((String) incrOrderMap.get("ProxyId"));
						statisticModel.setChangeTime(DateHandlerUtils.formatDate((Date) incrOrderMap.get("ChangeTime"),
								"yyyy-MM-dd HH:mm:ss"));
						statisticModel.setInsertTime(DateHandlerUtils.formatDate((Date) incrOrderMap.get("InsertTime"),
								"yyyy-MM-dd HH:mm:ss"));
						statisticModel.setLog_time(DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
						Map<String, Object> slaveIncrOrder = incrOrderDao.getLastIncrOrderFromRead();
						statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate((Date) slaveIncrOrder.get("InsertTime"),
								"yyyy-MM-dd HH:mm:ss"));
						minitorLogger.info(JSON.toJSONString(statisticModel));
					}
				} catch (Exception e) {
				}
			}
		});

	}

	/** 
	 * 分页查询getBriefOrdersByTimestamp
	 *
	 * @param startTimeDate
	 * @param endTimeDate
	 * @return
	 */
	private List<Map<String, Object>> getBriefOrders(Date startTimeDate, Date endTimeDate) {
		List<Map<String, Object>> briefOrderList = new ArrayList<Map<String, Object>>();
		String startTimestamp = DateHandlerUtils.formatDate(startTimeDate, "yyyy-MM-dd HH:mm:ss.SSS");
		String endTimestamp = DateHandlerUtils.formatDate(endTimeDate, "yyyy-MM-dd HH:mm:ss.SSS");
		boolean hasNext = true;
		while (hasNext) {
			long startTime = System.currentTimeMillis();
			String getBriefOrdersResult = orderCenterService.getBriefOrdersByTimestamp(startTimestamp, endTimestamp);
			OrderCenterResult orderCenterResult = null;
			try {
				orderCenterResult = JSON.parseObject(getBriefOrdersResult, OrderCenterResult.class);
			} catch (Exception e) {
				throw new IllegalStateException("getBriefOrdersByTimestamp JSON.parseObject error = " + e.getMessage() + ",message = "
						+ StringUtils.substring(getBriefOrdersResult, 0, 100));
			}
			long endTime = System.currentTimeMillis();
			jobLogger.info("use time = " + (endTime - startTime) + ",orderCenterService.getBriefOrdersByTimestamp and parseResult.");

			// 查询错误，放弃此次同步
			if (orderCenterResult == null || orderCenterResult.getRetcode() != 0 || orderCenterResult.getBody() == null) {
				throw new IllegalStateException("getBriefOrdersByTimestamp orderCenterResult is null or retDesc = "
						+ (orderCenterResult == null ? null : orderCenterResult.getRetdesc()));
			}
			List<Map<String, Object>> orders = orderCenterResult.getBody().getOrders();
			if (orders != null && orders.size() > 0) {
				briefOrderList.addAll(orders);
			}
			hasNext = orderCenterResult.getBody().isHasNext();
			if (hasNext) {
				startTimestamp = orders.get(orders.size() - 1).get("orderTimestamp").toString();
			}
		}
		return briefOrderList;
	}

	/** 
	 * 查找需要批量查询的数据
	 *
	 * @param orders
	 * @return
	 */
	private List<Map<String, Object>> findMissBriefOrderList(List<Map<String, Object>> orders) {
		long startTime = System.currentTimeMillis();
		List<Map<String, Object>> missBriefOrderList = new ArrayList<Map<String, Object>>();
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
				missBriefOrderList.add(orderMap);
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
				missBriefOrderList.add(orderMap);
			}
		}
		long endTime = System.currentTimeMillis();
		jobLogger.info("use time = " + (endTime - startTime) + ",findMissBriefOrderList,missBriefOrderList size = "
				+ missBriefOrderList.size());
		return missBriefOrderList;
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
			jobLogger.error("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
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
			jobLogger.error("AffiliateConfirmationId doHandler error " + e.getMessage(), e);
		}
		targetMap.put("Status", sourceMap.get("status"));
		targetMap.put("payStatus", sourceMap.get("payStatus") == null ? -1 : sourceMap.get("payStatus"));
		try {
			String checkInDate = (String) sourceMap.get("checkInDate");
			Date arrivalDate = DateUtils.parseDate(checkInDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("ArrivalDate", arrivalDate);
		} catch (ParseException e) {
			jobLogger.error("checkInDate is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
		}
		try {
			String checkOutDate = (String) sourceMap.get("checkOutDate");
			Date departureDate = DateUtils.parseDate(checkOutDate, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss" });
			targetMap.put("DepartureDate", departureDate);
		} catch (ParseException e) {
			jobLogger.error("checkOutDate is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']", e);
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
		int payStatus = Integer.parseInt(incrOrderMap.get("payStatus").toString());
		if (payStatus != EnumPayStatus.UNEXISTSPAY.getValue() && payStatus != EnumPayStatus.WAITING.getValue())
			return true;
		String filterOrderFromStrV = CommonsUtil.CONFIG_PROVIDAR.getProperty("FilterOrderFromStrV");
		if (StringUtils.isEmpty(filterOrderFromStrV))
			return true;
		String[] orderFroms = StringUtils.split(filterOrderFromStrV, ",", -1);
		String currentOrderFrom = String.valueOf(incrOrderMap.get("OrderFrom"));
		String status = incrOrderMap.get("Status").toString();
		if (!ArrayUtils.contains(orderFroms, currentOrderFrom) && StringUtils.equals(OrderChangeStatusEnum.V.toString(), status)) {
			jobLogger.info("status = " + status + ",orderFrom = " + currentOrderFrom
					+ ",ignore sync to incrOrder, due to no in value whose key is 'FilterOrderFromStrV' of 'config.properties'");
			return false;
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
		if (!StringUtils.equals("49", cardNo))
			return;
		incrOrderMap.put("Status", "D");
		OrderFromResult orderProxy = commonRepository.getProxyInfoByOrderFrom((int) incrOrderMap.get("OrderFrom"));
		if (orderProxy == null || orderProxy.getData() == null || StringUtils.isEmpty(orderProxy.getData().getProxyId()))
			return;
		incrOrderMap.put("ProxyId", orderProxy.getData().getProxyId());
		incrOrderMap.put("CardNo", orderProxy.getData().getCardNo());
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
		jobLogger.info(message);
	}

}
