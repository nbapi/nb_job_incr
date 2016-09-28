/**   
 * @(#)IncrRateRepository.java	2016年9月21日	下午4:15:22	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.dao.SqlServerDataDao;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:15:22   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrRateRepository {

	private static final Logger logger = Logger.getLogger("syncIncrRateLogger");

	private Set<String> filteredSHotelIds = new HashSet<String>();

	@Resource
	private M_SRelationRepository M_SRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private IncrRateDao incrRateDao;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public int DeleteExpireIncrData(String table, Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrRate DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		int limit = 10000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expireDate", expireDate);
		params.put("limit", limit);
		int result = 0;
		int count = 0;
		count = incrRateDao.DeleteExpireIncrData(params);
		result += count;
		while (count == limit) {
			count = incrRateDao.DeleteExpireIncrData(params);
		}
		logger.info("IncrRate delete successfully,expireDate = " + expireDate);
		return result;
	}

	/** 
	 * IncrRate同步到数据库
	 *
	 * @param changID
	 * @return
	 */
	public long SyncRatesToDB(long changID) {
		Map<String, Object> params = new HashMap<String, Object>();
		if (changID > 0) {
			params.put("Id", changID);
		} else {
			params.put("InsertTime", DateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss"));
		}
		logger.info("getDataFromPriceInfoTrack, params = " + params);
		List<Map<String, Object>> incrRateList = sqlServerDataDao.getDataFromPriceInfoTrack(params);
		int incrRateListSize = (incrRateList == null) ? 0 : incrRateList.size();
		logger.info("getDataFromPriceInfoTrack, incrRateList size = " + incrRateListSize);
		if (incrRateList == null || incrRateList.size() == 0)
			return changID;

		Map<String, String> dict = new HashMap<String, String>();
		List<Map<String, Object>> incrRates = new ArrayList<Map<String, Object>>();
		Date validDate = DateTime.now().plusYears(1).toDate();

		filteredSHotelIds = commonRepository.FillFilteredSHotelsIds();
		for (Map<String, Object> rowMap : incrRateList) {
			if (rowMap == null)
				continue;
			rowMap.put("InsertTime", new Date());

			String shotelId = (String) rowMap.get("HotelCode");
			if (filteredSHotelIds.contains(shotelId)) {
				String message = MessageFormat.format("CQ FilteredSHotelID：{0}", shotelId);
				logger.info("SyncRatesToDB," + message);
				continue;
			}

			String mhotelId = null;
			if (dict.containsKey(shotelId)) {
				mhotelId = dict.get(shotelId);
			} else {
				mhotelId = this.M_SRelationRepository.GetMHotelId(shotelId);
				dict.put(shotelId, mhotelId);
			}
			rowMap.put("HotelID", mhotelId);

			Date endDate = (Date) rowMap.get("EndDate");
			endDate = (endDate.compareTo(validDate) > 0) ? validDate : endDate;
			rowMap.put("EndDate", endDate);

			incrRates.add(rowMap);
		}
		int count = incrRateDao.BulkInsert(incrRates);
		logger.info("IncrRate BulkInsert successfully,count = " + count);

		changID = (long) incrRates.get(incrRates.size() - 1).get("ChangeID");
		return changID;
	}

}
