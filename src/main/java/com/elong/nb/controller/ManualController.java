/**   
 * @(#)ManualController.java	2017年4月21日	下午5:30:18	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.common.util.StringUtils;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.submeter.service.ISubmeterService;

/**
 * 手动修改某些值
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 下午5:30:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class ManualController {

	@Resource
	private IImpulseSenderService impulseSenderService;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@RequestMapping(value = "/test/insert/{tablePrefix}/{count}")
	public @ResponseBody String testInsert(@PathVariable("tablePrefix") String tablePrefix, @PathVariable("count") String count) {
		Object result = null;
		int countInt = Integer.valueOf(count);
		if (StringUtils.equals(tablePrefix, "IncrHotel")) {
			try {
				result = incrHotelSubmeterService.builkInsert(buildIncrHotelList(countInt));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (StringUtils.equals(tablePrefix, "IncrInventory")) {
			try {
				result = incrInventorySubmeterService.builkInsert(buildIncrInventoryList(countInt));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return JSON.toJSONString(result);
	}

	@RequestMapping(value = "/test/initImpulseSender/{tablePrefix}/{idVal}")
	public @ResponseBody String initImpulseSender(@PathVariable("tablePrefix") String tablePrefix, @PathVariable("idVal") String idVal) {
		String key = tablePrefix + "_ID";
		try {
			Long idLong = Long.valueOf(idVal);
			impulseSenderService.putId(tablePrefix + "_ID", idLong);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "delImpulseSender success.key = " + key;
	}

	@RequestMapping(value = "/test/delSubmeterTableCache/{tablePrefix}")
	public @ResponseBody String delSubmeterTableCache(@PathVariable("tablePrefix") String tablePrefix) {
		ICacheKey cacheKey = RedisManager.getCacheKey(tablePrefix + ".Submeter.TableNames");
		try {
			// 清除老数据
			redisManager.del(cacheKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "delSubmeterTableCache success.tablePrefix = " + tablePrefix;
	}

	@RequestMapping(value = "/putSubTableNumber/{tablePrefix}/{subTableNumber}")
	public @ResponseBody String putSubTableNumber(@PathVariable("tablePrefix") String tablePrefix,
			@PathVariable("subTableNumber") String subTableNumber) {
		try {
			incrSetInfoService.put(tablePrefix + ".SubTable.Number", Long.valueOf(subTableNumber));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "putSubTableNumber success.tablePrefix = " + tablePrefix + ",subTableNumber = " + subTableNumber;
	}

	protected List<IncrHotel> buildIncrHotelList(int count) {
		List<IncrHotel> rowList = new ArrayList<IncrHotel>();
		for (int i = 0; i < count; i++) {
			IncrHotel row = new IncrHotel();
			row.setTrigger("asdf");
			row.setTriggerID(i);
			row.setChangeTime(new Date());
			row.setEndDate(new Date());
			row.setHotelID("13412");
			row.setInsertTime(new Date());
			row.setStartDate(new Date());
			rowList.add(row);
		}
		return rowList;
	}

	protected List<IncrInventory> buildIncrInventoryList(int count) {
		List<IncrInventory> rowList = new ArrayList<IncrInventory>();
		for (int i = 0; i < count; i++) {
			IncrInventory row = new IncrInventory();
			row.setAvailableAmount(1);
			row.setAvailableDate(new Date());
			row.setChangeID((long) i);
			row.setChangeTime(new Date());
			row.setEndDate(new Date());
			row.setEndTime("00:00");
			row.setHotelCode("1234");
			row.setHotelID("13412");
			row.setIC_BeginTime("00:00");
			row.setIC_EndTime("23:49");
			row.setInsertTime(new Date());
			row.setIsInstantConfirm(false);
			row.setOperateTime(new Date());
			row.setOverBooking(1);
			row.setRoomTypeID("1234132");
			row.setStartDate(new Date());
			row.setStartTime("23:58");
			row.setStatus(false);
			rowList.add(row);
		}
		return rowList;
	}

}
