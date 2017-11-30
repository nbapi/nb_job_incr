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
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
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
public class ShardTestController {

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource(name = "incrRateSubmeterService")
	private ISubmeterService<IncrRate> incrRateSubmeterService;
	
	@RequestMapping(value = "/test/insertIncrInv")
	public @ResponseBody String insertIncrInv() {
		List<IncrInventory> incrInventorys = new ArrayList<IncrInventory>();
		Date now = new Date();
		for(int i=0;i<130;i++){
			IncrInventory incrInventory = new IncrInventory();
			incrInventory.setAvailableAmount(3);
			incrInventory.setAvailableDate(now);
			incrInventory.setChangeID(123l);
			incrInventory.setChangeTime(now);
			incrInventory.setChannel(1);
			incrInventory.setEndDate(now);
			incrInventory.setEndTime("23:59");
			incrInventory.setHotelCode("5123421");
			incrInventory.setHotelID("5123421");
			incrInventory.setIC_BeginTime("00:00");
			incrInventory.setIC_EndTime("23:59");
			incrInventory.setInsertTime(now);
			incrInventory.setIsInstantConfirm(true);
			incrInventory.setIsStraint(2);
			incrInventory.setOperateTime(now);
			incrInventory.setOverBooking(1);
			incrInventory.setRoomTypeID("1033");
			incrInventory.setSellChannel(2);
			incrInventory.setStartDate(now);
			incrInventory.setStartTime("00:00");
			incrInventory.setStatus(true);
			incrInventorys.add(incrInventory);
		}
		int rowCount = incrInventorySubmeterService.builkInsert(incrInventorys);
		return rowCount + "";
	} 

	@RequestMapping(value = "/test/getLastHotel/{trigger}/")
	public @ResponseBody String getLastHotel(@PathVariable("trigger") String trigger) {
		IncrHotel incrHotel = incrHotelSubmeterService.getLastIncrData(trigger);
		return JSON.toJSONString(incrHotel);
	}

	@RequestMapping(value = "/test/getIncrInv/{lastId}/{recordCount}")
	public @ResponseBody String getIncrInv(@PathVariable("lastId") String lastId, @PathVariable("recordCount") String recordCount) {
		long id = Long.valueOf(lastId);
		int maxRecordCount = Integer.valueOf(recordCount);
		List<IncrInventory> inventorys = incrInventorySubmeterService.getIncrDataList(id, maxRecordCount);
		return JSON.toJSONString(inventorys);
	}

	@RequestMapping(value = "/test/getIncrRate/{lastId}/{recordCount}")
	public @ResponseBody String getIncrRate(@PathVariable("lastId") String lastId, @PathVariable("recordCount") String recordCount) {
		long id = Long.valueOf(lastId);
		int maxRecordCount = Integer.valueOf(recordCount);
		List<IncrRate> incrrates = incrRateSubmeterService.getIncrDataList(id, maxRecordCount);
		return JSON.toJSONString(incrrates);
	}

}
