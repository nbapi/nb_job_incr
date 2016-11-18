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
import java.util.HashSet;
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

import com.alibaba.fastjson.JSON;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeDetailResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeListRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeMinIDRequest;
import com.elong.nb.agent.ProductForPartnerServiceContract.GetInventoryChangeMinIDResponse;
import com.elong.nb.agent.ProductForPartnerServiceContract.IProductForPartnerServiceContract;
import com.elong.nb.agent.ProductForPartnerServiceContract.InventoryChangeModel;
import com.elong.nb.agent.ProductForPartnerServiceContract.ResourceInventoryState;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateHandlerUtils;
import com.elong.nb.util.ExecutorUtils;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

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

	private boolean isToRetryInventoryDetailRequest;

	private Set<String> filteredSHotelIds = new HashSet<String>();

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
		GetInventoryChangeMinIDRequest request = new GetInventoryChangeMinIDRequest();
		request.setLastUpdateTime(new DateTime(lastChangeTime.getTime()));
		GetInventoryChangeMinIDResponse response = productForPartnerServiceContract.getInventoryChangeMinID(request);
		if (response.getResult().getResponseCode() == 0) {
			return response.getMinID();
		} else if (response.getMinID() == Long.MAX_VALUE) {
			return 0;
		} else {
			throw new RuntimeException(response.getResult().getErrorMessage());
		}
	}

	/** 
	 * 根据changeID同步库存增量
	 *
	 * @param changID
	 * @return
	 */
	public long syncInventoryToDB(long changID) {
		GetInventoryChangeListRequest request = new GetInventoryChangeListRequest();
		request.setId(changID);
		long startTime = new Date().getTime();
		List<InventoryChangeModel> changeList = productForPartnerServiceContractForList.getInventoryChangeList(request)
				.getInventoryChangeList().getInventoryChangeModel();
		int changeListSize = changeList == null ? 0 : changeList.size();
		logger.info("changeList size = " + changeListSize + ",from wcf [ProductForPartnerServiceContractForList.getInventoryChangeList]");
		long endTime = new Date().getTime();
		logger.info("use time = " + (endTime - startTime) + ",productForPartnerServiceContractForList.getInventoryChangeList");

		List<InventoryChangeModel> filterChangeList = new ArrayList<InventoryChangeModel>();
		if (changeList != null && changeList.size() > 0) {
			startTime = new Date().getTime();
			// 解决订阅库延时问题，获取明细时延时3分钟
			String inventoryChangeDelayMinutes = PropertiesHelper.getEnvProperties("InventoryChangeDelayMinutes", "config").toString();
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
			endTime = new Date().getTime();
			logger.info("use time = " + (endTime - startTime) + ",dohandler InventoryChangeDelay");
		}

		if (changeList != null && changeList.size() > 0) {
			// 填充全局变量FilteredSHotelIds
			startTime = new Date().getTime();
			filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
			endTime = new Date().getTime();
			logger.info("use time = " + (endTime - startTime) + ",commonRepository.fillFilteredSHotelsIds");

			// 最大支持300线程并行
			int maximumPoolSize = changeList.size() < 300 ? changeList.size() : 300;
			logger.info("maximumPoolSize = " + maximumPoolSize);
			startTime = new Date().getTime();
			ExecutorService executorService = ExecutorUtils.newSelfThreadPool(maximumPoolSize, 400);
			final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			for (final InventoryChangeModel changeModel : changeList) {
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						doHandlerChangeModel(changeModel, rows);
					}
				});
			}
			endTime = new Date().getTime();
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
				startTime = new Date().getTime();
				Collections.sort(rows, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> o1, Map<String, Object> o2) {
						return (int) ((long) (o1.get("ChangeID")) - (long) (o2.get("ChangeID")));
					}
				});
				endTime = new Date().getTime();
				logger.info("use time = " + (endTime - startTime) + ",sort rowMap by ChangeID");

				int recordCount = rows.size();
				if (recordCount > 0) {
					int successCount = 0;
					logger.info("IncrInventory BulkInsert start,recordCount = " + rows.size());
					String incrInventoryBatchSize = PropertiesHelper.getEnvProperties("IncrInventoryBatchSize", "config").toString();
					int pageSize = StringUtils.isEmpty(incrInventoryBatchSize) ? 2000 : Integer.valueOf(incrInventoryBatchSize);
					int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
					startTime = new Date().getTime();
					for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
						int startNum = (pageIndex - 1) * pageSize;
						int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
						successCount += incrInventoryDao.bulkInsert(rows.subList(startNum, endNum));
					}
					endTime = new Date().getTime();
					logger.info("use time = " + (endTime - startTime) + ",IncrInventory BulkInsert,successCount = " + successCount);
				}

			}
			// ID排序，去最大ID
			if (changeList != null && changeList.size() > 0) {
				startTime = new Date().getTime();
				Collections.sort(changeList, new Comparator<InventoryChangeModel>() {
					@Override
					public int compare(InventoryChangeModel o1, InventoryChangeModel o2) {
						return o1.getID().compareTo(o2.getID());
					}
				});
				changID = changeList.get(changeList.size() - 1).getID();
				endTime = new Date().getTime();
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
	private void doHandlerChangeModel(InventoryChangeModel changeModel, List<Map<String, Object>> rows) {
		String threadName = Thread.currentThread().getName();
		GetInventoryChangeDetailRequest request = null;
		try {
			boolean isFileterd = this.filteredSHotelIds.contains(changeModel.getHotelID());
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
			long startTime = new Date().getTime();
			GetInventoryChangeDetailResponse response = null;
			try {
				response = productForPartnerServiceContract.getInventoryChangeDetail(request);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);// 异常吃掉，下面会重试
			}
			long endTime = new Date().getTime();
			logger.info("use time [" + threadName + "] = " + (endTime - startTime)
					+ ",productForPartnerServiceContract.getInventoryChangeDetail");

			List<ResourceInventoryState> resourceInventoryStateList = null;
			if (response != null && response.getResourceInventoryStateList() != null) {
				resourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			}
			// retry
			if (resourceInventoryStateList == null || resourceInventoryStateList.size() == 0) {
				// logger.info(threadName + ":ResourceInventoryStateList is null or empty,and will retry.");
				if (isToRetryInventoryDetailRequest && changeModel.getBeginTime().compareTo(DateTime.now().plusDays(88)) < 0) {
					startTime = new Date().getTime();
					Thread.sleep(2000);
					response = productForPartnerServiceContract.getInventoryChangeDetail(request);
					endTime = new Date().getTime();
					logger.info("use time [" + threadName + "] = " + (endTime - startTime)
							+ ",retry productForPartnerServiceContract.getInventoryChangeDetail");
				}
			}
			// startTime = new Date().getTime();
			String mHotelId = this.msRelationRepository.getMHotelId(changeModel.getHotelID());
			// endTime = new Date().getTime();
			// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",msRelationRepository.getMHotelId");
			if (response != null && response.getResourceInventoryStateList() != null) {
				resourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			}
			if (resourceInventoryStateList != null && resourceInventoryStateList.size() > 0) {
				// startTime = new Date().getTime();
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
				// endTime = new Date().getTime();
				// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",build rowMap one.");
			} else {
				DateTime date = changeModel.getBeginTime();
				// startTime = new Date().getTime();
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
				// endTime = new Date().getTime();
				// logger.info("use time [" + threadName + "] = " + (endTime - startTime) + ",build rowMap two.");
			}
		} catch (Exception ex) {
			logger.error(threadName + ":SyncInventoryToDB,doHandlerChangeModel,error = " + ex.getMessage(), ex);
			noticeService.sendMessage(threadName + ":SyncInventoryToDB,doHandlerChangeModel,error",
					"request = " + JSON.toJSONString(request) + ".\n" + ExceptionUtils.getFullStackTrace(ex));
		}
	}

}
