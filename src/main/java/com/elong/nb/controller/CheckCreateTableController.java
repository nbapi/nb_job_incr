/**   
 * @(#)CheckCreateTableController.java	2017年4月11日	上午10:46:07	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.elong.nb.model.ResponseResult;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.submeter.service.ICheckCreateTableService;

/**
 * 检查数据库空分表是否达到规定数量，不足时建新分表
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月11日 上午10:46:07   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class CheckCreateTableController {

	private static final Logger logger = Logger.getLogger("CheckCreateTableLogger");

	@Resource
	private ICheckCreateTableService checkCreateTableService;

	/** 
	 * 检查并创建分表 
	 *
	 * @param tableNamePrefix
	 * @return
	 */
	@RequestMapping(value = "/checkCreateTable/{tablePrefix}", method = RequestMethod.GET)
	public @ResponseBody String checkCreateTable(@PathVariable("tablePrefix") String tablePrefix) {
		ResponseResult result = new ResponseResult();
		EnumIncrType incrType = null;
		try {
			incrType = EnumIncrType.valueOf(tablePrefix);
		} catch (Exception e1) {
		}
		if (incrType == null) {
			logger.error("EnumIncrType doesn't exists the element['" + tablePrefix + "']");
			result.setCode(ResponseResult.FAILURE);
			result.setMessage("EnumIncrType doesn't exists the element['" + tablePrefix + "']");
			return JSON.toJSONString(result);
		}

		long startTime = System.currentTimeMillis();
		logger.info("checkCreateTable EnumIncrType = " + incrType + ",start");
		List<String> needCreateTableList = null;
		try {
			needCreateTableList = checkCreateTableService.checkSubTable(incrType);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage("checkSubTable error = " + e.getMessage());
			return JSON.toJSONString(result);
		}
		List<String> successTableList = null;
		try {
			successTableList = checkCreateTableService.createSubTable(incrType, needCreateTableList);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			result.setCode(ResponseResult.FAILURE);
			result.setMessage("createSubTable error = " + e.getMessage());
			return JSON.toJSONString(result);
		}
		result.setCode(ResponseResult.SUCCESS);
		result.setMessage(JSON.toJSONString(successTableList));
		logger.info("checkCreateTable EnumIncrType = " + incrType + ",end,and use time = " + (System.currentTimeMillis() - startTime)
				+ "ms");
		return JSON.toJSONString(result);
	}

}
