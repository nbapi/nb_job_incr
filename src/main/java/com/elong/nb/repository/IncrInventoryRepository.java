/**   
 * @(#)IncrInventoryRepository.java	2016年9月21日	下午4:23:56	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeListRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeMinIDRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeMinIDResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.IProductForPartnerServiceContract;
import com.elong.nb.agent.ProductForPartnerServiceContract.InventoryChangeModel;
import com.elong.nb.agent.ProductForPartnerServiceContract.ResourceInventoryState;
import com.elong.nb.common.checklist.Constants;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateHandlerUtils;
import com.elong.nb.util.ExecutorUtils;
import com.elong.springmvc_enhance.utilities.ActionLogHelper;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:23:56   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrInventoryRepository {

	private static final Logger logger = Logger.getLogger("IncrInventoryLogger");

	private static final int MAXDAYS = 90;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IProductForPartnerServiceContract productForPartnerServiceContract;

	@Resource
	private IProductForPartnerServiceContract productForPartnerServiceContractForList;

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private INoticeService noticeService;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public int deleteExpireIncrData(Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrInventory DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		Map<String, Object> params = new HashMap<String, Object>();
		Date endTime = expireDate;
		Date startTime = DateHandlerUtils.getOffsetDate(expireDate, Calendar.HOUR_OF_DAY, -1);
		params.put("startTime", startTime);
		params.put("endTime", endTime);
		int result = 0;
		int count = incrInventoryDao.deleteExpireIncrData(params);
		result += count;

		int i = 1;
		while (count != 0) {
			endTime = DateHandlerUtils.getOffsetDate(expireDate, Calendar.HOUR_OF_DAY, -(i++));
			startTime = DateHandlerUtils.getOffsetDate(expireDate, Calendar.HOUR_OF_DAY, -i);
			params.put("startTime", startTime);
			params.put("endTime", endTime);
			count = incrInventoryDao.deleteExpireIncrData(params);
			logger.info("IncrInventory delete successfully,successCount = " + count + ",endTime = " + endTime);
			result += count;
		}
		return result;
	}

	/** 
	 * WCF调用后端接口
	 *
	 * @param lastChangeTime
	 * @return
	 */
	public long getInventoryChangeMinID(Date lastChangeTime) {
		long startTimel = System.currentTimeMillis();
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Object guid = requestAttributes.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);

		GetInventoryChangeMinIDRequest request = new GetInventoryChangeMinIDRequest();
		request.setLastUpdateTime(new DateTime(lastChangeTime.getTime()));
		GetInventoryChangeMinIDResponse response = productForPartnerServiceContract.getInventoryChangeMinID(request);
		long result = 0;
		if (response.getResult().getResponseCode() == 0) {
			result = response.getMinID();
		} else if (response.getMinID() == Long.MAX_VALUE) {
			result = 0;
		} else {
			RuntimeException exception = new RuntimeException(response.getResult().getErrorMessage());
			ActionLogHelper.businessLog(guid == null ? null : (String) guid, true, "getInventoryChangeMinID", "IncrInventoryRepository",
					exception, System.currentTimeMillis() - startTimel, 1, null, lastChangeTime);
			throw exception;
		}
		ActionLogHelper.businessLog(guid == null ? null : (String) guid, false, "getInventoryChangeMinID", "IncrInventoryRepository", null,
				System.currentTimeMillis() - startTimel, 0, result + "", lastChangeTime);
		return result;
	}

	/** 
	 * 根据changeID同步库存增量
	 *
	 * @param changID
	 * @return
	 */
	public long syncInventoryToDB(long changID) {
		long startTime = System.currentTimeMillis();
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Object guid = requestAttributes.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);

		GetInventoryChangeListRequest request = new GetInventoryChangeListRequest();
		request.setId(changID);
		List<InventoryChangeModel> changeList = productForPartnerServiceContractForList.getInventoryChangeList(request)
				.getInventoryChangeList().getInventoryChangeModel();
		int changeListSize = changeList == null ? 0 : changeList.size();
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime)
				+ ",productForPartnerServiceContractForList.getInventoryChangeList,changeList size = " + changeListSize);
		ActionLogHelper.businessLog(guid == null ? null : (String) guid, false, "getInventoryChangeList",
				"IProductForPartnerServiceContract", null, endTime - startTime, 0, changeListSize + "", changID);

		List<InventoryChangeModel> filterChangeList = new ArrayList<InventoryChangeModel>();
		if (changeList != null && changeList.size() > 0) {
			startTime = System.currentTimeMillis();
			// 解决订阅库延时问题，获取明细时延时3分钟
			String inventoryChangeDelayMinutes = CommonsUtil.CONFIG_PROVIDAR.getProperty("InventoryChangeDelayMinutes");
			inventoryChangeDelayMinutes = StringUtils.isEmpty(inventoryChangeDelayMinutes) ? "10" : inventoryChangeDelayMinutes;
			DateTime lastTime = DateTime.now().minusMinutes(1 * Integer.valueOf(inventoryChangeDelayMinutes));
			for (InventoryChangeModel item : changeList) {
				if (item == null || item.getUpdateTime() == null)
					continue;
				if (item.getUpdateTime().compareTo(lastTime) >= 0)
					continue;
				filterChangeList.add(item);
			}
			changeList = filterChangeList;
			int filterChangeListSize = filterChangeList == null ? 0 : filterChangeList.size();
			logger.info("filterChangeList size = " + filterChangeListSize + ",after dohandler InventoryChangeDelayMinutes = "
					+ inventoryChangeDelayMinutes);
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",dohandler InventoryChangeDelay");
		}
		
		if (changeList != null && changeList.size() > 0) {
			// 填充全局变量FilteredSHotelIds
			startTime = System.currentTimeMillis();
			final Set<String> filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",commonRepository.fillFilteredSHotelsIds");

			// 最大支持300线程并行
			int maximumPoolSize = changeList.size() < 300 ? changeList.size() : 300;
			logger.info("maximumPoolSize = " + maximumPoolSize);
			startTime = System.currentTimeMillis();
			ExecutorService executorService = ExecutorUtils.newSelfThreadPool(maximumPoolSize, 400);
			final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			for (final InventoryChangeModel changeModel : changeList) {
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						doHandlerChangeModel(changeModel, rows, filteredSHotelIds);
					}
				});
			}
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",executorService submit task");
			executorService.shutdown();
			try {
				while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
					// logger.info("thread-pool has not been closed yet.");
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			logger.info("thread-pool has been closed.");

			// ChangeID排序，存数据库
			if (rows.size() > 0) {
				startTime = System.currentTimeMillis();
				Collections.sort(rows, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> o1, Map<String, Object> o2) {
						return (int) ((long) (o1.get("ChangeID")) - (long) (o2.get("ChangeID")));
					}
				});
				endTime = System.currentTimeMillis();
				logger.info("use time = " + (endTime - startTime) + ",sort rowMap by ChangeID");

				int recordCount = rows.size();
				if (recordCount > 0) {
					int successCount = 0;
					logger.info("IncrInventory BulkInsert start,recordCount = " + rows.size());
					String incrInventoryBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrInventoryBatchSize");
					int pageSize = StringUtils.isEmpty(incrInventoryBatchSize) ? 2000 : Integer.valueOf(incrInventoryBatchSize);
					int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
					startTime = System.currentTimeMillis();
					for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
						int startNum = (pageIndex - 1) * pageSize;
						int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
						successCount += incrInventoryDao.bulkInsert(rows.subList(startNum, endNum));
					}
					endTime = System.currentTimeMillis();
					logger.info("use time = " + (endTime - startTime) + ",IncrInventory BulkInsert,successCount = " + successCount);
				}

			}
			// ID排序，去最大ID
			if (changeList != null && changeList.size() > 0) {
				startTime = System.currentTimeMillis();
				Collections.sort(changeList, new Comparator<InventoryChangeModel>() {
					@Override
					public int compare(InventoryChangeModel o1, InventoryChangeModel o2) {
						return o1.getID().compareTo(o2.getID());
					}
				});
				changID = changeList.get(changeList.size() - 1).getID();
				endTime = System.currentTimeMillis();
				logger.info("use time = " + (endTime - startTime) + ",sort rowMap by ID");
			}
		}
		return changID;
	}

	/** 
	 * 处理InventoryChangeModel
	 *
	 * @param changeModel
	 * @param rows
	 */
	private void doHandlerChangeModel(InventoryChangeModel changeModel, List<Map<String, Object>> rows, Set<String> filteredSHotelIds) {
		long startTimel = System.currentTimeMillis();
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Object guid = requestAttributes.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);

		String threadName = Thread.currentThread().getName();
		GetInventoryChangeDetailRequest request = null;
		try {
			boolean isFileterd = filteredSHotelIds.contains(changeModel.getHotelID());
			if (isFileterd) {
				// logger.info(threadName + ":filteredSHotelIds contain hotelID[" + changeModel.getHotelID() + "],ignore it.");
				return;
			}
			// #region 仅提供昨天和最近90天的房态数据 判断开始结束时间段是否在昨天和MaxDays之内
			int startDays = Days.daysBetween(DateTime.now(), changeModel.getBeginTime()).getDays();
			int endDays = Days.daysBetween(DateTime.now(), changeModel.getEndTime()).getDays();
			if (startDays > MAXDAYS || endDays < 0) {
				return;
			}
			if (startDays < -1) {
				changeModel.setBeginTime(DateTime.now());
				startDays = 0;
			}
			if (endDays > MAXDAYS) {
				changeModel.setEndTime(DateTime.now().plusDays(MAXDAYS));
				endDays = MAXDAYS;
			}
			if (changeModel.getBeginTime().compareTo(changeModel.getEndTime()) > 0) {
				return;
			}
			// #endregion

			request = new GetInventoryChangeDetailRequest();
			request.setHotelID(changeModel.getHotelID());
			request.setBeginTime(changeModel.getBeginTime());
			request.setEndTime(changeModel.getEndTime());
			request.setRoomTypeIDs(changeModel.getRoomTypeID());
			long startTime = System.currentTimeMillis();
			GetInventoryChangeDetailResponse response = null;
			try {
				response = productForPartnerServiceContract.getInventoryChangeDetail(request);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);// 异常吃掉，下面会重试
			}
			long endTime = System.currentTimeMillis();
			logger.info("use time [" + threadName + "] = " + (endTime - startTime)
					+ ",productForPartnerServiceContract.getInventoryChangeDetail");
			ActionLogHelper.businessLog(guid == null ? null : (String) guid, false, "getInventoryChangeDetail",
					"IProductForPartnerServiceContract", null, endTime - startTime, 0, JSON.toJSONString(response), request);

			List<ResourceInventoryState> resourceInventoryStateList = null;
			if (response != null && response.getResourceInventoryStateList() != null) {
				resourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			}
			// retry
			if (resourceInventoryStateList == null || resourceInventoryStateList.size() == 0) {
				// logger.info(threadName + ":ResourceInventoryStateList is null or empty,and will retry.");
				if (changeModel.getBeginTime().compareTo(DateTime.now().plusDays(88)) < 0) {
					startTime = System.currentTimeMillis();
					Thread.sleep(2000);
					response = productForPartnerServiceContract.getInventoryChangeDetail(request);
					endTime = System.currentTimeMillis();
					logger.info("use time [" + threadName + "] = " + (endTime - startTime)
							+ ",retry productForPartnerServiceContract.getInventoryChangeDetail");

					ActionLogHelper.businessLog(guid == null ? null : (String) guid, false, "retryGetInventoryChangeDetail",
							"IProductForPartnerServiceContract", null, endTime - startTime, 0, JSON.toJSONString(response), request);
				}
			}
			startTime = System.currentTimeMillis();
			String mHotelId = this.msRelationRepository.getMHotelId(changeModel.getHotelID());
			endTime = System.currentTimeMillis();
			ActionLogHelper.businessLog(guid == null ? null : (String) guid, false, "getMHotelId", "MSRelationRepository", null, endTime
					- startTime, 0, mHotelId, changeModel.getHotelID());

			// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",msRelationRepository.getMHotelId");
			if (response != null && response.getResourceInventoryStateList() != null) {
				resourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			}
			if (resourceInventoryStateList != null && resourceInventoryStateList.size() > 0) {
				// startTime = System.currentTimeMillis();
				for (ResourceInventoryState detail : resourceInventoryStateList) {
					synchronized (this.getClass()) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("HotelID", mHotelId);
						// TODO 临时处理，需修改表字段长度。后续改回来
						row.put("RoomTypeID",
								detail.getRoomTypeID().length() > 50 ? detail.getRoomTypeID().substring(0, 50) : detail.getRoomTypeID());
						row.put("HotelCode", detail.getHotelID());
						row.put("Status", detail.getStatus() == 0);
						row.put("AvailableDate", detail.getAvailableTime() == null ? null : detail.getAvailableTime().toDate());
						row.put("AvailableAmount", detail.getAvailableAmount());
						row.put("OverBooking", detail.getIsOverBooking());
						row.put("StartDate", detail.getBeginDate() == null ? null : detail.getBeginDate().toDate());
						row.put("EndDate", detail.getEndDate() == null ? null : detail.getEndDate().toDate());
						row.put("StartTime", detail.getBeginTime());
						row.put("EndTime", detail.getEndTime());
						row.put("OperateTime", detail.getOperateTime() == null ? null : detail.getOperateTime().toDate());
						row.put("InsertTime", DateTime.now().toDate());
						row.put("ChangeID", changeModel.getID());
						row.put("ChangeTime", changeModel.getUpdateTime() == null ? null : changeModel.getUpdateTime().toDate());
						rows.add(row);
					}
				}
				// endTime = System.currentTimeMillis();
				// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",build rowMap one.");
			} else {
				DateTime date = changeModel.getBeginTime();
				// startTime = System.currentTimeMillis();
				while (date.compareTo(changeModel.getEndTime()) < 0) {
					synchronized (this.getClass()) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("HotelID", mHotelId);
						// TODO 临时处理，需修改表字段长度。后续改回来
						row.put("RoomTypeID", changeModel.getRoomTypeID().length() > 50 ? changeModel.getRoomTypeID().substring(0, 50)
								: changeModel.getRoomTypeID());
						row.put("HotelCode", changeModel.getHotelID());
						row.put("Status", false);
						row.put("AvailableDate", date == null ? null : date.toDate());
						row.put("AvailableAmount", 0);
						row.put("OverBooking", 1);
						row.put("StartDate", date == null ? null : date.toDate());
						row.put("EndDate", date == null ? null : date.toDate());
						row.put("StartTime", "00:00:00");
						row.put("EndTime", "23:59:59");
						row.put("OperateTime", changeModel.getUpdateTime() == null ? null : changeModel.getUpdateTime().toDate());
						row.put("InsertTime", DateTime.now().toDate());
						row.put("ChangeID", changeModel.getID());
						row.put("ChangeTime", changeModel.getUpdateTime() == null ? null : changeModel.getUpdateTime().toDate());
						rows.add(row);
					}
					date = date.plusDays(1);
				}
				// endTime = System.currentTimeMillis();
				// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",build rowMap two.");
			}
		} catch (Exception ex) {
			logger.error(threadName + ":SyncInventoryToDB,doHandlerChangeModel,error = " + ex.getMessage(), ex);
			ActionLogHelper.businessLog(guid == null ? null : (String) guid, true, "retryGetInventoryChangeDetail",
					"IProductForPartnerServiceContract", ex, System.currentTimeMillis() - startTimel, 1, null, request);
			noticeService.sendMessage(threadName + ":SyncInventoryToDB,doHandlerChangeModel,error",
					"request = " + JSON.toJSONString(request) + ".\n" + ExceptionUtils.getFullStackTrace(ex));
		}
	}

}
