package com.elong.nb.dao.adataper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.elong.hotel.searchagent.thrift.dss.GetInvAndInstantConfirmResponse;
import com.elong.hotel.searchagent.thrift.dss.InvDetail;
import com.elong.hotel.searchagent.thrift.dss.MhotelDetail;
import com.elong.hotel.searchagent.thrift.dss.ShotelDetail;
import com.elong.hotel.searchagent.thrift.dss.SroomDetail;
import com.elong.nb.common.util.SafeConvertUtils;
import com.elong.nb.model.bean.IncrInventory;

public class IncrInventoryAdapter {
	
	private static final Logger logger = Logger.getLogger("CheckCreateTableLogger");

	public Map<String, List<IncrInventory>> toNBObect(GetInvAndInstantConfirmResponse response) {
		Map<String, List<IncrInventory>> incrInventoryMap = new HashMap<String, List<IncrInventory>>();
		if (response != null) {
			if (response.getReturn_code() == 0) {
				if (response.getMhotel_detail() != null && response.getMhotel_detail().size() > 0) {
					for (MhotelDetail mhotel : response.getMhotel_detail()) {
						if (mhotel != null && mhotel.getShotel_detail() != null && mhotel.getShotel_detail().size() > 0) {
							for (ShotelDetail shotel : mhotel.getShotel_detail()) {
								if (shotel != null && shotel.getSroom_detail() != null && shotel.getSroom_detail().size() > 0) {
									for (SroomDetail sroom : shotel.getSroom_detail()) {
										String hotelCode = SafeConvertUtils.ToHotelId(shotel.shotel_id);
										String roomTypeID = SafeConvertUtils.ToRoomId(sroom.sroom_id);
										String key = hotelCode + "|" + roomTypeID;
										List<IncrInventory> incrInventorys = incrInventoryMap.get(key);
										if (incrInventorys == null) {
											incrInventorys = new ArrayList<IncrInventory>();
										}
										if (sroom != null && sroom.getInv_detail() != null && sroom.getInv_detail().size() > 0) {
											for (InvDetail item : sroom.getInv_detail()) {
												IncrInventory inv = new IncrInventory();
												inv.setHotelID(SafeConvertUtils.ToHotelId(mhotel.getMhotel_id()));
												inv.setStartDate(new Date(item.getBegin_date()));
												inv.setEndDate(new Date(item.getEnd_date()));
												inv.setStartTime(item.getBegin_time());
												inv.setEndTime(item.getEnd_time());
												inv.setAvailableAmount(item.getAvailable_amount());
												inv.setAvailableDate(new Date(item.getAvailable_date()));
												inv.setRoomTypeID(roomTypeID);
												inv.setStatus(item.getStatus() == 0);// 0:有效
												inv.setOverBooking(item.getIs_over_booking());// 0:可超售 1:不可超售
												inv.setHotelCode(hotelCode);
												inv.setIsInstantConfirm(item.instant_confirm);// 0:立即确认 1:非立即确认
												String icBeginTime = item.getIc_begin_time();
												String icEndTime = item.getIc_end_time();
												// 不是立即确认情况下的兼容
												if (!item.instant_confirm) {
													if (inv.isStatus()) {
														if (StringUtils.isEmpty(icBeginTime)) {
															icBeginTime = "00:00";
														}
														if (StringUtils.isEmpty(icEndTime)) {
															icEndTime = "23:59";
														}
													} else {
														if (StringUtils.isEmpty(icBeginTime)) {
															icBeginTime = "23:59";
														}
														if (StringUtils.isEmpty(icEndTime)) {
															icEndTime = "00:00";
														}
													}
												}
												inv.setIC_BeginTime(icBeginTime);
												inv.setIC_EndTime(icEndTime);
												convertInventory(inv);
												incrInventorys.add(inv);
												if(hotelCode.equals("91512416")&&roomTypeID.equals("0019")){
													logger.info("item = " + JSON.toJSONString(item));
													logger.info("nbinv = " + JSON.toJSONString(inv));
												}
											}
										}
										incrInventoryMap.put(key, incrInventorys);
									}
								}
							}
						}
					}
				}
			}
		}
		return incrInventoryMap;
	}

	// 超售状态转换
	private void convertInventory(IncrInventory inv) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(1970, 0, 1, 0, 0, 0);
		Date minDate = calendar.getTime();
		if (inv.getAvailableAmount() > 3) {
			inv.setAvailableAmount(3);
			inv.setOverBooking(0);
		}
		if (inv.getEndDate().getTime() < minDate.getTime()) {
			inv.setEndDate(minDate);
		}
		if (inv.getStartDate().getTime() < minDate.getTime()) {
			inv.setStartDate(minDate);
		}
	}

}
