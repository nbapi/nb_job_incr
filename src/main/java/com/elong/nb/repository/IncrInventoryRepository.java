/**   
 * @(#)IncrInventoryRepository.java	2016年9月21日	下午4:23:56	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.text.MessageFormat;
import java.util.ArrayList;
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

	private static final Logger logger = Logger.getLogger("syncIncrInventoryLogger");

	private static final int MAXDAYS = 90;

	private boolean IsToRetryInventoryDetailRequest;

	private Set<String> filteredSHotelIds = new HashSet<String>();

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IProductForPartnerServiceContract ProductForPartnerServiceContract;

	@Resource
	private IProductForPartnerServiceContract ProductForPartnerServiceContractForList;

	@Resource
	private M_SRelationRepository M_SRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public int DeleteExpireIncrData(String table, Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrInventory DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		int limit = 10000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expireDate", expireDate);
		params.put("limit", limit);
		int result = 0;
		int count = 0;
		count = incrInventoryDao.DeleteExpireIncrData(params);
		result += count;
		while (count == limit) {
			count = incrInventoryDao.DeleteExpireIncrData(params);
		}
		logger.info("IncrInventory delete successfully,expireDate = " + expireDate);
		return result;
	}

	/** 
	 * WCF调用后端接口
	 *
	 * @param lastChangeTime
	 * @return
	 */
	public long GetInventoryChangeMinID(Date lastChangeTime) {
		GetInventoryChangeMinIDRequest request = new GetInventoryChangeMinIDRequest();
		request.setLastUpdateTime(new DateTime(lastChangeTime.getTime()));
		GetInventoryChangeMinIDResponse response = ProductForPartnerServiceContract.getInventoryChangeMinID(request);
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
	public long SyncInventoryToDB(long changID) {
		GetInventoryChangeListRequest request = new GetInventoryChangeListRequest();
		request.setId(changID);
		List<InventoryChangeModel> changeList = ProductForPartnerServiceContractForList.getInventoryChangeList(request)
				.getInventoryChangeList().getInventoryChangeModel();
		int changeListSize = changeList == null ? 0 : changeList.size();
		logger.info("changeList size = " + changeListSize + ",from wcf [ProductForPartnerServiceContractForList.getInventoryChangeList]");

		List<InventoryChangeModel> filterChangeList = new ArrayList<InventoryChangeModel>();
		if (changeList != null && changeList.size() > 0) {
			// 解决订阅库延时问题，获取明细时延时3分钟
			String InventoryChangeDelayMinutes = PropertiesHelper.getEnvProperties("InventoryChangeDelayMinutes", "config").toString();
			InventoryChangeDelayMinutes = StringUtils.isEmpty(InventoryChangeDelayMinutes) ? "10" : InventoryChangeDelayMinutes;
			DateTime lastTime = DateTime.now().minusMinutes(1 * Integer.valueOf(InventoryChangeDelayMinutes));
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
					+ InventoryChangeDelayMinutes);
		}

		if (changeList != null && changeList.size() > 0) {
			// 填充全局变量FilteredSHotelIds
			filteredSHotelIds = commonRepository.FillFilteredSHotelsIds();

			// 最大支持300线程并行
			int maximumPoolSize = changeList.size() < 300 ? changeList.size() : 300;
			logger.info("maximumPoolSize = " + maximumPoolSize);
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
			executorService.shutdown();
			try {
				while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
					logger.info("SyncInventoryToDB,线程池没有关闭");
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			logger.info("SyncInventoryToDB,线程池已经关闭");

			// ChangeID排序，存数据库
			if (rows.size() > 0) {
				Collections.sort(rows, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> o1, Map<String, Object> o2) {
						return (int) ((long) (o1.get("ChangeID")) - (long) (o2.get("ChangeID")));
					}
				});

				int pageSize = 1000;
				int recordCount = rows.size();
				logger.info("IncrInventory BulkInsert start,recordCount = " + recordCount);
				int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
				for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
					int startNum = (pageIndex - 1) * pageSize;
					int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
					int count = incrInventoryDao.BulkInsert(rows.subList(startNum, endNum));
					logger.info("IncrInventory BulkInsert successfully,count = " + count);
				}
			}
			// ID排序，去最大ID
			if (changeList != null && changeList.size() > 0) {
				Collections.sort(changeList, new Comparator<InventoryChangeModel>() {
					@Override
					public int compare(InventoryChangeModel o1, InventoryChangeModel o2) {
						return o1.getID().compareTo(o2.getID());
					}
				});
				changID = changeList.get(changeList.size() - 1).getID();
			}
		}
		return changID;
	}

	private void doHandlerChangeModel(InventoryChangeModel changeModel, List<Map<String, Object>> rows) {
		String threadName = Thread.currentThread().getName();
		try {
			if (this.filteredSHotelIds.contains(changeModel.getHotelID())) {
				logger.info(threadName + ":SyncInventoryToDB,CQ FilteredSHotelID:" + changeModel.getHotelID());
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

			GetInventoryChangeDetailRequest request = new GetInventoryChangeDetailRequest();
			request.setHotelID(changeModel.getHotelID());
			request.setBeginTime(changeModel.getBeginTime());
			request.setEndTime(changeModel.getEndTime());
			request.setRoomTypeIDs(changeModel.getRoomTypeID());
			GetInventoryChangeDetailResponse response = ProductForPartnerServiceContract.getInventoryChangeDetail(request);

			List<ResourceInventoryState> ResourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			// retry
			if (ResourceInventoryStateList == null || ResourceInventoryStateList.size() == 0) {
				logger.info(threadName + ":incr.inv.update,0,data-lack,response inv is empty," + JSON.toJSONString(request) + ","
						+ JSON.toJSONString(response));
				if (IsToRetryInventoryDetailRequest && changeModel.getBeginTime().compareTo(DateTime.now().plusDays(88)) < 0) {
					Thread.sleep(2000);
					response = ProductForPartnerServiceContract.getInventoryChangeDetail(request);
				}
			}
			String MHotelId = this.M_SRelationRepository.GetMHotelId(changeModel.getHotelID());
			ResourceInventoryStateList = response.getResourceInventoryStateList().getResourceInventoryState();
			if (ResourceInventoryStateList != null && ResourceInventoryStateList.size() > 0) {
				for (ResourceInventoryState detail : ResourceInventoryStateList) {
					synchronized (this.getClass()) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("HotelID", MHotelId);
						row.put("RoomTypeID", detail.getRoomTypeID());
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
			} else {
				DateTime date = changeModel.getBeginTime();
				while (date.compareTo(changeModel.getEndTime()) < 0) {
					synchronized (this.getClass()) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("HotelID", MHotelId);
						row.put("RoomTypeID", changeModel.getRoomTypeID());
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
				logger.error(threadName + ":"
						+ MessageFormat.format("incr inv detail empty: {0} \t{1}", changeModel.getID(), JSON.toJSONString(changeModel)));
			}
		} catch (Exception ex) {
			logger.error(threadName + ":incr.inv.IncrInventory,incr-fail", ex);
			throw new RuntimeException(ex);
		}
	}

}
