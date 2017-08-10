/**   
 * @(#)SyncIncrToDBJob.java	2016年9月8日	下午6:00:43	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.job;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import com.alibaba.fastjson.JSON;
import com.elong.hotel.schedule.entity.TaskResult;
import com.elong.nb.common.model.NbapiHttpRequest;
import com.elong.nb.common.util.HttpClientUtil;
import com.elong.nb.model.ResponseResult;

/**
 * 增量同步Job
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月8日 下午6:00:43   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class SyncIncrToDBJob {

	/** 
	 * job执行入口
	 *
	 * @param param
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public TaskResult execute(String param) throws ClientProtocolException, IOException {
		TaskResult r = new TaskResult();
		try {
			NbapiHttpRequest nbapiHttpRequest = new NbapiHttpRequest();
			nbapiHttpRequest.setUrl(param);
			String result = HttpClientUtil.httpGet(nbapiHttpRequest);
			ResponseResult response = JSON.parseObject(result, ResponseResult.class);
			if (response != null) {
				if (response.getCode() == ResponseResult.SUCCESS) {
					r.setCode(0);
					r.setMessage(response.getMessage());
				} else {
					r.setCode(-1);
					r.setMessage("SyncIncrToDBJob fail..." + response.getMessage());
				}
			} else {
				r.setCode(-1);
				r.setMessage("SyncIncrToDBJob fail...,Response is null...");
			}

		} catch (Exception e) {
			r.setCode(-1);
			r.setMessage("SyncIncrToDBJob error:" + e.getMessage());
		}
		return r;
	}

	public static void main(String[] args) {
		SyncIncrToDBJob obj = new SyncIncrToDBJob();
		try {
			obj.execute("http://www.baidu.com");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
