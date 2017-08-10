package com.elong.nb.test;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class SyncIncrTestCase {

	@Test
	public void testSyncHotelToDB() throws Exception {
		Date date = new Date();

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -1);
		calendar.set(Calendar.SECOND, 0);
		System.out.println(calendar.getTime());

		calendar.set(Calendar.SECOND, 59);
		System.out.println(calendar.getTime());
	}

}
