/**   
 * @(#)RedisKeyConst.java	2016年9月18日	下午3:45:42	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.consts;

import com.elong.nb.cache.ICacheKey;

/**
 * 增量job用到的所有redis的key常量
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月18日 下午3:45:42   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public interface RedisKeyConst {

	public static final String KEY_ID_S_M = "data.ms.sid_mid";
	public static final String KEY_ID_M_S = "data.ms.mid_sid";

	public static final String KEY_Hotel_S_M = "data.hotel.sid_mid";
	public static final String KEY_Hotel_M_S = "data.hotel.mid_sid";

	public static final String StateSyncTimeKey = "Incr.State.Time";

	public static final String KEY_Inventory_LastID = "Incr.Inventory.LastID";
	public static final String KEY_Rate_LastID = "Incr.Rate.LastID";
	public static final String KEY_Order_LastID = "Incr.Order.LastID";

	public static final String KEY_Proxy_CardNo_OrderFrom = "Proxy.CardNo.OrderFrom.{0}";

	public static final ICacheKey KEY_Rate_LastID_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_Rate_LastID;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_Order_LastID_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_Order_LastID;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_Inventory_LastID_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_Inventory_LastID;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey StateSyncTimeKey_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return StateSyncTimeKey;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_ID_S_M_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_ID_S_M;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_ID_M_S_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_ID_M_S;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_Hotel_S_M_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_Hotel_S_M;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

	public static final ICacheKey KEY_Hotel_M_S_CacheKey = new ICacheKey() {
		@Override
		public String getKey() {
			return KEY_Hotel_M_S;
		}

		@Override
		public int getExpirationTime() {
			return -1;
		}
	};

}
