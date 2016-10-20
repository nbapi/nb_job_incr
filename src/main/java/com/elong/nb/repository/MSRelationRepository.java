/**   
 * @(#)M_SRelationRepository.java	2016年9月9日	下午6:01:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.elong.nb.cache.RedisManager;
import com.elong.nb.common.model.RedisKeyConst;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月9日 下午6:01:27   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class MSRelationRepository {

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	/** 
	 * 获取sHotelID对应的mHotelID
	 *
	 * @param sHotelID
	 * @return
	 */
	public String getMHotelId(String sHotelID) {
		if (!redisManager.exists(RedisKeyConst.CacheKey_KEY_ID_S_M)) {
			return sHotelID;
		}
		String mHotelID = redisManager.hashGet(RedisKeyConst.CacheKey_KEY_ID_S_M, sHotelID);
		return StringUtils.isEmpty(mHotelID) ? sHotelID : mHotelID;
	}

}
