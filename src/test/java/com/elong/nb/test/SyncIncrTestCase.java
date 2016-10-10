package com.elong.nb.test;

import org.apache.http.impl.cookie.DateUtils;
import org.joda.time.DateTime;
import org.junit.Test;

public class SyncIncrTestCase {

	@Test
	public void testSyncHotelToDB() throws Exception {
		// SyncHotelToDB,writeIncrOrderLog,SyncInventoryToDB,SyncRatesToDB,SyncStateToDB
//		String reqUrl = null;
		// reqUrl = "http://nbapi-syncincr.vip.elong.com/SyncHotelToDB";
		// reqUrl = "http://nbapi-syncincr.vip.elong.com/writeIncrOrderLog";
		// reqUrl = "http://nbapi-syncincr.vip.elong.com/SyncInventoryToDB";
//		reqUrl = "http://nbapi-syncincr.vip.elong.com/SyncRatesToDB";
//		 reqUrl = "http://nbapi-syncincr.vip.elong.com/SyncStateToDB";
//		try {
//			String result = HttpClientUtils.httpPost(reqUrl, "", "application/json");
//			System.out.println(result);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
        String dateValue = "1476096000099";//"2016-10-10T18:40:00.0998750+08:00";
        System.out.println(DateTime.parse(dateValue).toDate().getTime());
	}
}
