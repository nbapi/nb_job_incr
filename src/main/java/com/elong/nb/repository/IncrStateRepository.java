/**   
 * @(#)IncrStateRepository.java	2016年9月21日	下午4:20:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.elong.nb.dao.IncrStateDao;
import com.elong.nb.dao.SqlServerDataDao;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:20:04   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrStateRepository {

	private static final Logger logger = Logger.getLogger("IncrStateLogger");

	@Resource
	private IncrStateDao incrStateDao;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	@Resource
	private MSRelationRepository msRelationRepository;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public int deleteExpireIncrData(Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrState DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		int limit = 10000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expireDate", expireDate);
		params.put("limit", limit);
		int result = 0;
		int count = 0;
		count = incrStateDao.deleteExpireIncrData(params);
		result += count;
		while (count == limit) {
			count = incrStateDao.deleteExpireIncrData(params);
		}
		logger.info("IncrState delete successfully,expireDate = " + expireDate);
		return result;
	}

	/** 
	 * 同步指定数据来源(type)的状态增量
	 *
	 * @param startTime
	 * @param endTime
	 * @param type
	 */
	public void syncStateToDB(String startTime, String endTime, String type) {
		String incrStateBatchSize = PropertiesHelper.getEnvProperties("IncrStateBatchSize", "config").toString();
		int pageSize = StringUtils.isEmpty(incrStateBatchSize) ? 2000 : Integer.valueOf(incrStateBatchSize);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("startTime", startTime);
		params.put("endTime", endTime);
		logger.info("getDataCount,params = " + params + ",type = " + type);
		int recordCount = getDataCount(params, type);
		logger.info("getDataCount,recordCount = " + recordCount);
		if (recordCount > 0) {
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				int startNum = (pageIndex - 1) * pageSize + 1;
				int endNum = pageIndex * pageSize;
				params.put("startNum", startNum);
				params.put("endNum", endNum);
				logger.info("getDataList,params = " + params + ",type = " + type);
				List<Map<String, Object>> dataList = getDataList(params, type);
				int resultSize = dataList == null ? 0 : dataList.size();
				logger.info("getDataList,result size = " + resultSize);
				if (dataList == null || dataList.size() == 0)
					continue;

				if (StringUtils.equals("HotelId", type) || StringUtils.equals("HotelCode", type)) {
					long starTime = new Date().getTime();
					// M和S酒店增量过来的，需要处理一下关系Redis
					for (Map<String, Object> row : dataList) {
						// 处理一下酒店关联HotelId是M酒店
						if (row == null || row.get("HotelId") == null || StringUtils.isEmpty((String) row.get("HotelId")))
							continue;

						String mhotelid = (String) row.get("HotelId");
						// 1表示open酒店打开,重置Redis,0关闭清除redis
						msRelationRepository.resetHotelMSCache(mhotelid);
					}
					long enTime = new Date().getTime();
					logger.info("use time = " + (enTime - starTime) + ",msRelationRepository.resetHotelMSCache.");
				}
				int count = incrStateDao.bulkInsert(dataList);
				logger.info("IncrState BulkInsert successfully,count = " + count + ",type = " + type);
			}
		}
	}

	/** 
	 * 状态增量待同步数据记录数
	 *
	 * @param params
	 * @param type
	 * @return
	 */
	private int getDataCount(Map<String, Object> params, String type) {
		if (StringUtils.equals("HotelId", type)) {
			return sqlServerDataDao.getHotelIdCount(params);
		} else if (StringUtils.equals("HotelCode", type)) {
			return sqlServerDataDao.getHotelCodeCount(params);
		} else if (StringUtils.equals("RoomId", type)) {
			return sqlServerDataDao.getRoomIdCount(params);
		} else if (StringUtils.equals("RoomTypeId", type)) {
			return sqlServerDataDao.getRoomTypeIdCount(params);
		} else if (StringUtils.equals("RatePlanId", type)) {
			return sqlServerDataDao.getRatePlanIdCount(params);
		} else if (StringUtils.equals("RatePlanPolicy", type)) {
			return sqlServerDataDao.getRatePlanPolicyCount(params);
		}
		return 0;
	}

	/** 
	 * 状态增量待同步数据
	 *
	 * @param params
	 * @param type
	 * @return
	 */
	private List<Map<String, Object>> getDataList(Map<String, Object> params, String type) {
		if (StringUtils.equals("HotelId", type)) {
			return sqlServerDataDao.getHotelIdData(params);
		} else if (StringUtils.equals("HotelCode", type)) {
			return sqlServerDataDao.getHotelCodeData(params);
		} else if (StringUtils.equals("RoomId", type)) {
			return sqlServerDataDao.getRoomIdData(params);
		} else if (StringUtils.equals("RoomTypeId", type)) {
			return sqlServerDataDao.getRoomTypeIdData(params);
		} else if (StringUtils.equals("RatePlanId", type)) {
			return sqlServerDataDao.getRatePlanIdData(params);
		} else if (StringUtils.equals("RatePlanPolicy", type)) {
			return sqlServerDataDao.getRatePlanPolicyData(params);
		}
		return Collections.emptyList();
	}

}
