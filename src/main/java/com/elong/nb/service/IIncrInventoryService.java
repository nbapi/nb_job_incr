/**   
 * @(#)IIncrInventoryService.java	2016年9月21日	下午2:18:21	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.service;


/**
 * IncrInventory服务接口
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月21日 下午2:18:21   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface IIncrInventoryService {

	/** 
	 * 同步库存增量
	 *
	 */
	public void syncInventoryToDB();

	/** 
	 * 递归，根据changeID同步库存增量 
	 *
	 * @param changeID
	 * @param beginTime 本次递归开始时间
	 */
	public void syncInventoryToDB(long changeID, long beginTime);
	
	/** 
	 * 同步库存黑名单引起库存增量
	 *
	 */
	public void syncInventoryDueToBlack();

}
