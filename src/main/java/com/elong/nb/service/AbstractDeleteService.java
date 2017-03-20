/**   
 * @(#)AbstractDeleteService.java	2017年3月15日	下午3:29:35	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.elong.nb.common.util.CommonsUtil;
import com.elong.nb.util.DateHandlerUtils;

/**
 * 删除过期增量数据接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年3月15日 下午3:29:35   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public abstract class AbstractDeleteService {

	/** 
	 * 删除过期增量数据(查询待删数据主键,通过主键in方式批量删除。每次最多2000条循环。每次循环间隔2秒)
	 *
	 * @param expireDate
	 * @return
	 */
	public int deleteExpireIncrData(Date expireDate) {
		if (expireDate == null) {
			throw new IllegalArgumentException("DeleteExpireIncrData,the paramter 'expireDate' must not be null.");
		}
		String formatDateStr = DateHandlerUtils.formatDate(expireDate, "yyyy-MM-dd HH:mm:ss");
		logger("deleteExpireIncrData begin to execute,expireDate = " + formatDateStr);
		long startTime = System.currentTimeMillis();
		// 查询待删数据主键,每批次最多2000条
		int maxRecordCount = 2000;
		String maxRecordCountStr = CommonsUtil.CONFIG_PROVIDAR.getProperty("GetIncrIdMaxRecordCount");
		if (StringUtils.isNotEmpty(maxRecordCountStr)) {
			try {
				maxRecordCount = Integer.valueOf(maxRecordCountStr.trim());
			} catch (NumberFormatException e) {
				logger("deleteExpireIncrData,the config item'GetIncrIdMaxRecordCount' must be an Integer type.");
				maxRecordCount = 2000;
			}
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("changeTime", expireDate);
		params.put("maxRecordCount", maxRecordCount);
		List<BigInteger> incrIdList = getIncrIdList(params);
		int successCount = CollectionUtils.isEmpty(incrIdList) ? 0 : incrIdList.size();
		// 待删条件数据存在，并且执行时间未超过10分钟(防止执行时间过长job自动停)，则继续删除
		while (incrIdList != null && incrIdList.size() > 0 && DateHandlerUtils.withinTenMinutes(startTime)) {
			long delStartTime = System.currentTimeMillis();
			// 通过主键in方式批量删除,每批次最多2000条
			int count = deleteByIncrIdList(incrIdList);
			logger("use time = " + (System.currentTimeMillis() - delStartTime) + ",deleteExpireIncrData successfully,count = " + count
					+ ",expireDate = " + formatDateStr);
			successCount += count;
			// sleep一秒再继续，防止数据库压力太大
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
			}
			// 查询待删数据主键,每批次最多2000条
			incrIdList = getIncrIdList(params);
		}
		logger("use time = " + (System.currentTimeMillis() - startTime) + ",deleteExpireIncrData successfully,successCount = "
				+ successCount + ",expireDate = " + formatDateStr);
		return successCount;
	}

	/** 
	 * 查询待删数据主键,每批次最多2000条
	 *
	 * @param params
	 * @return
	 */
	protected abstract List<BigInteger> getIncrIdList(Map<String, Object> params);

	/** 
	 * 通过主键in方式批量删除,每批次最多2000条
	 *
	 * @param incrIdList
	 * @return
	 */
	protected abstract int deleteByIncrIdList(List<BigInteger> incrIdList);

	/** 
	 * 自定义日志，为了不同增量日志打印到各自日志文件
	 *
	 * @param message
	 */
	protected abstract void logger(String message);

}
