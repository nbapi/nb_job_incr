/**   
 * @(#)SyncIncrOrdersController.java	2016年9月14日	下午5:36:23	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.incrcontroller;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.OrderMessageResponse;
import com.elong.nb.service.IIncrOrderService;

/**
 * IncrOrder同步Controller
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月14日 下午5:36:23   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class SyncIncrOrdersController {

	private static final Logger logger = Logger.getLogger("IncrOrderLogger");

	private ExecutorService executor = Executors.newFixedThreadPool(10);

	@Resource
	private IIncrOrderService incrOrderService;

	private boolean sendConfirmMessage = false;

	@RequestMapping(value = "/changeSendConfirmMessage", method = RequestMethod.GET)
	public @ResponseBody String changeSendConfirmMessage() {
		sendConfirmMessage = !sendConfirmMessage;
		logger.info("sendConfirmMessage = " + sendConfirmMessage);
		return "sendConfirmMessage = " + sendConfirmMessage + "\n";
	}

	/** 
	 * 同步IncrOrder到数据库
	 *
	 * @return
	 */
	@RequestMapping(value = "/writeIncrOrderLog")
	public @ResponseBody String writeLog(HttpServletRequest request) {
		long startTime = new Date().getTime();
		OrderMessageResponse messageResponse = null;
		try {
			logger.info("SyncIncrOrders,Controller,start.");
			final String message = request.getParameter("message");
			logger.info("SyncIncrOrders,Controller,message = " + message);

			messageResponse = incrOrderService.checkMessage(message);
			logger.info("SyncIncrOrders,Controller,checkMessage finished");
			if (sendConfirmMessage) {
				if (OrderMessageResponse.SUCCESS.equals(messageResponse.getResponseCode())) {
					executor.submit(new Runnable() {
						public void run() {
							try {
								logger.info(Thread.currentThread().getName() + " begin to handlerMessage.");
								incrOrderService.handlerMessage(message);
								logger.info(Thread.currentThread().getName() + " end to handlerMessage.");
							} catch (Exception e) {
								logger.error("SyncIncrOrders,Controller,handlerMessage error = " + e.getMessage(), e);
							}
						}
					});
				} else if (OrderMessageResponse.IGNORE.equals(messageResponse.getResponseCode())) {
					// 忽略数据直接返回成功
					messageResponse.setResponseCode(OrderMessageResponse.SUCCESS);
				}
			}
			logger.info("SyncIncrOrders,Controller,end.");
		} catch (Exception e) {
			logger.error("SyncIncrOrders,Controller,error = " + e.getMessage(), e);
			messageResponse.setResponseCode(OrderMessageResponse.FAILURE);
			messageResponse.setExceptionMessage(e.getMessage());
		}
		String result = JSON.toJSONString(messageResponse);
		logger.info("SyncIncrOrders,Controller,result = " + result);
		long endTime = new Date().getTime();
		logger.info("SyncIncrOrders,Controller,use time = " + (endTime - startTime) + "ms");
		return result;
	}

}
