/**   
 * @(#)IncrHotelDao.java	2016年8月19日	上午10:41:27	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import java.util.List;
import java.util.Map;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.bean.IncrHotel;

/**
 * 酒店增量数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年8月19日 下午2:26:21   user     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@DataSource("dataSource_nbhotelincr")
public interface IncrHotelDao {

	/** 
	 * 删除过期IncrHotel
	 *
	 * @param expireDate
	 * @param limit
	 * @return 返回删除数量
	 */
	public int DeleteExpireIncrData(Map<String,Object> params);

	/** 
	 * 获取trigger的最后一条IncrHotel
	 *
	 * @param trigger
	 * @return
	 */
	public IncrHotel GetLastHotel(String trigger);
	
	/** 
	 * 批量插入IncrHotel
	 *
	 * @param incrHotelList
	 */
	public int BulkInsert(List<IncrHotel> incrHotelList);
	

}
