/**   
 * @(#)SqlServerDataDao.java	2016年9月13日	下午3:04:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;
import java.util.Map;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.NBMSRelation;

/**
 * PriceInfo_track数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月13日 下午3:04:27   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface SqlServerDataDao {

	/** 
	 * 查询增量价格待同步数据
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_14")
	public List<Map<String, Object>> getDataFromPriceInfoTrack(Map<String, Object> params);

	/** 
	 * 查询增量订单待同步数据
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_14")
	public List<Map<String, Object>> getDataFromReserveTrack(Map<String, Object> params);

	/** 
	 * hotelId状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getHotelIdCount(Map<String, Object> params);

	/** 
	 * HotelCode状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getHotelCodeCount(Map<String, Object> params);

	/** 
	 * RoomId状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getRoomIdCount(Map<String, Object> params);

	/** 
	 * RoomTypeId状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getRoomTypeIdCount(Map<String, Object> params);

	/** 
	 * RatePlanId状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getRatePlanIdCount(Map<String, Object> params);

	/** 
	 * RatePlanPolicy状态增量记录数
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public int getRatePlanPolicyCount(Map<String, Object> params);

	/** 
	 * hotelId状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getHotelIdData(Map<String, Object> params);

	/** 
	 * hotelCode状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getHotelCodeData(Map<String, Object> params);

	/** 
	 * RoomId状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getRoomIdData(Map<String, Object> params);

	/** 
	 * RoomTypeId状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getRoomTypeIdData(Map<String, Object> params);

	/** 
	 * RatePlanId状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getRatePlanIdData(Map<String, Object> params);

	/** 
	 * RatePlanPolicy状态增量
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<Map<String, Object>> getRatePlanPolicyData(Map<String, Object> params);
	
	/** 
	 * 指定mhotelId的所有ms关系数据
	 *
	 * @param params
	 * @return
	 */
	@DataSource("dataSource_sqlserver_13")
	public List<NBMSRelation> getMSRelationData(Map<String, Object> params);

}
