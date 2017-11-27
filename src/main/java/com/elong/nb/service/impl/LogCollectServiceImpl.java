/**   
 * @(#)LogCollectServiceImpl.java	2017年7月14日	下午5:13:08	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Calendar;
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
import com.elong.nb.dao.IncrHotelDao;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.dao.IncrStateDao;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
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
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrInventory> incrHotelSubmeterService;

	@Resource
	private IncrHotelDao incrHotelDao;

	@Resource(name = "incrRateSubmeterService")
	private ISubmeterService<IncrRate> incrRateSubmeterService;

	@Resource
	private IncrRateDao incrRateDao;

	@Resource
	private IncrStateDao incrStateDao;

	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	@Override
	public String writeLog() {
		final Date logTime = new Date();

		Future<String> future1 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrInventoryLog(logTime);
			}
		});

		Future<String> future2 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrOrderLog(logTime);
			}
		});

		Future<String> future3 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrRateLog(logTime);
			}
		});

		Future<String> future4 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrStateLog(logTime);
			}
		});

		Future<String> future5 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrHotelLog(logTime, "Rate");
			}
		});

		Future<String> future6 = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return writeIncrHotelLog(logTime, "Inventory");
			}
		});

		try {
			logger.info("writeIncrInventoryLog = " + future1.get() + ",writeIncrOrderLog = " + future2.get());
			logger.info("writeIncrRateLog = " + future3.get() + ",writeIncrStateLog = " + future4.get());
			logger.info("writeIncrHotelLogFromRate = " + future5.get() + ",writeIncrHotelLogFromInventory = " + future6.get());
			return "Success";
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	private String writeIncrHotelLog(Date logTime, String triggerName) {
		String dataSource = incrHotelSubmeterService.getLastShardDataSource();
		String subTableName = incrHotelSubmeterService.getLastTableName();
		IncrHotel masterIncrHotel = incrHotelDao.getLastIncrFromWrite(dataSource, subTableName, triggerName);
		IncrHotel slaveIncrHotel = incrHotelDao.getLastIncrFromRead(dataSource, subTableName, triggerName);
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Data.name() + "_" + triggerName);
		statisticModel.setChangeTime(DateHandlerUtils.formatDate(masterIncrHotel.getChangeTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate(masterIncrHotel.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate(slaveIncrHotel.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		int recordCount = incrHotelDao.getRecordCountFromRead(dataSource, subTableName, getPreviousMinuteBegin(logTime),
				getPreviousMinuteEnd(logTime), triggerName);
		statisticModel.setRecordCount(recordCount + "");
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private String writeIncrInventoryLog(Date logTime) {
		String dataSource = incrInventorySubmeterService.getLastShardDataSource();
		String subTableName = incrInventorySubmeterService.getLastTableName();
		IncrInventory masterIncrInventory = incrInventoryDao.getLastIncrFromWrite(dataSource, subTableName);
		IncrInventory slaveIncrInventory = incrInventoryDao.getLastIncrFromRead(dataSource, subTableName);
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Inventory.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate(masterIncrInventory.getChangeTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate(masterIncrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate(slaveIncrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		int recordCount = incrInventoryDao.getRecordCountFromRead(dataSource, subTableName, getPreviousMinuteBegin(logTime),
				getPreviousMinuteEnd(logTime));
		statisticModel.setRecordCount(recordCount + "");
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private String writeIncrOrderLog(Date logTime) {
		Map<String, Object> masterIncrOrder = incrOrderDao.getLastIncrOrderFromWrite();
		Map<String, Object> slaveIncrOrder = incrOrderDao.getLastIncrOrderFromRead();
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Order.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate((Date) masterIncrOrder.get("ChangeTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate((Date) masterIncrOrder.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(logTime, "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate((Date) slaveIncrOrder.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		int recordCount = incrOrderDao.getRecordCountFromRead(getPreviousMinuteBegin(logTime), getPreviousMinuteEnd(logTime));
		statisticModel.setRecordCount(recordCount + "");
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private String writeIncrRateLog(Date logTime) {
		String dataSource = incrRateSubmeterService.getLastShardDataSource();
		String subTableName = incrRateSubmeterService.getLastTableName();
		IncrRate masterIncrRate = incrRateDao.getLastIncrFromWrite(dataSource, subTableName);
		IncrRate slaveIncrRate = incrRateDao.getLastIncrFromRead(dataSource, subTableName);
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.Rate.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate(masterIncrRate.getChangeTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate(masterIncrRate.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(logTime, "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate(slaveIncrRate.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
		int recordCount = incrRateDao.getRecordCountFromRead(dataSource, subTableName, getPreviousMinuteBegin(logTime),
				getPreviousMinuteEnd(logTime));
		statisticModel.setRecordCount(recordCount + "");
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private String writeIncrStateLog(Date logTime) {
		Map<String, Object> masterIncrState = incrStateDao.getLastIncrFromWrite();
		Map<String, Object> slaveIncrState = incrStateDao.getLastIncrFromRead();
		IncrInsertStatistic statisticModel = new IncrInsertStatistic();
		statisticModel.setBusiness_type(BUSINESS_TYPE);
		statisticModel.setIncrType(EnumIncrType.State.name());
		statisticModel.setChangeTime(DateHandlerUtils.formatDate((Date) masterIncrState.get("ChangeTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setInsertTime(DateHandlerUtils.formatDate((Date) masterIncrState.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setLog_time(DateHandlerUtils.formatDate(logTime, "yyyy-MM-dd HH:mm:ss"));
		statisticModel.setSlaveInsertTime(DateHandlerUtils.formatDate((Date) slaveIncrState.get("InsertTime"), "yyyy-MM-dd HH:mm:ss"));
		int recordCount = incrStateDao.getRecordCountFromRead(getPreviousMinuteBegin(logTime), getPreviousMinuteEnd(logTime));
		statisticModel.setRecordCount(recordCount + "");
		minitorLogger.info(JSON.toJSONString(statisticModel));
		return "Success";
	}

	private static Date getPreviousMinuteBegin(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -5);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	private static Date getPreviousMinuteEnd(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -5);
		calendar.set(Calendar.SECOND, 59);
		return calendar.getTime();
	}

}
