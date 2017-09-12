/**   
 * @(#)IncrHotelRepository.java	2016年9月21日	下午4:11:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
import com.elong.nb.submeter.service.ISubmeterService;
import com.elong.nb.util.ConfigUtils;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:11:04   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrHotelRepository {

	private static final Logger logger = Logger.getLogger("IncrHotelLogger");

	@Resource(name = "incrHotelSubmeterService")
	private ISubmeterService<IncrHotel> incrHotelSubmeterService;

	@Resource(name = "incrInventorySubmeterService")
	private ISubmeterService<IncrInventory> incrInventorySubmeterService;

	@Resource(name = "incrRateSubmeterService")
	private ISubmeterService<IncrRate> incrRateSubmeterService;

	/** 
	 * 获取trigger的最后一条IncrHotel 
	 *
	 * @param trigger
	 * @return
	 */
	public IncrHotel getLastHotel(String trigger) {
		if (StringUtils.isEmpty(trigger)) {
			throw new IllegalArgumentException("GetLastHotel,the paramter 'trigger' must not be null or empty.");
		}
		return incrHotelSubmeterService.getLastIncrData(trigger);
	}

	/** 
	 * 获取大于指定IncrID的maxRecordCount条库存增量
	 *
	 * @param incrID
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrInventory> getIncrInventories(long incrID, int maxRecordCount) {
		if (incrID == 0l || maxRecordCount == 0) {
			throw new IllegalArgumentException("GetIncrInventories,the paramter ['incrID' or 'maxRecordCount'] must not be null or 0.");
		}
		return incrInventorySubmeterService.getIncrDataList(incrID, maxRecordCount);
	}

	/** 
	 * 获取大于指定IncrID的maxRecordCount条IncrRate
	 *
	 * @param incrID
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrRate> getIncrRates(long incrID, int maxRecordCount) {
		if (incrID == 0l || maxRecordCount == 0) {
			throw new IllegalArgumentException("GetIncrRates,the paramter ['incrID' or 'maxRecordCount'] must not be 0.");
		}
		return incrRateSubmeterService.getIncrDataList(incrID, maxRecordCount);
	}

	/** 
	 * 批量插入IncrHotel
	 *
	 * @param hotels
	 */
	public void syncIncrHotelToDB(List<IncrHotel> incrHotels) {
		int recordCount = incrHotels.size();
		if (recordCount == 0)
			return;
		logger.info("IncrHotel BulkInsert start,recordCount = " + recordCount);
		String builkInsertSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrInsertSizePerTask");
		int pageSize = StringUtils.isEmpty(builkInsertSize) ? 2000 : Integer.valueOf(builkInsertSize);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		List<MysqlHotelThread> callableList = new ArrayList<MysqlHotelThread>();
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize;
			int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
			MysqlHotelThread mysqlInventoryThread = new MysqlHotelThread(incrHotels.subList(startNum, endNum));
			callableList.add(mysqlInventoryThread);
		}
		int callableListSize = callableList.size();

		// 多线程插数据
		int mysqlInsertThreadCount = ConfigUtils.getIntConfigValue("MysqlInsertThreadCount", 32);
		mysqlInsertThreadCount = callableListSize < mysqlInsertThreadCount ? callableListSize : mysqlInsertThreadCount;
		ExecutorService executorService = Executors.newFixedThreadPool(mysqlInsertThreadCount);
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		try {
			List<Future<Integer>> futureList = executorService.invokeAll(callableList);
			executorService.shutdown();
			for (Future<Integer> future : futureList) {
				int perThreadSuccessCount = future.get();
				successCount += perThreadSuccessCount;
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",IncrHotel BulkInsert successfully,successCount = "
				+ successCount + ",threadCount = " + mysqlInsertThreadCount);
	}
	
	/**
	 * 数据库插入数据任务
	 *
	 * <p>
	 * 修改历史:											<br>  
	 * 修改日期    		修改人员   	版本	 		修改内容<br>  
	 * -------------------------------------------------<br>  
	 * 2017年9月12日 上午11:32:51   suht     1.0    	初始化创建<br>
	 * </p> 
	 *
	 * @author		suht  
	 * @version		1.0  
	 * @since		JDK1.7
	 */
	private class MysqlHotelThread implements Callable<Integer> {

		private List<IncrHotel> incrHotelList;

		public MysqlHotelThread(List<IncrHotel> incrHotelList) {
			this.incrHotelList = incrHotelList;
		}

		@Override
		public Integer call() throws Exception {
			return incrHotelSubmeterService.builkInsert(incrHotelList);
		}

	}

}
