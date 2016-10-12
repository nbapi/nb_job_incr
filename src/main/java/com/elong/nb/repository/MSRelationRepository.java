/**   
 * @(#)M_SRelationRepository.java	2016年9月9日	下午6:01:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.elong.nb.agent.NorthBoundForAPIService.INorthBoundForAPIService;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.consts.RedisKeyConst;
import com.elong.nb.dao.SqlServerDataDao;
import com.elong.nb.model.NBMSRelation;

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

	@Resource
	private INorthBoundForAPIService northBoundForAPIService;

	@Resource
	private SqlServerDataDao sqlServerDataDao;

	/** 
	 * 获取sHotelID对应的mHotelID
	 *
	 * @param sHotelID
	 * @return
	 */
	public String getMHotelId(String sHotelID) {
		String res = redisManager.hashGet(RedisKeyConst.KEY_ID_S_M_CacheKey, sHotelID);
		if (StringUtils.isEmpty(res)) {
			if (!redisManager.exists(RedisKeyConst.KEY_ID_S_M_CacheKey)) {
				return sHotelID;
			}
		}
		if (StringUtils.isEmpty(res)) {
			res = sHotelID;
		}
		return res;
	}

	/** 
	 * 缓存去掉已关闭的酒店关联，M酒店和S酒店
	 *
	 * @param mhotelId
	 */
	public void resetHotelMSCache(String mhotelId) {
		List<NBMSRelation> relatioins = sqlServerDataDao.getMSRelationData(mhotelId);

		// region 去掉已关闭的酒店关联，M酒店和S酒店
		List<NBMSRelation> noClosedHotel = new ArrayList<NBMSRelation>();
		if (relatioins != null && relatioins.size() > 0) {
			for (NBMSRelation ms : relatioins) {
				// 注意：在这里1代表酒店已关闭
				if (ms.getMStatus() == "1" || ms.getSStatus() == "1") {
					redisManager.hdel(RedisKeyConst.KEY_ID_S_M_CacheKey, ms.getSHotelID());
					// m对应s的关系也要清掉
					redisManager.hdel(RedisKeyConst.KEY_ID_M_S_CacheKey, ms.getMHotelID());
					continue;
				}
				noClosedHotel.add(ms);
			}
		}
		relatioins = noClosedHotel;

		if (relatioins != null && relatioins.size() > 0) {
			// region 新映射
			Map<String, List<NBMSRelation>> map = new HashMap<String, List<NBMSRelation>>();
			for (NBMSRelation ms : relatioins) {
				if (ms == null)
					continue;
				redisManager.hashPut(RedisKeyConst.KEY_Hotel_S_M_CacheKey, ms.getSHotelID(), JSON.toJSONString(ms));

				if (ms == null || !StringUtils.equals("0", ms.getSStatus()))
					continue;
				List<NBMSRelation> valList = map.get(ms.getMHotelID());
				if (valList == null) {
					valList = new ArrayList<NBMSRelation>();
				}
				valList.add(ms);
				map.put(ms.getMHotelID(), valList);
			}

			for (Map.Entry<String, List<NBMSRelation>> entry : map.entrySet()) {
				redisManager.hashPut(RedisKeyConst.KEY_Hotel_M_S_CacheKey, entry.getKey(), JSON.toJSONString(entry.getValue()));
			}
		}
	}

}
