package com.elong.hotel.commercial.task;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.ClientProtocolException;

import com.elong.common.http.HttpMethodUtil;
import com.elong.common.http.HttpUtil;
import com.elong.common.http.model.HttpResult;
import com.elong.hotel.schedule.entity.TaskResult;

public class Synchronizer {
     
	public TaskResult execute(String param) throws ClientProtocolException, IOException {
	    TaskResult r = new TaskResult();
	    try { 
	      URL url = new URL(param);
	      HttpUtil http = HttpUtil.getInstance(url.getHost(), url.getPort(), "http");
	      http.setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
	      http.setMethod(HttpMethodUtil.getGetMethod(url.getFile()));
	      http.setConnectionTimeout(200000);
	      HttpResult result = http.getHttpResult();
	      setInfo(r, result);
	    } catch (Exception e) {
	    
	      r.setCode(-1);
	      r.setMessage("ElongOrderTask error:" + e.getMessage());
	    }
		System.out.println("______________"+r.getMessage());
	    return r;
	  }

	  private void setInfo(TaskResult tr, HttpResult result) {
	    tr.setCode(result.getStatusCode() - 200);
	    tr.setMessage(result.isResultOk() ? result.getContent() : result.getStatusCode()
	        + result.getErr().getMessage());
	  }
}
