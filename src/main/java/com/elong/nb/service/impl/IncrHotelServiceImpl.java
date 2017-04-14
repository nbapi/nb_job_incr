/**   
 * @(#)IncrHotelServiceImpl.java	2016年9月21日	下午2:38:25	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.dao.IncrHotelDao;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
import com.elong.nb.repository.IncrHotelRepository;
import com.elong.nb.service.AbstractDeleteService;
import com.elong.nb.service.IIncrHotelService;
import com.elong.nb.util.DateHandlerUtils;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * IncrHotel服务接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:38:25   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class IncrHotelServiceImpl extends AbstractDeleteService implements IIncrHotelService {

	private static final Logger logger = Logger.getLogger("IncrHotelLogger");

	private static final int MaxRecordCount = 1000;

	@Resource
	private IncrHotelRepository incrHotelRepository;

	@Resource
	private IncrHotelDao incrHotelDao;

	/** 
	 * 删除酒店增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrHotelService#delHotelFromDB()    
	 */
	@Override
	public void delHotelFromDB() {
		// 删除30小时以前的数据
		deleteExpireIncrData(DateHandlerUtils.getDBExpireDate());
	}

	/** 
	 * 同步酒店增量
	 * 
	 *
	 * @see com.elong.nb.service.IIncrHotelService#SyncHotelToDB()    
	 */
	@Override
	public void syncHotelToDB() {
		final String triggerInventory = "Inventory";
		final String triggerRate = "Rate";

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						long startTime = System.currentTimeMillis();
						IncrHotel hotel = incrHotelRepository.getLastHotel(triggerInventory);
						long endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerInventory + ",lastHotel = "
								+ JSON.toJSONString(hotel));
						List<IncrInventory> inventorys = null;
						startTime = System.currentTimeMillis();
						if (hotel == null) {
							inventorys = incrHotelRepository.getIncrInventories(DateHandlerUtils.getCacheExpireDate(), MaxRecordCount);
						} else {
							inventorys = incrHotelRepository.getIncrInventories(hotel.TriggerID, MaxRecordCount);
						}
						endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerInventory + ",inventorys size = "
								+ inventorys.size());
						if (inventorys == null || inventorys.size() == 0)
							break;
						List<IncrHotel> hotels = new ArrayList<IncrHotel>();
						for (IncrInventory item : inventorys) {
							IncrHotel incrHotel = new IncrHotel();
							incrHotel.setChangeTime(item.getChangeTime());
							incrHotel.setHotelID(item.getHotelID());
							incrHotel.setStartDate(item.getAvailableDate());
							incrHotel.setEndDate(item.getAvailableDate());
							incrHotel.setTrigger(triggerInventory);
							incrHotel.setTriggerID(Long.valueOf(item.getIncrID() + ""));
							incrHotel.setInsertTime(new Date());
							hotels.add(incrHotel);
						}
						startTime = System.currentTimeMillis();
						hotels = filterDuplicationHotel(hotels);
						endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",filterDuplicationHotel");
						incrHotelRepository.syncIncrHotelToDB(hotels);
					}
				} catch (Exception e) {
					logger.error("SyncHotelToDB,thread dohandler 'IncrInventory' error" + e.getMessage(), e);
				}
			}
		});

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						long startTime = System.currentTimeMillis();
						IncrHotel hotel = incrHotelRepository.getLastHotel(triggerRate);
						long endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerRate + ",lastHotel = "
								+ JSON.toJSONString(hotel));
						List<IncrRate> rates = null;
						startTime = System.currentTimeMillis();
						if (hotel == null) {
							rates = incrHotelRepository.getIncrRates(DateHandlerUtils.getCacheExpireDate(), MaxRecordCount);
						} else {
							rates = incrHotelRepository.getIncrRates(hotel.TriggerID, MaxRecordCount);
						}
						endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerRate + ",rates size = " + rates.size());
						if (rates == null || rates.size() == 0)
							break;
						List<IncrHotel> hotels = new ArrayList<IncrHotel>();
						for (IncrRate item : rates) {
							IncrHotel incrHotel = new IncrHotel();
							incrHotel.setChangeTime(item.getChangeTime());
							incrHotel.setHotelID(item.getHotelID());
							incrHotel.setStartDate(item.getStartDate());
							incrHotel.setEndDate(item.getEndDate());
							incrHotel.setTrigger(triggerRate);
							incrHotel.setTriggerID(Long.valueOf(item.getIncrID() + ""));
							incrHotel.setInsertTime(new Date());
							hotels.add(incrHotel);
						}
						startTime = System.currentTimeMillis();
						hotels = filterDuplicationHotel(hotels);
						endTime = System.currentTimeMillis();
						logger.info("use time = " + (endTime - startTime) + ",filterDuplicationHotel");
						incrHotelRepository.syncIncrHotelToDB(hotels);
					}
				} catch (Exception e) {
					logger.error("SyncHotelToDB,thread dohandler 'IncrRate' error" + e.getMessage(), e);
				}
			}
		});

		executorService.shutdown();
		try {
			while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
				// logger.info("thread-pool has not been closed yet.");
			}
		} catch (InterruptedException e) {
			logger.error("SyncHotelToDB,awaitTermination error = " + e.getMessage(), e);
		}
		logger.info("thread-pool has been closed.");
	}

	/** 
	 * 过滤重复Hotel增量
	 *
	 * @param hotels
	 * @return
	 */
	private List<IncrHotel> filterDuplicationHotel(List<IncrHotel> hotels) {
		if (hotels == null || hotels.size() == 0)
			return hotels;
		logger.info("before filterDuplicationHotel,count = " + hotels.size());
		// group by hotelID,startDate,EndDate
		Map<String, List<IncrHotel>> groupMap = new HashMap<String, List<IncrHotel>>();
		for (IncrHotel hotel : hotels) {
			if (hotel == null)
				continue;
			IncrHotel tempHotel = new IncrHotel();
			tempHotel.setHotelID(hotel.getHotelID());
			tempHotel.setStartDate(hotel.getStartDate());
			tempHotel.setEndDate(hotel.getEndDate());
			String groupKey = JSON.toJSONString(tempHotel);

			List<IncrHotel> groupValList = groupMap.get(groupKey);
			if (groupValList == null) {
				groupValList = new ArrayList<IncrHotel>();
			}
			tempHotel.setChangeTime(hotel.getChangeTime());
			tempHotel.setTriggerID(hotel.getTriggerID());
			tempHotel.setTrigger(hotel.getTrigger());
			tempHotel.setInsertTime(hotel.getInsertTime());
			groupValList.add(tempHotel);
			groupMap.put(groupKey, groupValList);
		}

		List<IncrHotel> resultlist = new ArrayList<IncrHotel>();
		for (Map.Entry<String, List<IncrHotel>> entry : groupMap.entrySet()) {
			List<IncrHotel> groupValList = entry.getValue();
			// max(changeTime)
			Collections.sort(groupValList, new Comparator<IncrHotel>() {
				@Override
				public int compare(IncrHotel o1, IncrHotel o2) {
					if (o1 == null && o2 == null)
						return 0;
					if (o1 == null)
						return -1;
					if (o2 == null)
						return 1;
					if (o1.getChangeTime() == null && o2.getChangeTime() == null)
						return 0;
					if (o1.getChangeTime() == null)
						return -1;
					if (o2.getChangeTime() == null)
						return 1;
					return o1.getChangeTime().compareTo(o2.getChangeTime());
				}
			});
			IncrHotel incrHotel = groupValList.get(groupValList.size() - 1);
			// max(trigger)
			Collections.sort(groupValList, new Comparator<IncrHotel>() {
				@Override
				public int compare(IncrHotel o1, IncrHotel o2) {
					if (o1 == null && o2 == null)
						return 0;
					if (o1 == null)
						return -1;
					if (o2 == null)
						return 1;
					if (o1.getTrigger() == null && o2.getTrigger() == null)
						return 0;
					if (o1.getTrigger() == null)
						return -1;
					if (o2.getTrigger() == null)
						return 1;
					return o1.getTrigger().compareTo(o2.getTrigger());
				}
			});
			IncrHotel tempHotel = groupValList.get(groupValList.size() - 1);
			incrHotel.setTrigger(tempHotel.getTrigger());
			// max(triggerID)
			Collections.sort(groupValList, new Comparator<IncrHotel>() {
				@Override
				public int compare(IncrHotel o1, IncrHotel o2) {
					if (o1 == null && o2 == null)
						return 0;
					if (o1 == null)
						return -1;
					if (o2 == null)
						return 1;
					long diffVal = o1.getTriggerID() - o2.getTriggerID();
					return diffVal == 0 ? 0 : (diffVal > 0 ? 1 : -1);
				}
			});
			tempHotel = groupValList.get(groupValList.size() - 1);
			incrHotel.setTriggerID(tempHotel.getTriggerID());
			// max(insertTime)
			Collections.sort(groupValList, new Comparator<IncrHotel>() {
				@Override
				public int compare(IncrHotel o1, IncrHotel o2) {
					if (o1 == null && o2 == null)
						return 0;
					if (o1 == null)
						return -1;
					if (o2 == null)
						return 1;
					if (o1.getInsertTime() == null && o2.getInsertTime() == null)
						return 0;
					if (o1.getInsertTime() == null)
						return -1;
					if (o2.getInsertTime() == null)
						return 1;
					return o1.getInsertTime().compareTo(o2.getInsertTime());
				}
			});
			tempHotel = groupValList.get(groupValList.size() - 1);
			incrHotel.setInsertTime(tempHotel.getInsertTime());
			resultlist.add(incrHotel);
		}
		logger.info("after filterDuplicationHotel,count = " + resultlist.size());
		return resultlist;
	}

	@Override
	protected List<BigInteger> getIncrIdList(Map<String, Object> params) {
		return incrHotelDao.getIncrIdList(params);
	}

	@Override
	protected int deleteByIncrIdList(List<BigInteger> incrIdList) {
		return incrHotelDao.deleteByIncrIdList(incrIdList);
	}

	@Override
	protected void logger(String message) {
		logger.info(message);
	}

}
