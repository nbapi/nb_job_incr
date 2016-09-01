package com.elong.hotel.task.ordertask;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;

import com.elong.common.http.HttpMethodUtil;
import com.elong.common.http.HttpUtil;
import com.elong.common.http.model.HttpResult;
import com.elong.hotel.schedule.entity.TaskResult;

public class ElongOrderNewTask {
  public TaskResult execute(String param) throws ClientProtocolException, IOException {
    TaskResult r = new TaskResult();
    try {
      String[] params = param.split("\\|");
      URL url = new URL(params[0].trim());
      HttpUtil http = HttpUtil.getInstance(url.getHost(), url.getPort(), url.getProtocol());
      http.setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
      http.setMethod(HttpMethodUtil.getGetMethod(url.getFile()));
      try {
        http.setConnectionTimeout(params.length > 1 && StringUtils.isNotBlank(params[1]) ? Integer
            .parseInt(params[1].trim()) : 30000);
      } catch (NumberFormatException e) {
        http.setConnectionTimeout(30000);
      }
      HttpResult result = http.getHttpResult();
      setInfo(r, result);
    } catch (Exception e) {
      e.printStackTrace();
      r.setCode(-1);
      r.setMessage("ElongOrderTask逻辑异常:" + e.getMessage());
    }
    return r;
  }

  private void setInfo(TaskResult tr, HttpResult result) {
    tr.setCode(result.getStatusCode() - 200);
    tr.setMessage(result.isResultOk() ? result.getContent() : result.getStatusCode()
        + result.getErr().getMessage());
  }

}
