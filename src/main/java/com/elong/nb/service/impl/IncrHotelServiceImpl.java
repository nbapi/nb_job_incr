/**   
 * @(#)IncrHotelServiceImpl.java	2016年9月21日	下午2:38:25	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
import com.elong.nb.repository.IncrHotelRepository;
import com.elong.nb.service.IIncrHotelService;
import com.elong.nb.service.IIncrSetInfoService;

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
public class IncrHotelServiceImpl implements IIncrHotelService {

	private static final Logger logger = Logger.getLogger("IncrHotelLogger");

	private static final int MaxRecordCount = 1000;

	@Resource
	private IncrHotelRepository incrHotelRepository;
	
	@Resource
	private IIncrSetInfoService incrSetInfoService;

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
						long incrInventoryTriggerID = 0;
						if (hotel == null) {
							incrInventoryTriggerID = Long.valueOf(incrSetInfoService.get("IncrInventory.last.TriggerID"));
						} else {
							incrInventoryTriggerID = hotel.TriggerID;
						}
						inventorys = incrHotelRepository.getIncrInventories(incrInventoryTriggerID, MaxRecordCount);
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
							incrHotel.setTriggerID(Long.valueOf(item.getID() + ""));
							incrHotel.setInsertTime(new Date());
							hotels.add(incrHotel);
						}
						incrHotelRepository.syncIncrHotelToDB(hotels);
						Thread.sleep(2000);
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
						long incrRateTriggerID = 0;
						if (hotel == null) {
							incrRateTriggerID = Long.valueOf(incrSetInfoService.get("IncrRate.last.TriggerID"));
						} else {
							incrRateTriggerID = hotel.TriggerID;
						}
						rates = incrHotelRepository.getIncrRates(incrRateTriggerID, MaxRecordCount);
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
						incrHotelRepository.syncIncrHotelToDB(hotels);
						Thread.sleep(2000);
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

}
