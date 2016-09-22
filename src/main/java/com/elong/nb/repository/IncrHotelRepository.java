/**   
 * @(#)IncrHotelRepository.java	2016年9月21日	下午4:11:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.elong.nb.dao.IncrHotelDao;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;

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

	private static final Logger logger = Logger.getLogger("syncIncrHotelLogger");

	@Resource
	private IncrHotelDao incrHotelDao;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IncrRateDao incrRateDao;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public void DeleteExpireIncrData(String table, Date expireDate) {
		logger.info("DeleteExpireIncrData start.table = " + table + ",expireDate = " + expireDate);
		int limit = 10000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expireDate", expireDate);
		params.put("limit", limit);
		int count = 0;
		count = incrHotelDao.DeleteExpireIncrData(params);
		while (count == limit) {
			count = incrHotelDao.DeleteExpireIncrData(params);
		}
		logger.info("DeleteExpireIncrData successfully.table = " + table + ",expireDate = " + expireDate);
	}

	/** 
	 * 获取trigger的最后一条IncrHotel 
	 *
	 * @param trigger
	 * @return
	 */
	public IncrHotel GetLastHotel(String trigger) {
		return incrHotelDao.GetLastHotel(trigger);
	}

	/** 
	 * 获取大于指定ChangeTime的maxRecordCount条库存增量
	 *
	 * @param changeTime
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrInventory> GetIncrInventories(Date changeTime, int maxRecordCount) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("changeTime", changeTime);
		params.put("maxRecordCount", maxRecordCount);
		return incrInventoryDao.GetIncrInventories(params);
	}

	/** 
	 * 获取大于指定IncrID的maxRecordCount条库存增量
	 *
	 * @param incrID
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrInventory> GetIncrInventories(long incrID, int maxRecordCount) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("incrID", incrID);
		params.put("maxRecordCount", maxRecordCount);
		return incrInventoryDao.GetIncrInventories(params);
	}

	/** 
	 * 获取大于指定ChangeTime的maxRecordCount条IncrRate
	 *
	 * @param changTime
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrRate> GetIncrRates(Date changTime, int maxRecordCount) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("changTime", changTime);
		params.put("maxRecordCount", maxRecordCount);
		return incrRateDao.GetIncrRates(params);
	}

	/** 
	 * 获取大于指定IncrID的maxRecordCount条IncrRate
	 *
	 * @param incrID
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrRate> GetIncrRates(long incrID, int maxRecordCount) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("incrID", incrID);
		params.put("maxRecordCount", maxRecordCount);
		return incrRateDao.GetIncrRates(params);
	}

	/** 
	 * 批量插入IncrHotel
	 *
	 * @param hotels
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void SyncIncrHotelToDB(List<IncrHotel> hotels) {
		int count = incrHotelDao.BulkInsert(hotels);
		logger.info("IncrHotel BulkInsert successfully,count = " + count);
	}

}
