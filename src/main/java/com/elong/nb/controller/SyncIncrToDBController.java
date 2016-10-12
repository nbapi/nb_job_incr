/**   
 * @(#)SyncIncrToDBController.java	2016年10月11日	下午2:02:09	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.ResponseResult;
import com.elong.nb.service.IIncrHotelService;
import com.elong.nb.service.IIncrInventoryService;
import com.elong.nb.service.IIncrRateService;
import com.elong.nb.service.IIncrStateService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateUtils;

/**
 * IncrHotel、IncrRate、IncrInventory、IncrState同步Controller
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年10月11日 下午2:02:09   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class SyncIncrToDBController {

	private static final Logger logger = Logger.getLogger("IncrCommonLogger");

	@Resource
	private IIncrHotelService incrHotelService;

	@Resource
	private IIncrInventoryService incrInventoryService;

	@Resource
	private IIncrRateService incrRateService;

	@Resource
	private IIncrStateService incrStateService;

	@Resource
	private INoticeService noticeService;

	/** 
	 * 同步IncrHotel到数据库
	 *
	 * @return
	 */
	@RequestMapping(value = "/Sync*ToDB")
	public @ResponseBody String syncIncrDataToDB(HttpServletRequest request) {
		String pathVariable = request.getRequestURI().replaceAll(request.getContextPath(), "").replaceFirst("/", "");
		long startTime = new Date().getTime();
		ResponseResult result = new ResponseResult();
		try {
			result.setCode(ResponseResult.SUCCESS);
			result.setMessage(pathVariable + " successfully.");
			logger.info(pathVariable + ",Controller,start.");
			if (StringUtils.equals("SyncHotelToDB", pathVariable)) {
				incrHotelService.syncHotelToDB();
			} else if (StringUtils.equals("SyncInventoryToDB", pathVariable)) {
				incrInventoryService.syncInventoryToDB();
			} else if (StringUtils.equals("SyncRatesToDB", pathVariable)) {
				incrRateService.syncRatesToDB();
			} else if (StringUtils.equals("SyncStateToDB", pathVariable)) {
				incrStateService.syncStateToDB();
			} else {
				logger.info(pathVariable + ",is not supportted.");
				result.setMessage(pathVariable + ",is not supportted.");
			}
			logger.info(pathVariable + ",Controller,end.");
		} catch (Exception e) {
			logger.error(pathVariable + ",Controller,error = " + e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage(e.getMessage());
			noticeService.sendMessage(pathVariable + ",error:" + DateUtils.formatDate(new Date(), "YYYY-MM-DD HH:mm:ss"),
					ExceptionUtils.getFullStackTrace(e));
		}
		long endTime = new Date().getTime();
		logger.info(pathVariable + ",Controller,use time = " + (endTime - startTime) + "ms");
		return JSON.toJSONString(result);
	}
}