/**   
 * @(#)SetInfoController.java	2017年3月15日	上午10:44:18	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.elong.nb.common.model.RedisKeyConst;
import com.elong.nb.service.IIncrSetInfoService;

/**
 * 修改IncrSetInfo数据入口啊
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年3月15日 上午10:44:18   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Controller
public class SetInfoController {

	@Resource
	private IIncrSetInfoService incrSetInfoService;

	/** 
	 * 手动更新Incr.Inventory.LastID入口，为了追库存增量
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/updateIncrInventoryLastID", method = RequestMethod.GET)
	public @ResponseBody String updateIncrInventoryLastID(HttpServletRequest request) {
		String lastIDStr = request.getParameter("lastID");
		if (StringUtils.isEmpty(lastIDStr))
			return "lastID must not be null or empty.\n";
		long newLastChgID;
		try {
			newLastChgID = Long.valueOf(lastIDStr);
		} catch (NumberFormatException e) {
			return e.getMessage() + "\n";
		}
		if (newLastChgID == 0l) {
			return "lastID must not be 0l.\n";
		}
		incrSetInfoService.put(RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey(), newLastChgID);
		String setValue = incrSetInfoService.get(RedisKeyConst.CacheKey_KEY_Inventory_LastID.getKey());
		return "updateIncrInventoryLastID successfully,newLastChgID = " + 0 + "\n" + ",getValue = " + setValue;
	}

}
