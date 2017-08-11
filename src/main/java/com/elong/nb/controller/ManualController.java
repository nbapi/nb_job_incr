/**   
 * @(#)ManualController.java	2017年4月21日	下午5:30:18	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.cache.ICacheKey;
import com.elong.nb.cache.RedisManager;
import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.IImpulseSenderService;
import com.elong.nb.submeter.service.impl.SubmeterTableCache;

/**
 * 手动修改某些值
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 下午5:30:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class ManualController {

	private RedisManager redisManager = RedisManager.getInstance("redis_job", "redis_job");

	@Resource
	private IImpulseSenderService impulseSenderService;

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	@Resource
	private SubmeterTableCache submeterTableCache;

	/** 
	 * 获取当前使用非空表名集合
	 *
	 * @param tablePrefix
	 * @return
	 */
	@RequestMapping(value = "/test/submeterTableCache/{tablePrefix}")
	public @ResponseBody String getSubmeterTableNames(@PathVariable("tablePrefix") String tablePrefix) {
		return JSON.toJSONString(submeterTableCache.queryNoEmptySubTableList(tablePrefix, false));
	}

	/** 
	 * 设置分表开始序号(分表上线时初始化分表开始序号)
	 *
	 * @param tablePrefix
	 * @param subTableNumber (比如分表序号要从100开始，则subTableNumber设置为99)
	 * @return
	 */
	@RequestMapping(value = "/test/putSubTableNumber/{tablePrefix}/{subTableNumber}")
	public @ResponseBody String putSubTableNumber(@PathVariable("tablePrefix") String tablePrefix,
			@PathVariable("subTableNumber") String subTableNumber) {
		try {
			incrSetInfoService.put(tablePrefix + ".SubTable.Number", Long.valueOf(subTableNumber));
		} catch (Exception e) {
			return "putSubTableNumber error = " + e.getMessage() + ",tablePrefix = " + tablePrefix + ",subTableNumber = " + subTableNumber;
		}
		return "putSubTableNumber success.tablePrefix = " + tablePrefix + ",subTableNumber = " + subTableNumber;
	}

	/** 
	 * 设置增量配置信息
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	@RequestMapping(value = "/test/putIncrSetInfo/{key}/{value}")
	public @ResponseBody String putIncrSetInfo(@PathVariable("key") String key, @PathVariable("value") String value) {
		try {
			incrSetInfoService.put(key, Long.valueOf(value));
		} catch (Exception e) {
			return "putIncrSetInfo error = " + e.getMessage() + ",key = " + key + ",value = " + value;
		}
		return "putIncrSetInfo success.key = " + key + ",value = " + value;
	}

	/** 
	 * 设置发号器id(分表上线时初始化发号器id)
	 *
	 * @param tablePrefix
	 * @param idVal
	 * @return
	 */
	@RequestMapping(value = "/test/initImpulseSender/{tablePrefix}/{idVal}")
	public @ResponseBody String initImpulseSender(@PathVariable("tablePrefix") String tablePrefix, @PathVariable("idVal") String idVal) {
		String key = tablePrefix + "_ID";
		try {
			Long idLong = Long.valueOf(idVal);
			impulseSenderService.putId(tablePrefix + "_ID", idLong);
		} catch (Exception e) {
			return "initImpulseSender error = " + e.getMessage();
		}
		return "initImpulseSender success.key = " + key + ",value = " + idVal;
	}

	/** 
	 * 查看发号器当前id
	 *
	 * @param tablePrefix
	 * @return
	 */
	@RequestMapping(value = "/test/getImpulseSenderID/{tablePrefix}")
	public @ResponseBody String getImpulseSenderID(@PathVariable("tablePrefix") String tablePrefix) {
		String key = tablePrefix + "_ID";
		try {
			long impulseSenderId = impulseSenderService.curId(tablePrefix + "_ID");
			return "getImpulseSenderID success.key = " + key + ",value = " + impulseSenderId;
		} catch (Exception e) {
			return "getImpulseSenderID error = " + e.getMessage() + ",key = " + key;
		}
	}

	/** 
	 * 清除缓存分表表名数据 
	 *
	 * @param tablePrefix
	 * @return
	 */
	@RequestMapping(value = "/test/delSubmeterTableCache/{tablePrefix}")
	public @ResponseBody String delSubmeterTableCache(@PathVariable("tablePrefix") String tablePrefix) {
		ICacheKey cacheKey = RedisManager.getCacheKey(tablePrefix + ".Submeter.TableNames");
		try {
			// 清除老数据
			redisManager.del(cacheKey);
		} catch (Exception e) {
			return "delSubmeterTableCache error = " + e.getMessage() + ",tablePrefix = " + tablePrefix;
		}
		return "delSubmeterTableCache success.tablePrefix = " + tablePrefix;
	}

}
