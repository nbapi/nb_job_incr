/**   
 * @(#)IJob.java	2016年6月3日	下午2:48:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.job;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import com.elong.hotel.schedule.entity.TaskResult;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年6月3日 下午2:48:27   Administrator     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		Administrator  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IJob {

	public TaskResult execute(String param) throws ClientProtocolException, IOException;

}
