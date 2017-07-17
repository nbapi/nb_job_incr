/**   
 * @(#)LogCollectServiceImpl.java	2017年7月14日	下午5:13:08	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.IncrInsertStatistic;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.service.LogCollectService;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.DateHandlerUtils;

/**
 * 增量数据监控日志收集
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年7月14日 下午5:13:08   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class LogCollectServiceImpl implements LogCollectService {

	private static final Logger logger = Logger.getLogger("IncrCommonLogger");

	protected static final Logger minitorLogger = Logger.getLogger("MinitorLogger");

	private static final String BUSINESS_TYPE = "nbincrinsert";

	@Resource
	private IncrOrderDao incrOrderDao;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> submeterService;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	private ExecutorService executorService = Executors.newFixedThreadPool(2);

	@Override
	public String writeLog() {
		Future<String> future1 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrInventoryLog();
			}
		});

		Future<String> future2 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrOrderLog();
			}
		});

		try {
			logger.info("writeIncrInventoryLog = " + future1.get() + ",writeIncrOrderLog = " + future2.get());
			return "Success";
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	private String writeIncrInventoryLog() {
		String subTableName = submeterService.getLastTableName();
		IncrInventory masterIncrInventory = incrInventoryDao.getLastIncrFromWrite(subTableName);
		IncrInventory slaveIncrInventory = incrInventoryDao.getLastIncrFromRead(subTableName);
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Inventory.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate(masterIncrInventory.getChangeTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate(masterIncrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate(slaveIncrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private String writeIncrOrderLog() {
		Map<String, Object> masterIncrOrder = incrOrderDao.getLastIncrOrderFromWrite();
		Map<String, Object> slaveIncrOrder = incrOrderDao.getLastIncrOrderFromRead();
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Order.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate((Date) masterIncrOrder.get("ChangeTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate((Date) masterIncrOrder.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate((Date) slaveIncrOrder.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

}
