/**   
 * @(#)IncrSetInfoDao.java	2017年2月17日	下午2:31:16	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.dao;

import com.elong.nb.db.DataSource;
import com.elong.nb.model.IncrSetInfo;

/**
 * 增量配置数据组件
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年2月17日 下午2:31:16   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@DataSource("dataSource_nbhotelincr")
public interface IncrSetInfoDao {

	public IncrSetInfo queryByKey(String key);

	public void update(IncrSetInfo incrSetInfo);
	
	public void insert(IncrSetInfo incrSetInfo);

}
