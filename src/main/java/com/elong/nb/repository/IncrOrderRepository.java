/**   
 * @(#)IncrOrderRepository.java	2016年9月21日	下午4:27:38	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.elong.nb.dao.IncrOrderDao;
import com.elong.nb.dao.SqlServerDataDao;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.util.DateHandlerUtils;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午4:27:38   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class IncrOrderRepository {

	private static final Logger logger = Logger.getLogger("IncrOrderLogger");

	@Resource
	private IncrOrderDao incrOrderDao;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	@Resource
	private CommonRepository commonRepository;

	/** 
	 * 删除过期增量数据
	 * @param table
	 * @param expireDate
	 */
	public int deleteExpireIncrData(Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("IncrOrder DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		Map<String, Object> params = new HashMap<String, Object>();
		Date startTime = DateHandlerUtils.getOffsetDate(Calendar.HOUR_OF_DAY, -1);
		Date endTime = expireDate;
		params.put("startTime", startTime);
		params.put("endTime", endTime);
		int result = 0;
		int count = incrOrderDao.deleteExpireIncrData(params);
		result += count;

		int i = 1;
		while (count != 0) {
			endTime = DateHandlerUtils.getOffsetDate(expireDate, Calendar.HOUR_OF_DAY, -(i++));
			startTime = DateHandlerUtils.getOffsetDate(expireDate, Calendar.HOUR_OF_DAY, -i);
			params.put("startTime", startTime);
			params.put("endTime", endTime);
			count = incrOrderDao.deleteExpireIncrData(params);
			logger.info("IncrOrder delete successfully,successCount = " + count + ",endTime = " + endTime);
			result += count;
		}
		return result;
	}

	/** 
	 * 读取SQL SERVER数据库里面的订单增量，同步到Mysql
	 *
	 * @param changID
	 * @return
	 */
	public long syncOrdersToDB(long changID) {
		Map<String, Object> params = new HashMap<String, Object>();
		if (changID > 0) {
			params.put("Id", changID);
		} else {
			params.put("InsertTime", DateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss"));
		}
		List<Map<String, Object>> incrOrderList = sqlServerDataDao.getDataFromReserveTrack(params);
		if (incrOrderList == null || incrOrderList.size() == 0)
			return changID;

		Date now = new Date();
		for (Map<String, Object> row : incrOrderList) {
			row.put("InsertTime", now);
			String cardNo = (row.get("CardNo") == null) ? StringUtils.EMPTY : row.get("CardNo").toString();
			if (StringUtils.equals("49", cardNo)) {
				// 订单增量 如果card是49，则通过orderFrom调用接口，返回原来的proxyid和card,并且status置成D
				OrderFromResult orderProxy = commonRepository.getProxyInfoByOrderFrom((int) row.get("OrderFrom"));
				if (orderProxy != null && orderProxy.getData() != null && !StringUtils.isEmpty(orderProxy.getData().getProxyId())) {
					row.put("ProxyId", orderProxy.getData().getProxyId());
					row.put("CardNo", orderProxy.getData().getCardNo());
					row.put("Status", "D");
				}
			}
		}
		int count = incrOrderDao.bulkInsert(incrOrderList);
		try {
			changID = (long) incrOrderList.get(incrOrderList.size() - 1).get("IncrId");
		} catch (Exception e) {
			changID = (int) incrOrderList.get(incrOrderList.size() - 1).get("IncrId");
		}
		logger.info("job.incr.SyncOrdersToDB," + MessageFormat.format("SyncIncrOrders saved ====> {0}, count = {1}", changID, count));
		return changID;
	}

}
