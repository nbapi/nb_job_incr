package com.elong.nb.test;

import java.text.ParseException;
import java.util.Calendar;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.elong.nb.util.DateUtils;

public class SyncIncrTestCase {

	@Test
	public void testSyncHotelToDB() throws Exception {
		// SyncHotelToDB,writeIncrOrderLog,SyncInventoryToDB,SyncRatesToDB,SyncStateToDB
		// String reqUrl = null;
		// reqUrl = "http://localhost:8080/nb_job_incr/SyncHotelToDB";
		// reqUrl = "http://localhost:8080/nb_job_incr/writeIncrOrderLog";
		// reqUrl = "http://localhost:8080/nb_job_incr/SyncInventoryToDB";
		// reqUrl = "http://localhost:8080/nb_job_incr/SyncRatesToDB";
		// reqUrl = "http://localhost:8080/nb_job_incr/SyncStateToDB";
		// try {
		// String result = HttpClientUtils.httpPost(reqUrl, "", "application/json");
		// System.out.println(result);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		String orderTimestamp = "2016-09-30 16:49:32.837";
		
		try {
			System.out.println(org.apache.commons.lang.time.DateUtils.parseDate(orderTimestamp, new String[] { "yyyy-MM-dd HH:mm:ss:SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS", }));
		} catch (ParseException e) {
			System.out.println("orderTimestamp is error format,not be ['yyyy-MM-dd HH:mm:ss:SSS','yyyy-MM-dd HH:mm:ss']");
		}
		
	}
}
