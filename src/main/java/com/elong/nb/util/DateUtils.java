/**   
 * @(#)DateUtils.java	2016年9月20日	下午5:03:37	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.util;

import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月20日 下午5:03:37   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class DateUtils {

	/**   
	 * 得到dBExpireDate的值   
	 *   
	 * @return dBExpireDate的值
	 */
	public static Date getDBExpireDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR, -30);
		return calendar.getTime();
	}

	/**   
	 * 得到cacheExpireDate的值   
	 *   
	 * @return cacheExpireDate的值
	 */
	public static Date getCacheExpireDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, -30);
		return calendar.getTime();
	}

}
