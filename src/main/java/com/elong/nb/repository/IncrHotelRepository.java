/**   
 * @(#)IncrHotelRepository.java	2016年9月21日	下午4:11:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.elong.nb.dao.IncrHotelDao;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.model.bean.IncrHotel;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.bean.IncrRate;
import com.elong.nb.submeter.service.ISubmeterService;

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

	@Resource
	private IncrHotelDao incrHotelDao;

	@Resource
	private IncrInventoryDao incrInventoryDao;

	@Resource
	private IncrRateDao incrRateDao;

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
	 * 获取大于指定ChangeTime的maxRecordCount条库存增量
	 *
	 * @param changeTime
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrInventory> getIncrInventories(Date changeTime, int maxRecordCount) {
		if (changeTime == null || maxRecordCount == 0) {
			throw new IllegalArgumentException("GetIncrInventories,the paramter ['changeTime' or 'maxRecordCount'] must not be null or 0.");
		}
		return incrInventorySubmeterService.getIncrDataList(changeTime, maxRecordCount);
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
	 * 获取大于指定ChangeTime的maxRecordCount条IncrRate
	 *
	 * @param changeTime
	 * @param maxRecordCount
	 * @return
	 */
	public List<IncrRate> getIncrRates(Date changeTime, int maxRecordCount) {
		if (changeTime == null || maxRecordCount == 0) {
			throw new IllegalArgumentException("GetIncrRates,the paramter ['changeTime' or 'maxRecordCount'] must not be null or 0.");
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("changeTime", changeTime);
		params.put("maxRecordCount", maxRecordCount);
		return incrRateDao.getIncrRates(params);
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
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("incrID", incrID);
		params.put("maxRecordCount", maxRecordCount);
		return incrRateDao.getIncrRates(params);
	}

	/** 
	 * 批量插入IncrHotel
	 *
	 * @param hotels
	 */
	public void syncIncrHotelToDB(List<IncrHotel> hotels) {
		if (hotels == null || hotels.size() == 0)
			return;
		
		Collections.sort(hotels, new Comparator<IncrHotel>() {
			@Override
			public int compare(IncrHotel o1, IncrHotel o2) {
				if (o1 == null && o2 == null)
					return 0;
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				long diffVal = o1.getTriggerID() - o2.getTriggerID();
				return diffVal == 0 ? 0 : (diffVal > 0 ? 1 : -1);
			}
		});
		
		logger.info("IncrHotel BulkInsert start,recordCount = " + hotels.size());
		long startTime = System.currentTimeMillis();
		int successCount = incrHotelSubmeterService.builkInsert(hotels);
		long endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",IncrHotel BulkInsert successfully,successCount = " + successCount);
	}

}
