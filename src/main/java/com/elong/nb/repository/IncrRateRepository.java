/**   
 * @(#)IncrRateRepository.java	2016年9月21日	下午4:15:22	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.dao.IncrRateDao;
import com.elong.nb.dao.SqlServerDataDao;
import com.elong.nb.service.IFilterService;

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

	private static final Logger logger = Logger.getLogger("IncrRateLogger");

	@Resource
	private MSRelationRepository msRelationRepository;

	@Resource
	private CommonRepository commonRepository;

	@Resource
	private IncrRateDao incrRateDao;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	@Resource
	private IFilterService filterService;

	/** 
	 * IncrRate同步到数据库
	 *
	 * @param changID
	 * @return
	 */
	public long syncRatesToDB(long changID) {
		Map<String, Object> params = new HashMap<String, Object>();
		String tablename = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrRateFromTable");
		if (StringUtils.isEmpty(tablename)) {
			params.put("tablename", "PriceInfo_track");
		} else {
			params.put("tablename", tablename);
		}
		if (changID > 0) {
			params.put("Id", changID);
		} else {
			params.put("InsertTime", DateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss"));
		}
		logger.info("getDataFromPriceInfoTrack, params = " + params);
		long startTime = System.currentTimeMillis();
		List<Map<String, Object>> incrRateList = sqlServerDataDao.getDataFromPriceInfoTrack(params);
		if (StringUtils.isEmpty(tablename)) {
			incrRateList = sqlServerDataDao.getDataFromPriceInfoTrack(params);
		} else {
			incrRateList = sqlServerDataDao.getDataFromPriceInfo(params);
		}
		long endTime = System.currentTimeMillis();
		int incrRateListSize = (incrRateList == null) ? 0 : incrRateList.size();
		logger.info("use time = " + (endTime - startTime) + ",getDataFromPriceInfoTrack, incrRateList size = " + incrRateListSize);
		if (incrRateList == null || incrRateList.size() == 0)
			return changID;

		Map<String, String> dict = new HashMap<String, String>();
		List<Map<String, Object>> incrRates = new ArrayList<Map<String, Object>>();
		Date validDate = DateTime.now().plusYears(1).toDate();

		startTime = System.currentTimeMillis();
		Set<String> filteredSHotelIds = commonRepository.fillFilteredSHotelsIds();
		for (Map<String, Object> rowMap : incrRateList) {
			if (rowMap == null)
				continue;
			rowMap.put("InsertTime", new Date());

			String shotelId = (String) rowMap.get("HotelCode");
			if (filteredSHotelIds.contains(shotelId)) {
				// if (filterService.doFilter(shotelId)) {
				// logger.info("filteredSHotelIds contain value[" + shotelId + "],ignore it.");
				continue;
			}

			String mhotelId = null;
			if (dict.containsKey(shotelId)) {
				mhotelId = dict.get(shotelId);
			} else {
				mhotelId = msRelationRepository.getMHotelId(shotelId);
				dict.put(shotelId, mhotelId);
			}
			rowMap.put("HotelID", mhotelId);

			Date endDate = (Date) rowMap.get("EndDate");
			endDate = (endDate.compareTo(validDate) > 0) ? validDate : endDate;
			rowMap.put("EndDate", endDate);

			incrRates.add(rowMap);
		}
		endTime = System.currentTimeMillis();
		logger.info("use time = " + (endTime - startTime) + ",fillFilteredSHotelsIds, incrRates size = " + incrRates.size());

		int recordCount = incrRates.size();
		if (recordCount > 0) {
			int successCount = 0;
			logger.info("IncrRate BulkInsert start,recordCount = " + recordCount);
			String incrRateBatchSize = CommonsUtil.CONFIG_PROVIDAR.getProperty("IncrRateBatchSize");
			int pageSize = StringUtils.isEmpty(incrRateBatchSize) ? 2000 : Integer.valueOf(incrRateBatchSize);
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			startTime = System.currentTimeMillis();
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				int startNum = (pageIndex - 1) * pageSize;
				int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
				successCount += incrRateDao.bulkInsert(incrRates.subList(startNum, endNum));
			}
			endTime = System.currentTimeMillis();
			logger.info("use time = " + (endTime - startTime) + ",IncrRate BulkInsert successfully,successCount = " + successCount);
			changID = (long) incrRates.get(incrRates.size() - 1).get("ChangeID");
		}

		return changID;
	}

}
