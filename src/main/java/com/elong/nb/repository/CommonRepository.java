/**   
 * @(#)CommonRepository.java	2016年9月14日	下午6:34:04	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.consts.RedisKeyConst;
import com.elong.nb.model.OrderFromResult;
import com.elong.nb.util.HttpClientUtils;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

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

	private static final Log logger = LogFactory.getLog(CommonRepository.class);

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	/** 
	 * 本地缓存文件中过滤SHotelIds	
	 *
	 * Set<String> CommonRepository.java filteredSHotelIds
	 */
	private Set<String> filteredSHotelIds = new HashSet<String>();

	/** 
	 * 读取文件中SHotelId,过滤SHotelId
	 *
	 * @return
	 */
	public Set<String> FillFilteredSHotelsIds() {
		if (this.filteredSHotelIds.size() > 0) {
			return filteredSHotelIds;
		}

		String rootPath = Thread.currentThread().getContextClassLoader().getResource("/").getPath();
		String filePath = rootPath + "conf/custom/env/FilteCtripQunarHotelIds.txt";
		File file = new File(filePath);
		if (file.exists()) {
			try {
				List<String> contentList = FileUtils.readLines(file);
				if (contentList != null && contentList.size() > 0) {
					for (String content : contentList) {
						if (StringUtils.isEmpty(content))
							continue;
						filteredSHotelIds.add(content.trim());
					}
				}
				logger.info("FillFilteredSHotelsIds size = ," + filteredSHotelIds.size());
			} catch (Exception ex) {
				String title = "增量库存在读取携程去哪过滤酒店信息时抛出异常";
				String content = "读取携程去哪过滤的酒店信息文件出现异常,异常原因:" + ex.getMessage();
				logger.error("title =" + title + ",content = " + content, ex);
			}
		} else {
			logger.info("FillFilteredSHotelsIds,File Not Exist!!!");
		}
		return filteredSHotelIds;
	}

	/** 
	 * 获取代码编号ProxyId、CardNo会员卡号
	 *
	 * @param orderFromId
	 * @return
	 */
	public OrderFromResult GetProxyInfoByOrderFrom(int orderFromId) {
		OrderFromResult orderFromResult = null;
		final String minitorKey = MessageFormat.format(RedisKeyConst.KEY_Proxy_CardNo_OrderFrom, orderFromId);
		ICacheKey cacheKey = new ICacheKey() {
			@Override
			public String getKey() {
				return minitorKey;
			}

			@Override
			public int getExpirationTime() {
				return -1;
			}
		};
		if (redisManager.exists(cacheKey)) {
			orderFromResult = (OrderFromResult) redisManager.getObj(cacheKey);
			return orderFromResult;
		}

		String OrderFromNameUrl = PropertiesHelper.getEnvProperties("OrderFromNameUrl", "config").toString();
		OrderFromNameUrl = StringUtils.isEmpty(OrderFromNameUrl) ? "http://api.vip.elong.com/admin.php/Api/getprojectname?orderFromId={0}"
				: OrderFromNameUrl;
		String url = MessageFormat.format(OrderFromNameUrl, orderFromId);

		try {
			logger.info("httpGet,url = " + url);
			String result = HttpClientUtils.httpGet(url);
			logger.info("httpGet,result = " + result);
			JSONObject jsonObj = JSON.parseObject(result);
			if (jsonObj != null) {
				orderFromResult = jsonObj.getObject("Data", OrderFromResult.class);
			}
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
