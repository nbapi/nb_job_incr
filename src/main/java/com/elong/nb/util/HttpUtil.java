/**   
 * @(#)HttpUtil.java	2016年6月16日	下午7:28:59	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.elong.nb.model.ResponseResult;
import com.google.gson.Gson;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年6月16日 下午7:28:59   zhangyang.zhu     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		zhangyang.zhu  
 * @version		1.0  
 * @since		JDK1.7
 */
public class HttpUtil {

	protected static Logger logger = LogManager.getLogger(HttpUtil.class);
	private static Gson gson = new Gson();

	/** 
	 *
	 * @param reqUrl
	 * @param reqData
	 * @return
	 * @throws Exception
	 */
	public static String httpPost(String reqUrl, String reqData) throws Exception {
		HttpURLConnection conn = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(reqUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoInput(true);// 设置是否从httpUrlConnection读入，默认情况下是true;
			conn.setDoOutput(true);
			conn.setUseCaches(false); // Post 请求不能使用缓存
			conn.setConnectTimeout(8 * 1000);
			conn.setReadTimeout(30 * 1000);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.connect();
			// DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写道流里面
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.write(reqData.getBytes("UTF-8"));
			out.flush();
			out.close();
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String lines;
			StringBuilder sb = new StringBuilder();
			while ((lines = reader.readLine()) != null)
				sb.append(lines);
			return sb.toString();
		} catch (Exception ex) {
			logger.error("http Error,reqUrl:" + reqUrl + ",Exception:" + ex.getMessage());
			throw ex;

		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}
			}
			if (null != conn)
				try {
					conn.disconnect();
				} catch (Exception ex) {
					logger.error("method:httpPost,使用finally块来关闭输入流,Exception:" + ex.getMessage());
					throw ex;
				}
		}
	}

	/** 
	 *
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static ResponseResult httpGet(String url) throws Exception {
		ResponseResult result = new ResponseResult();
		BufferedReader in = null;
		try {
			String urlNameString = url;
			URL realUrl = new URL(urlNameString);
			// 打开和URL之间的连接
			HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			connection.connect();
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			String line;
			if ((line = in.readLine()) != null) {
				buffer.append(line);
			}
			result = gson.fromJson(buffer.toString(), ResponseResult.class);
		} catch (Exception e) {
			result.setCode(-1);
			result.setMessage(e.getMessage());
			logger.error("method:httpGet,发送GET请求出现异常！URL：" + url + ",Exception:" + e.getMessage());
			throw e;
		}
		// 使用finally块来关闭输入流
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				result.setCode(-1);
				result.setMessage(e2.getMessage());
				logger.error("method:httpGet 使用finally块来关闭输入流,Exception:" + e2.getMessage());
				throw e2;
			}
		}
		return result;
	}

	/** 
	 *
	 * @param reqUrl
	 * @return
	 * @throws Exception
	 */
	public static String httpGetData(String reqUrl) throws Exception {
		HttpURLConnection conn = null;
		BufferedReader in = null;
		logger.info("[HTTPGET]开始访问" + reqUrl);
		try {
			long start = System.currentTimeMillis();
			URL url = new URL(reqUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);// 设置是否从httpUrlConnection读入，默认情况下是true;
			conn.setDoOutput(false);
			conn.setConnectTimeout(8 * 1000);
			conn.setReadTimeout(60 * 1000);
			conn.connect();
			int code = conn.getResponseCode();
			if (HttpURLConnection.HTTP_OK != code) {
				logger.info("http访问错误:" + reqUrl);
				throw new RuntimeException("http访问错误,返回码：" + code);
			}
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			String line;
			if ((line = in.readLine()) != null) {
				buffer.append(line);
			}
			String result = buffer.toString();
			// String result = StreamUtils.copyToString(conn.getInputStream(),Charset.forName("UTF-8"));
			long end = System.currentTimeMillis();
			logger.info("[HTTPGET]访问结果" + ",time:" + (end - start));
			return result;
		} catch (Exception ex) {
			logger.info("http访问错误:" + reqUrl);
			logger.info("[HTTPGET]访问出错:" + ex);
			ex.printStackTrace();
			throw ex;
		} finally {
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}
			}
			if (null != conn)
				try {
					conn.disconnect();
				} catch (Exception ex) {
				}
		}
	}
}
