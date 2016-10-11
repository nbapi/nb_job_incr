/**   
 * @(#)SyncInventoryToDBController.java	2016年9月9日	上午10:01:05	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.incrcontroller;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.ResponseResult;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateUtils;

/**
 * IncrInventory同步Controller
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月9日 上午10:01:05   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class SyncInventoryToDBController {

	private static final Logger logger = Logger.getLogger("IncrInventoryLogger");

	@Resource
	private IIncrInventoryService incrInventoryService;
	
	@Resource
	private INoticeService noticeService;

	/** 
	 * 同步IncrInventory到数据库
	 *
	 * @return
	 */
	@RequestMapping(value = "/SyncInventoryToDB")
	public @ResponseBody String syncInventoryToDB() {
		long startTime = new Date().getTime();
		ResponseResult result = new ResponseResult();
		try {
			logger.info("SyncInventoryToDB,Controller,start.");
			incrInventoryService.syncInventoryToDB();
			result.setCode(ResponseResult.SUCCESS);
			result.setMessage("SyncInventoryToDB successfully.");
			logger.info("SyncInventoryToDB,Controller,end.");
		} catch (Exception e) {
			logger.error("SyncInventoryToDB,Controller,error = " + e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage(e.getMessage());
			noticeService.sendMessage("SyncInventoryToDB,error:" + DateUtils.formatDate(new Date(), "YYYY-MM-DD HH:mm:ss"), ExceptionUtils.getFullStackTrace(e));
		}
		long endTime = new Date().getTime();
		logger.info("SyncInventoryToDB,Controller,use time = " + (endTime - startTime) + "ms");
		return JSON.toJSONString(result);
	}

}
