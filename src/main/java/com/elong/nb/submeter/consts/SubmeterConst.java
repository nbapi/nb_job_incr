/**   
 * @(#)SubmeterConst.java	2017年5月2日	下午3:23:54	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.submeter.consts;

/**
 * 分表相关常量
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年5月2日 下午3:23:54   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface SubmeterConst {

	/** 
	 * 存在于redis缓存中的非空表名最大数量(DB drop分表时要保证数据库非空分表至少有10个)
	 *
	 * int SubmeterConst.java NOEMPTY_SUMETER_COUNT_IN_REDIS
	 */
	public static final int NOEMPTY_SUMETER_COUNT_IN_REDIS = 10;

	/** 
	 * 数据库中末端连续空分表数量
	 *
	 * int SubmeterConst.java EMPTY_SUBMETER_COUNT_IN_DB
	 */
	public static final int EMPTY_SUBMETER_COUNT_IN_DB = 30;

	/** 
	 * 每个分表最大记录数
	 *
	 * int SubmeterConst.java PER_SUBMETER_ROW_COUNT
	 */
	public static final int PER_SUBMETER_ROW_COUNT = 100000;

}
