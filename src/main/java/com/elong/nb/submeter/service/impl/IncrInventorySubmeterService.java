/**   
 * @(#)IncrInventorySubmeterService.java	2017年4月21日	上午10:19:58	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.IncrInsertStatistic;
import com.elong.nb.dao.IncrInventoryDao;
import com.elong.nb.model.bean.IncrInventory;
import com.elong.nb.model.enums.EnumIncrType;
import com.elong.nb.util.DateHandlerUtils;

/**
 * IncrInventory分表实现
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年4月21日 上午10:19:58   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Service(value = "incrInventorySubmeterService")
public class IncrInventorySubmeterService extends AbstractSubmeterService<IncrInventory> {

	@Resource
	private IncrInventoryDao incrInventoryDao;

	/** 
	 * 获取分表前缀  
	 *
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#getTablePrefix()    
	 */
	@Override
	public String getTablePrefix() {
		return "IncrInventory";
	}

	/** 
	 * 插入分表数据
	 *
	 * @param subTableName
	 * @param subRowList
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#bulkInsertSub(java.lang.String, java.util.List)    
	 */
	@Override
	protected int bulkInsertSub(String subTableName, final List<IncrInventory> subRowList) {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String businessType = "nbincrinsert";
					for (IncrInventory incrInventory : subRowList) {
						IncrInsertStatistic statisticModel = new IncrInsertStatistic();
						String logTime = DateHandlerUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
						statisticModel.setBusiness_type(businessType);
						statisticModel.setIncrType(EnumIncrType.Inventory.name());
						statisticModel.setChangeTime(DateHandlerUtils.formatDate(incrInventory.getChangeTime(), "yyyy-MM-dd HH:mm:ss"));
						statisticModel.setInsertTime(DateHandlerUtils.formatDate(incrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss"));
						statisticModel.setLog_time(logTime);
						IncrInventory slaveIncrInventory = getLastIncrData(null);
						String slaveInsertTime = slaveIncrInventory == null ? logTime : DateHandlerUtils.formatDate(
								slaveIncrInventory.getInsertTime(), "yyyy-MM-dd HH:mm:ss");
						statisticModel.setSlaveInsertTime(slaveInsertTime);
						minitorLogger.info(JSON.toJSONString(statisticModel));
					}
				} catch (Exception e) {
				}
			}
		});
		return 0;// incrInventoryDao.bulkInsertSub(subTableName, subRowList);
	}

	/** 
	 * 获取分表数据 
	 *
	 * @param subTableName
	 * @param params
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getIncrDataList(java.lang.String, java.util.Map)    
	 */
	@Override
	protected List<IncrInventory> getIncrDataList(String subTableName, Map<String, Object> params) {
		return incrInventoryDao.getIncrInventories(subTableName, params);
	}

	/** 
	 * 创建分表
	 *
	 * @param newTableName 
	 *
	 * @see com.elong.nb.submeter.service.ISubmeterService#createSubTable(java.lang.String)    
	 */
	@Override
	public void createSubTable(String newTableName) {
		incrInventoryDao.createSubTable(newTableName);
	}

	/** 
	 * 获取最后一条记录
	 *
	 * @param subTableName
	 * @param trigger
	 * @return 
	 *
	 * @see com.elong.nb.submeter.service.impl.AbstractSubmeterService#getLastIncrData(java.lang.String, java.lang.String)    
	 */
	@Override
	protected IncrInventory getLastIncrData(String subTableName, String trigger) {
		return incrInventoryDao.getLastIncrData(subTableName);
	}

}
