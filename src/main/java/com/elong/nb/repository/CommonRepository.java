/**   
 * @(#)CommonRepository.java	2016年9月14日	下午6:34:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.util.HttpUtil;

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

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

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

		try {
			logger.info("httpGet,url = " + url);
			String result = HttpUtil.httpGetData(url);
			logger.info("httpGet,result = " + result);
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

}
