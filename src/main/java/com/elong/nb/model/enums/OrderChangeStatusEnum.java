/**   
 * @(#)OrderChangeStatusEnum.java	2016年9月19日	下午1:44:30	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.model.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 增量订单过滤状态枚举
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月19日 下午1:44:30   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public enum OrderChangeStatusEnum {

	V("V"),N("N"), A("A"), F("F"), C("C"), Z("Z"), D("D"), B("B"), O("O"), G("G"), U("U"), H("H"), B1("B1"), B2("B2"), B3("B3"), S("S");

	private String code;

	/**   
	 *   
	 * @param code   
	 */
	private OrderChangeStatusEnum(String code) {
		this.code = code;
	}

	/** 
	 * 指定code是否属于枚举范围
	 *
	 * @param code
	 * @return
	 */
	public static boolean containCode(String code) {
		if (StringUtils.isEmpty(code))
			return false;
		for (OrderChangeStatusEnum statusEnum : OrderChangeStatusEnum.values()) {
			if (code.equals(statusEnum.code)) {
				return true;
			}
		}
		return false;
	}

	/** 
	 * 获取所有code字符串
	 *
	 * @return
	 */
	public static String toAllCode() {
		StringBuffer sb = new StringBuffer();
		for (OrderChangeStatusEnum statusEnum : OrderChangeStatusEnum.values()) {
			sb.append("'" + statusEnum.code + "',");
		}
		return sb.substring(0, sb.length() - 1);
	}

	@Override
	public String toString() {
		return this.code;
	}

}
