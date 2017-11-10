/**   
 * @(#)CommonRepository.java	2016年9月14日	下午6:34:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.NbapiHttpRequest;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.common.util.HttpClientUtil;
import com.elong.nb.model.OrderFromResult;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月14日 下午6:34:04   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class CommonRepository {

	private static final Logger logger = Logger.getLogger("IncrCommonLogger");

	private RedisManager redisManager = RedisManager.getInstance("redis_shared", "redis_shared");
	
	private RedisManager redisManagerIncr = RedisManager.getInstance("redis_shared_2", "redis_shared_2");

	/** 
	 * 获取代码编号ProxyId、CardNo会员卡号
	 *
	 * @param orderFromId
	 * @return
	 */
	public OrderFromResult getProxyInfoByOrderFrom(int orderFromId) {
		OrderFromResult orderFromResult = null;
		final String minitorKey = MessageFormat.format(RedisKeyConst.KEY_Proxy_CardNo_OrderFrom, orderFromId + "");
		ICacheKey cacheKey = RedisManager.getCacheKey(minitorKey);
		boolean exists = redisManager.exists(cacheKey);
		logger.info("getProxyInfoByOrderFrom,redis exists key = " + minitorKey + ",exists = " + exists);
		if (exists) {
			String result = redisManager.getStr(cacheKey);
			orderFromResult = JSON.parseObject(result, OrderFromResult.class);
			return orderFromResult;
		}

		String orderFromNameUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("OrderFromNameUrl");
		orderFromNameUrl = StringUtils.isEmpty(orderFromNameUrl) ? "http://api.vip.elong.com/admin.php/Api/getprojectname?orderFromId={0}"
				: orderFromNameUrl;
		String url = MessageFormat.format(orderFromNameUrl, orderFromId + "");
		NbapiHttpRequest nbapiHttpRequest = new NbapiHttpRequest();
		nbapiHttpRequest.setUrl(orderFromNameUrl);

		try {
			logger.info("httpGet,url = " + url);
			String result = HttpClientUtil.httpGet(nbapiHttpRequest);
			logger.info("httpGet,result = " + result);
			result = StringUtils.replace(result, "\\", "");
			orderFromResult = JSON.parseObject(result, OrderFromResult.class);
		} catch (Exception ex) {
			orderFromResult = orderFromResult == null ? new OrderFromResult() : orderFromResult;
			orderFromResult.setCode(0);
			orderFromResult.setData(null);
			orderFromResult.setMsg("反序列化出现错误");
		}
		if (orderFromResult != null && orderFromResult.getCode() == 200 && orderFromResult.getData() != null
				&& !StringUtils.isEmpty(orderFromResult.getData().getProxyId())) {
			redisManager.put(cacheKey, orderFromResult);
		}
		return orderFromResult;
	}

	public Map<String, String> batchHashGetMapFromRedis(Map<String, List<String>> keyMap) {
		long startTime = System.currentTimeMillis();
		if (keyMap == null || keyMap.size() == 0)
			return Collections.emptyMap();

		String batchSizeGetFromRedis = CommonsUtil.CONFIG_PROVIDAR.getProperty("BatchSizeGetFromRedis");
		Map<String, String> resultMap = new HashMap<String, String>();
		for (Map.Entry<String, List<String>> entry : keyMap.entrySet()) {
			ICacheKey hashKeyName = RedisManager.getCacheKey(entry.getKey());
			List<String> keyList = entry.getValue();
			int recordCount = keyList.size();
			int pageSize = StringUtils.isEmpty(batchSizeGetFromRedis) ? 2000 : Integer.valueOf(batchSizeGetFromRedis);
			int pageCount = (int) Math.ceil(recordCount * 1.0 / pageSize);
			for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
				int startNum = (pageIndex - 1) * pageSize;
				int endNum = pageIndex * pageSize > recordCount ? recordCount : pageIndex * pageSize;
				List<String> subKeyList = keyList.subList(startNum, endNum);
				List<String> subValList = redisManagerIncr.hashMGet(hashKeyName, subKeyList.toArray(new String[0]));
				for (int i = 0; i < subKeyList.size(); i++) {
					resultMap.put(subKeyList.get(i), subValList.get(i));
				}
			}
		}
		logger.info("use time = " + (System.currentTimeMillis() - startTime) + ",batchHashGetMapFromRedis,keyMap size = " + keyMap.size());
		return resultMap;
	}

}
