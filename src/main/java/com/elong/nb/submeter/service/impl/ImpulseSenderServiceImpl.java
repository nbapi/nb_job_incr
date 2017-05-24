/**   
 * @(#)ImpulseSenderServiceImpl.java	2017年4月18日	上午11:25:17	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.util.JedisPoolUtil;

/**
 * 发号器接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月18日 上午11:25:17   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class ImpulseSenderServiceImpl implements IImpulseSenderService {

	private static final Logger logger = Logger.getLogger("ImpulseSenderLogger");

	private static final String REDIS_CONFIG = "redis_impulseSender";

	/** 
	 * 获取id
	 *
	 * @return 
	 *
	 * @see com.elong.nb.service.IImpulseSenderService#getId(String)    
	 */
	@Override
	public long getId(String key) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("ImpulseSender getId must not be null parameter['key']");
		}
		long id = JedisPoolUtil.getJedis(REDIS_CONFIG).incr(key);
		logger.info("key = " + key + ",value = " + id);
		return id;
	}

	/** 
	 * 删除key 
	 *
	 * @param key 
	 *
	 * @see com.elong.nb.service.IImpulseSenderService#del(java.lang.String)    
	 */
	@Override
	public void del(String key) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("ImpulseSender getId must not be null parameter['key']");
		}
		JedisPoolUtil.getJedis(REDIS_CONFIG).del(key);
	}

	/** 
	 * 设置id 
	 *
	 * @param key
	 * @param id
	 * @return 
	 *
	 * @see com.elong.nb.service.IImpulseSenderService#putId(java.lang.String, java.lang.Long)    
	 */
	@Override
	public long putId(String key, Long id) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("ImpulseSender putId must not be null parameter['key']");
		}
		if (id == null) {
			throw new IllegalArgumentException("ImpulseSender putId must not be null parameter['id']");
		}
		JedisPoolUtil.getJedis(REDIS_CONFIG).set(key, String.valueOf(id));
		return id;
	}

	/** 
	 * 当前id
	 *
	 * @param key
	 * @return 
	 *
	 * @see com.elong.nb.service.IImpulseSenderService#curId(java.lang.String)    
	 */
	@Override
	public long curId(String key) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("ImpulseSender curId must not be null parameter['key']");
		}
		String idStr = JedisPoolUtil.getJedis(REDIS_CONFIG).get(key);
		return Long.valueOf(idStr);
	}
}
