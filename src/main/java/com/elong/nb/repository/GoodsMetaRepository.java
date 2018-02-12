/**   
 * @(#)GoodsMetaRepository.java	2017年6月8日	下午2:32:17	   
 *     
 * Copyrights (C) 2017艺龙旅行网保留所有权利
 */
package com.elong.nb.repository;

import org.apache.thrift.TException;
import org.springframework.stereotype.Repository;

import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbRequest;
import com.elong.hotel.goods.ds.thrift.GetBasePrice4NbResponse;
import com.elong.hotel.searchagent.thrift.dss.GetInvAndInstantConfirmRequest;
import com.elong.hotel.searchagent.thrift.dss.GetInvAndInstantConfirmResponse;
import com.elong.nb.agent.thrift.utils.ThriftUtils;
import com.elong.nb.common.util.CommonsUtil;

/**
 * (类型功能说明描述)
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2017年6月8日 下午2:32:17   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
@Repository
public class GoodsMetaRepository {

	private static final String server_ip = CommonsUtil.CONFIG_PROVIDAR.getProperty("goods.server_ip");
	private static final int server_port = Integer.valueOf(CommonsUtil.CONFIG_PROVIDAR.getProperty("goods.server_port"));
	private static final int server_timeout = Integer.valueOf(CommonsUtil.CONFIG_PROVIDAR.getProperty("goods.server_timeout"));

	/** 
	 * 为了记checklist 
	 *
	 * @param request
	 * @return
	 * @throws TException
	 */
	public GetBasePrice4NbResponse getMetaPrice4Nb(GetBasePrice4NbRequest request) throws TException {
		return ThriftUtils.getMetaPrice4Nb(request, server_ip, server_port, server_timeout);
	}

	public GetInvAndInstantConfirmResponse getInventory(GetInvAndInstantConfirmRequest request) throws Exception {
		return ThriftUtils.getInventory(request, server_ip, server_port, server_timeout);
	}

}
