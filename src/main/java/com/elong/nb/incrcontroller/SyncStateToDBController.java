/**   
 * @(#)SyncStateToDBController.java	2016年9月7日	下午4:23:39	   
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
import com.elong.nb.service.IIncrStateService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateUtils;

/**
 * IncrState同步Controller
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
public class SyncStateToDBController {

	private static final Logger logger = Logger.getLogger("IncrStateLogger");

	@Resource
	private IIncrStateService incrStateService;
	
	@Resource
	private INoticeService noticeService;

	/** 
	 * 同步IncrState到数据库
	 *
	 * @return
	 */
	@RequestMapping(value = "/SyncStateToDB")
	public @ResponseBody String syncStateToDB() {
		long startTime = new Date().getTime();
		ResponseResult result = new ResponseResult();
		try {
			logger.info("SyncStateToDB,Controller,start.");
			incrStateService.syncStateToDB();
			result.setCode(ResponseResult.SUCCESS);
			result.setMessage("SyncStateToDB successfully.");
			logger.info("SyncStateToDB,Controller,end.");
		} catch (Exception e) {
			logger.error("SyncStateToDB,Controller,error = " + e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage(e.getMessage());
			noticeService.sendMessage("SyncInventoryToDB,error:" + DateUtils.formatDate(new Date(), "YYYY-MM-DD HH:mm:ss"), ExceptionUtils.getFullStackTrace(e));
		}
		long endTime = new Date().getTime();
		logger.info("SyncStateToDB,Controller,use time = " + (endTime - startTime) + "ms");
		return JSON.toJSONString(result);
	}

}
