/**   
 * @(#)XsdDateTimeConverter.java	2016年6月16日	下午7:27:39	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.util;

import org.joda.time.DateTime;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年6月16日 下午7:27:39   zhangyang.zhu     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		zhangyang.zhu  
 * @version		1.0  
 * @since		JDK1.7
 */
public class XsdDateTimeConverter {

    /** 
     *
     * @param dateTime
     * @return
     */
    public static DateTime unmarshal(String dateTime) {
        return new DateTime(dateTime);
    }

    /** 
     *
     * @param dateTime
     * @return
     */
    public static String marshal(DateTime dateTime) {
        return dateTime.toString();
    }
}
