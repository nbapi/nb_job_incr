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

import com.elong.nb.service.IIncrSetInfoService;
import com.elong.nb.submeter.service.IImpulseSenderService;

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

	@Resource
	private IImpulseSenderService impulseSenderService;

	@Resource
	private IIncrSetInfoService incrSetInfoService;


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

}
