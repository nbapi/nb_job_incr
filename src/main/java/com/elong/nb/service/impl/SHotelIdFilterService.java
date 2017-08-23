/**   
 * @(#)SHotelIdFilterService.java	2017年2月21日	上午11:12:18	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elong.nb.common.model.NbapiHttpRequest;
import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.common.util.HttpClientUtil;
import com.elong.nb.model.HotelCodeRuleRealRequest;
import com.elong.nb.model.HotelCodeRuleRealResponse;
import com.elong.nb.model.RequestBase;
import com.elong.nb.model.ResponseBase;
import com.elong.nb.service.IFilterService;

/**
 * SHotelId过滤服务
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年2月21日 上午11:12:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class SHotelIdFilterService implements IFilterService {

	private static final Logger logger = Logger.getLogger("IncrInventoryLogger");

	/** 
	 * 本地缓存文件中过滤SHotelIds	
	 *
	 * Set<String> CommonRepository.java filteredSHotelIds
	 */
	private Map<String, Boolean> filteredSHotelIds = new ConcurrentHashMap<String, Boolean>();

	/** 
	 * 上次本地缓存更新时间
	 *
	 * long SHotelIdFilterService.java lastChangeTime
	 */
	private long lastChangeTime = 0l;

	/** 
	 * 判断是否过滤掉
	 *
	 * @param params
	 * @return 
	 *
	 * @see com.elong.nb.service.IFilterService#doFilter(java.util.Map)    
	 */
	@Override
	public boolean doFilter(String hotelCode) {
		if (StringUtils.isEmpty(hotelCode))
			return false;
		// 本地缓存、5分钟刷新本地缓存
		if (!filteredSHotelIds.containsKey(hotelCode) || (System.currentTimeMillis() - lastChangeTime) >= 5 * 60 * 1000) {
			boolean isFilter = checkHotelCode(hotelCode);
			filteredSHotelIds.put(hotelCode, isFilter);
			return isFilter;
		}
		return filteredSHotelIds.get(hotelCode);
	}

	/** 
	 * 检查hotelCode是否触发CtripQunarRule规则过滤
	 *
	 * @param hotelCode
	 * @return
	 */
	private boolean checkHotelCode(String hotelCode) {
		RequestBase<HotelCodeRuleRealRequest> requestBase = new RequestBase<HotelCodeRuleRealRequest>();
		requestBase.setFrom("nb_job_incr");
		requestBase.setLogId(UUID.randomUUID().toString());

		HotelCodeRuleRealRequest realRequest = new HotelCodeRuleRealRequest();
		Map<String, String> hotelCodeRule = new HashMap<String, String>();
		hotelCodeRule.put(hotelCode, "CtripQunarRule");
		realRequest.setHotelCodeRule(hotelCodeRule);
		requestBase.setRealRequest(realRequest);
		String reqData = JSON.toJSONString(requestBase);

		NbapiHttpRequest nbapiHttpRequest = new NbapiHttpRequest();
		String ruleUrl = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetHitHotelCodeUrl");
		ruleUrl = StringUtils.isEmpty(ruleUrl) ? "http://192.168.233.40:9014/api/Hotel/GetHitHotelCode" : ruleUrl;
		nbapiHttpRequest.setUrl(ruleUrl);
		nbapiHttpRequest.setParamStr(reqData);
		String result = null;
		try {
			result = HttpClientUtil.httpJsonPost(nbapiHttpRequest);
		} catch (Exception e) {
			logger.error("SHotelIdFilterService,httpPost error = " + e.getMessage(), e);
		}
		if (StringUtils.isEmpty(result))
			return false;

		ResponseBase<?> responseBase = JSON.parseObject(result, ResponseBase.class);
		if (!StringUtils.equals("0", responseBase.getResponseCode()))
			return false;

		JSONObject jsonObj = (JSONObject) responseBase.getRealResponse();
		HotelCodeRuleRealResponse realResponse = JSONObject.parseObject(jsonObj.toJSONString(), HotelCodeRuleRealResponse.class);
		if (realResponse == null || realResponse.getResultMap() == null || realResponse.getResultMap().size() == 0)
			return false;

		return true;
	}

}
