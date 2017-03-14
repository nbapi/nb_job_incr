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
import com.elong.nb.service.IIncrOrderService;
import com.elong.nb.service.IIncrRateService;
import com.elong.nb.service.IIncrStateService;
import com.elong.nb.service.INoticeService;
import com.elong.nb.util.DateHandlerUtils;

/**
 * IncrHotel、IncrRate、IncrInventory、IncrState、IncrOrder(兜底)同步Controller
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
	private IIncrOrderService incrOrderService;

	@Resource
	private INoticeService noticeService;

	/** 
	 * 删除酒店增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/DelHotelFromDB")
	public @ResponseBody String delHotelFromDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "DelHotelFromDB");
	}

	/** 
	 * 酒店增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncHotelToDB")
	public @ResponseBody String syncHotelToDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncHotelToDB");
	}

	/** 
	 * 删除库存增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/DelInventoryFromDB")
	public @ResponseBody String delInventoryFromDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "DelInventoryFromDB");
	}

	/** 
	 * 库存增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncInventoryToDB")
	public @ResponseBody String syncInventoryToDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncInventoryToDB");
	}

	/** 
	 * 黑名单变化引起库存增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncInventoryDueToBlack")
	public @ResponseBody String syncInventoryDueToBlack(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncInventoryDueToBlack");
	}

	/** 
	 * 删除房价增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/DelRatesFromDB")
	public @ResponseBody String delRatesFromDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "DelRatesFromDB");
	}

	/** 
	 * 房价增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncRatesToDB")
	public @ResponseBody String syncRatesToDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncRatesToDB");
	}

	/** 
	 * 删除状态增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/DelStateFromDB")
	public @ResponseBody String delStateFromDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "DelStateFromDB");
	}

	/** 
	 * 状态增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncStateToDB")
	public @ResponseBody String syncStateToDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncStateToDB");
	}

	/** 
	 * 删除状态增量job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/DelOrderFromDB")
	public @ResponseBody String delOrderFromDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "DelOrderFromDB");
	}

	/** 
	 * 订单增量兜底job
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/SyncOrderToDB")
	public @ResponseBody String syncOrderToDB(HttpServletRequest request) {
		return doHandlerIncrData(request, "SyncOrderToDB");
	}

	/** 
	 * 同步Incr到数据库
	 *
	 * @return
	 */
	private String doHandlerIncrData(HttpServletRequest request, String pathVariable) {
		long startTime = System.currentTimeMillis();
		ResponseResult result = new ResponseResult();
		try {
			result.setCode(ResponseResult.SUCCESS);
			result.setMessage(pathVariable + " successfully.");
			logger.info(pathVariable + ",Controller,start.");
			if (StringUtils.equals("SyncHotelToDB", pathVariable)) {
				incrHotelService.syncHotelToDB();
			} else if (StringUtils.equals("SyncInventoryToDB", pathVariable)) {
				incrInventoryService.syncInventoryToDB();
			} else if (StringUtils.equals("SyncInventoryDueToBlack", pathVariable)) {
				incrInventoryService.syncInventoryDueToBlack();
			} else if (StringUtils.equals("SyncRatesToDB", pathVariable)) {
				incrRateService.syncRatesToDB();
			} else if (StringUtils.equals("SyncStateToDB", pathVariable)) {
				incrStateService.syncStateToDB();
			} else if (StringUtils.equals("SyncOrderToDB", pathVariable)) {
				incrOrderService.syncOrderToDB();
			} else if (StringUtils.equals("DelHotelFromDB", pathVariable)) {
				incrHotelService.delHotelFromDB();
			} else if (StringUtils.equals("DelInventoryFromDB", pathVariable)) {
				String delCycleCountStr = request.getParameter("delCycleCount");
				int delCycleCount = StringUtils.isEmpty(delCycleCountStr) ? 0 : Integer.valueOf(delCycleCountStr);
				incrInventoryService.delInventoryFromDB(delCycleCount);
			} else if (StringUtils.equals("DelRatesFromDB", pathVariable)) {
				incrRateService.delRatesFromDB();
			} else if (StringUtils.equals("DelStateFromDB", pathVariable)) {
				incrStateService.delStateFromDB();
			} else if (StringUtils.equals("DelOrderFromDB", pathVariable)) {
				incrOrderService.delOrderFromDB();
			} else {
				logger.info(pathVariable + ",is not supportted.");
				result.setMessage(pathVariable + ",is not supportted.");
			}
			logger.info(pathVariable + ",Controller,end.");
		} catch (Exception e) {
			logger.error(pathVariable + ",Controller,error = " + e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage(e.getMessage());
			noticeService.sendMessage(pathVariable + ",error:" + DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"),
					ExceptionUtils.getFullStackTrace(e));
		}
		long endTime = System.currentTimeMillis();
		logger.info(pathVariable + ",Controller,use time = " + (endTime - startTime) + "ms");
		return JSON.toJSONString(result);
	}

}
