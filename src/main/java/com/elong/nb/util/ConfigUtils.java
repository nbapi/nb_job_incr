/**   
 * @(#)ConfigUtils.java	2017年6月12日	下午3:38:33	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.util;

import org.apache.commons.lang.StringUtils;

import com.elong.nb.common.util.CommonsUtil;

/**
 * 读取config配置文件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年6月12日 下午3:38:33   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class ConfigUtils {

	public static int getIntConfigValue(String configKey, int defaultValue) {
		String configValue = CommonsUtil.CONFIG_PROVIDAR.getProperty(configKey);
		return StringUtils.isEmpty(configValue) ? defaultValue : Integer.valueOf(configValue);
	}

	public static String getStringConfigValue(String configKey, String defaultValue) {
		String configValue = CommonsUtil.CONFIG_PROVIDAR.getProperty(configKey);
		return StringUtils.isEmpty(configValue) ? defaultValue : configValue;
	}

}
