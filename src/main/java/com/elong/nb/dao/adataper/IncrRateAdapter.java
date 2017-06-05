package com.elong.nb.dao.adataper;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.elong.hotel.goods.ds.thrift.BasePrice;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbResponse;
import com.elong.hotel.goods.ds.thrift.MHotelBasePrice;
import com.elong.hotel.goods.ds.thrift.RatePlanBasePrice;
import com.elong.hotel.goods.ds.thrift.SHotelBasePrice;
import com.elong.hotel.goods.ds.thrift.SRoomBasePrice;
import com.elong.nb.common.util.SafeConvertUtils;

public class IncrRateAdapter{

	public List<Map<String, Object>> toNBObject(GetBasePrice4NbResponse goodsObject) {
		if (goodsObject.getMhotel_base_price() != null) {
			int dataSize = goodsObject.getMhotel_base_price().size();
			if (dataSize > 0) {
				List<Map<String, Object>> rates = new LinkedList<Map<String, Object>>();
				for (int i = 0; i < dataSize; i++) {
					MHotelBasePrice mhotelBasePrice = goodsObject.mhotel_base_price.get(i);
					if (mhotelBasePrice != null) {
						String hotelId = SafeConvertUtils.ToHotelId(mhotelBasePrice.getMhotel_id());
						if (mhotelBasePrice.getShotel_base_price() != null) {
							int shotelSize = mhotelBasePrice.getShotel_base_price().size();
							for (int j = 0; j < shotelSize; j++) {
								SHotelBasePrice shotelBasePrice = mhotelBasePrice.getShotel_base_price().get(j);
								String hotelCode = SafeConvertUtils.ToHotelId(shotelBasePrice.getShotel_id());
								if (shotelBasePrice.getSroom_base_price() != null) {
									int roomTypeSize = shotelBasePrice.getSroom_base_price().size();
									for (int k = 0; k < roomTypeSize; k++) {
										SRoomBasePrice roomTypeBasePrice = shotelBasePrice.getSroom_base_price().get(k);
										String roomTypeId = SafeConvertUtils.ToRoomId(roomTypeBasePrice.getSroom_id());
										if (roomTypeBasePrice.getRateplan_base_price() != null) {
											int rpSize = roomTypeBasePrice.getRateplan_base_price().size();
											for (int l = 0; l < rpSize; l++) {
												RatePlanBasePrice rpBasePrice = roomTypeBasePrice.getRateplan_base_price().get(l);
												int ratePlanId = rpBasePrice.getRateplan_id();
												if (rpBasePrice.getBase_price() != null) {
													int baseSize = rpBasePrice.getBase_price().size();
													for (int m = 0; m < baseSize; m++) {
														BasePrice basePrice = rpBasePrice.getBase_price().get(m);
														Map<String, Object> rate = new HashMap<String, Object>();
														rate.put("AddBed", basePrice.allow_add_bed ? new Double(
																basePrice.add_bed_price / 1000.0) : -1d);
														rate.put("CurrencyCode", basePrice.getCurrency_code());
														rate.put("EndDate", new Date(1000 * (long) basePrice.getEnd_date()));
														rate.put("HotelCode", hotelCode);
														rate.put("HotelID", hotelId);
														rate.put("Member", new Double(basePrice.getGeneral_price_origin()) / 1000);
														rate.put("MemberCost", new Double(basePrice.getGeneral_cost_origin()) / 1000);
														rate.put("PriceID", basePrice.getPrice_id());
														rate.put("RateplanID", ratePlanId);
														rate.put("RoomTypeID", roomTypeId);
														rate.put("StartDate", new Date(1000 * (long) basePrice.getStart_date()));
														rate.put("Status", basePrice.getStatus() == 1);
														rate.put("Weekend", new Double(basePrice.getWeekend_price_origin()) / 1000);
														rate.put("WeekendCost", new Double(basePrice.getWeekend_cost_origin()) / 1000);
														rate.put("InsertTime", new Date());
														rate.put("OperateTime", new Date());
														rates.add(rate);
													}
												}
											}
										}
									}
								}
							}
						}
					}

				}
				return rates;
			}
		}
		return null;
	}

}
