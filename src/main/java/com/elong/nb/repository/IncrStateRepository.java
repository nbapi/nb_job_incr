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
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.elong.nb.dao.IncrStateDao;
import com.elong.nb.dao.SqlServerDataDao;

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

	private static final Logger logger = Logger.getLogger("syncIncrStateLogger");

	@Resource
	private IncrStateDao incrStateDao;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	@Resource
	private M_SRelationRepository M_SRelationRepository;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public void DeleteExpireIncrData(String table, Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrState DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		logger.info("DeleteExpireIncrData start.table = " + table + ",expireDate = " + expireDate);
		int limit = 10000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expireDate", expireDate);
		params.put("limit", limit);
		int count = 0;
		count = incrStateDao.DeleteExpireIncrData(params);
		while (count == limit) {
			count = incrStateDao.DeleteExpireIncrData(params);
		}
		logger.info("DeleteExpireIncrData successfully.table = " + table + ",expireDate = " + expireDate);
	}

	/** 
	 * 同步指定数据来源(type)的状态增量
	 *
	 * @param startTime
	 * @param endTime
	 * @param type
	 */
	public void SyncStateToDB(DateTime startTime, DateTime endTime, String type) {
		int pageSize = 50000;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("startTime", startTime.toString("yyyy-MM-dd HH:mm:ss"));
		params.put("endTime", endTime.toString("yyyy-MM-dd HH:mm:ss"));
		int recordCount = getDataCount(params, type);
		int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
		for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
			int startNum = (pageIndex - 1) * pageSize + 1;
			int endNum = pageIndex * pageSize;
			params.put("startNum", startNum);
			params.put("endNum", endNum);
			List<Map<String, Object>> hotelIdDataList = getDataList(params, type);
			logger.info("SyncStateToDB,from = " + type + ",startNum = " + startNum + ",endNum = " + endNum);
			if (hotelIdDataList == null || hotelIdDataList.size() == 0)
				continue;

			if (StringUtils.equals("HotelId", type) || StringUtils.equals("HotelCode", type)) {
				// M和S酒店增量过来的，需要处理一下关系Redis
				for (Map<String, Object> row : hotelIdDataList) {
					// 处理一下酒店关联HotelId是M酒店
					if (row == null || row.get("HotelId") == null || StringUtils.isEmpty((String) row.get("HotelId")))
						continue;

//					String mhotelid = (String) row.get("HotelId");
					// 1表示open酒店打开,重置Redis,0关闭清除redis
					// M_SRelationRepository.ResetHotelMSCache(mhotelid);//TODO 暂时注掉，wcf调不通，待产品组提供新接口
				}
			}
			int count = incrStateDao.BulkInsert(hotelIdDataList);
			logger.info("SyncStateToDB,from = " + type + ",BulkInsert successfully,count = " + count);
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
