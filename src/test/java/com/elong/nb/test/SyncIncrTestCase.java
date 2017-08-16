package com.elong.nb.test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.FileUtils;
import org.junit.Test;

import com.alibaba.fastjson.JSON;

public class SyncIncrTestCase {

	@Test
	public void testSyncHotelToDB() throws Exception {

	}

	public void buchangMessage() throws Exception {
		List<String> list = FileUtils.readLines(new File("/Users/user/git/nb_job_incr/src/test/java/com/elong/nb/test/data.txt"));
		for (String str : list) {
			String[] array = StringUtils.split(str, "|");
			String orderTimestamp = array[0];
			int orderId = Integer.valueOf(array[1]);
			String status = array[2];
			String checkInDate = array[3];
			String checkOutDate = array[4];
			int roomCount = Integer.valueOf(array[5]);

			String message = buildMessage(orderId, status, orderTimestamp, checkInDate, checkOutDate, roomCount);
			sendRequest(message);
		}
	}

	private void sendRequest(String message) {
		// String reqUrl = "http://nbapi-syncincr.vip.elong.com/writeIncrOrderLog";
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("message", message);
			// String result = HttpClientUtils.postRequest(reqUrl, params);
			// System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String buildMessage(int orderId, String status, String orderTimestamp, String checkInDate, String checkOutDate, int roomCount) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("orderId", orderId);
		params.put("status", status);
		params.put("orderTimestamp", orderTimestamp);
		params.put("checkInDate", checkInDate);
		params.put("checkOutDate", checkOutDate);
		params.put("roomCount", roomCount);
		return JSON.toJSONString(params);
	}

}
