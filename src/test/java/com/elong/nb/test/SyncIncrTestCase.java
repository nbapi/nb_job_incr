package com.elong.nb.test;

import org.junit.Test;

import com.elong.nb.model.enums.SubmeterConst;

public class SyncIncrTestCase {

	@Test
	public void testSyncHotelToDB() throws Exception {
		System.out.println(getSelectedSubTableName("IncrHotel", 28662400087l));
	}

	private String getSelectedSubTableName(String tablePrefix,long lastId) {
		int submeterRowCount = SubmeterConst.PER_SUBMETER_ROW_COUNT;
		long tableNumber = (int) Math.ceil(lastId * 1.0 / submeterRowCount);
		return tablePrefix + "_" + tableNumber;
	}

}
