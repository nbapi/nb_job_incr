/**   
 * @(#)NoticeServiceImpl.java	2016年8月9日	下午6:36:52	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service.impl;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.DateUtils;
import org.springframework.stereotype.Service;

import com.elong.nb.service.INoticeService;
import com.elong.nb.util.HttpClientUtils;
import com.elong.springmvc_enhance.utilities.PropertiesHelper;

/**
 * 通知接口实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月9日 下午6:36:52   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		user 
 * @version		1.0  
 * @since		JDK1.7
 */
@Service
public class NoticeServiceImpl implements INoticeService {

	private static final Log logger = LogFactory.getLog(NoticeServiceImpl.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	/** 
	 * 发送通知信息
	 *
	 * @param title 
	 * @param content 
	 *
	 * @see com.elong.nb.hotelmapping.service.INoticeService#sendMessage(java.lang.String,java.lang.String)    
	 */
	@Override
	public void sendMessage(final String title, final String content) {
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					String alertBaseUrl = PropertiesHelper.getEnvProperties("service.alertService", "service").toString();
					String alertName = PropertiesHelper.getEnvProperties("service.alertName", "service").toString();
					String alertUrlParam = getAlertUrlParam(title, content, alertName);
					logger.debug("sendMessage,params = " + alertUrlParam);
					String result = HttpClientUtils.httpPost(alertBaseUrl, alertUrlParam);
					logger.debug("sendMessage,result = " + result);
				} catch (Exception e) {
					logger.error("sendMessage,error = " + e.getMessage(), e);
				}
			}
		});
	}

	/** 
	 * 报警参数
	 *
	 * @param alertName
	 * @param title
	 * @param content
	 * @return
	 */
	private String getAlertUrlParam(String alertName, String title, String content) {
		StringBuilder sb = new StringBuilder();
		sb.append("alertName=").append(alertName);
		sb.append("&alertTitle=").append(title + ",alertTime = " + DateUtils.formatDate(new Date(), "YYYY-MM-DD HH:mm:ss"));
		sb.append("&alertContent=").append(content);
		return sb.toString();
	}
}