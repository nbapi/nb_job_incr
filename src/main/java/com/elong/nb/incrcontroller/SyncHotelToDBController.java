/**   
 * @(#)SyncHotelToDBController.java	2016年9月7日	下午4:23:39	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.incrcontroller;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.ResponseResult;
import com.elong.nb.service.IIncrHotelService;

/**
 * IncrHotel同步Controller
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月7日 下午4:23:39   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class SyncHotelToDBController {

	private static final Logger logger = Logger.getLogger("syncIncrHotelLogger");

	@Resource
	private IIncrHotelService incrHotelService;

	/** 
	 * 同步IncrHotel到数据库
	 *
	 * @return
	 */
	@RequestMapping(value = "/SyncHotelToDB")
	public @ResponseBody String SyncHotelToDB() {
		long startTime = new Date().getTime();
		ResponseResult result = new ResponseResult();
		try {
			logger.info("SyncHotelToDB,Controller,start.");
			incrHotelService.SyncHotelToDB();
			result.setCode(ResponseResult.SUCCESS);
			result.setMessage("SyncHotelToDB successfully.");
			logger.info("SyncHotelToDB,Controller,end.");
		} catch (Exception e) {
			logger.error("SyncHotelToDB,Controller,error = " + e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage(e.getMessage());
		}
		long endTime = new Date().getTime();
		logger.info("SyncHotelToDB,Controller,use time = " + (endTime - startTime) + "ms");
		return JSON.toJSONString(result);
	}

}
