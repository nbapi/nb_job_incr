/**   
 * @(#)IncrHotelServiceImpl.java	2016年9月21日	下午2:38:25	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.elong.nb.util.ConfigUtils;

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

	@Override
	public void syncHotelToDBFromRate() {
		long beginTime = System.currentTimeMillis();
		final String triggerRate = "Rate";
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
				// 价格增量截止id是否走配置id，0表示走配置，1表示正常流程
				int incrRateSetEnable = ConfigUtils.getIntConfigValue("IncrRate.last.TriggerID.SetEnable", 1);
				if (hotel == null || incrRateSetEnable == 0) {
					incrRateTriggerID = Long.valueOf(incrSetInfoService.get("IncrRate.last.TriggerID"));
				} else {
					incrRateTriggerID = hotel.TriggerID;
				}
				rates = incrHotelRepository.getIncrRates(incrRateTriggerID, MaxRecordCount);
				endTime = System.currentTimeMillis();
				logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerRate + ",rates size = " + rates.size());
				if (rates == null || rates.size() == 0 || (endTime - beginTime) > 10 * 60 * 1000)
					break;
				List<IncrHotel> hotels = new ArrayList<IncrHotel>();
				for (IncrRate item : rates) {
					IncrHotel incrHotel = new IncrHotel();
					incrHotel.setChangeTime(item.getChangeTime());
					incrHotel.setHotelID(item.getHotelID());
					incrHotel.setStartDate(item.getStartDate());
					incrHotel.setEndDate(item.getEndDate());
					incrHotel.setTrigger(triggerRate);
					incrHotel.setTriggerID(Long.valueOf(item.getID() + ""));
					incrHotel.setChannel(item.getChannel());
					incrHotel.setInsertTime(new Date());
					incrHotel.setSellChannel(item.getSellChannel());
					incrHotel.setIsStraint(item.getIsStraint());
					hotels.add(incrHotel);
				}
				incrHotelRepository.syncIncrHotelToDB(hotels);
				Thread.sleep(200);
			}
		} catch (Exception e) {
			throw new IllegalStateException("SyncHotelToDB,thread dohandler 'IncrRate' error" + e.getMessage(), e);
		}
	}

	@Override
	public void syncHotelToDBFromInventory() {
		long beginTime = System.currentTimeMillis();
		final String triggerInventory = "Inventory";
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
				// 库存增量截止id是否走配置id，0表示走配置，1表示正常流程
				int incrInventorySetEnable = ConfigUtils.getIntConfigValue("IncrInventory.last.TriggerID.SetEnable", 1);
				if (hotel == null || incrInventorySetEnable == 0) {
					incrInventoryTriggerID = Long.valueOf(incrSetInfoService.get("IncrInventory.last.TriggerID"));
				} else {
					incrInventoryTriggerID = hotel.TriggerID;
				}
				inventorys = incrHotelRepository.getIncrInventories(incrInventoryTriggerID, MaxRecordCount);
				endTime = System.currentTimeMillis();
				logger.info("use time = " + (endTime - startTime) + ",Trigger = " + triggerInventory + ",inventorys size = "
						+ inventorys.size());
				if (inventorys == null || inventorys.size() == 0 || (endTime - beginTime) > 10 * 60 * 1000)
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
					incrHotel.setChannel(item.getChannel());
					incrHotel.setInsertTime(new Date());
					incrHotel.setSellChannel(item.getSellChannel());
					incrHotel.setIsStraint(item.getIsStraint());
					hotels.add(incrHotel);
				}
				incrHotelRepository.syncIncrHotelToDB(hotels);
				Thread.sleep(200);
			}
		} catch (Exception e) {
			throw new IllegalStateException("SyncHotelToDB,thread dohandler 'IncrInventory' error" + e.getMessage(), e);
		}
	}

}
