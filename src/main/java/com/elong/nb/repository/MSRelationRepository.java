/**   
 * @(#)M_SRelationRepository.java	2016年9月9日	下午6:01:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.elong.nb.ms.agent.HotelDataServiceAgent;

/**
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月9日 下午6:01:27   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class MSRelationRepository {

	/** 
	 * 获取sHotelID对应的mHotelID，保证都是有效的
	 *
	 * @param sHotelID
	 * @return
	 */
	public String getValidMHotelId(String sHotelID) {
		Map<String, String> resultMap = HotelDataServiceAgent.getMhotelIdByShotelId(new String[] { sHotelID });
		if (resultMap == null)
			return null;
		String mHotelID = resultMap.get(sHotelID);
		return StringUtils.isEmpty(mHotelID) ? null : mHotelID;
	}

}
