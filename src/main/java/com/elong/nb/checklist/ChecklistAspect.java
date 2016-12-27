package com.elong.nb.checklist;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.JoinPoint;

import com.alibaba.fastjson.JSON;
import com.elong.nb.common.checklist.Constants;
import com.elong.springmvc_enhance.utilities.ActionLogHelper;

public class ChecklistAspect {

	public static final String ELONG_REQUEST_STARTTIME = "elongRequestStartTime";

	 private static ThreadLocal<Map<String, Object>> threadLocal = new ThreadLocal<Map<String, Object>>() {  
	        public Map<String, Object> initialValue() {  
	            return new HashMap<String, Object>();  
	        }  
	    }; 

	/**
	 * 之前
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogBefore(JoinPoint point) {
		Map<String, Object> threadMap = threadLocal.get();
		threadMap.put(ELONG_REQUEST_STARTTIME, System.currentTimeMillis());

		Object guid = threadMap.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null) {
			threadMap.put(Constants.ELONG_REQUEST_REQUESTGUID, UUID.randomUUID().toString());
		}
		
		threadLocal.set(threadMap);
	}

	/**
	 * 之后
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogAfter(JoinPoint point, Object returnValue) {
		Map<String, Object> threadMap = threadLocal.get();
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) threadMap.get(ELONG_REQUEST_STARTTIME);
		float useTime = System.currentTimeMillis() - start;
		Object guid = threadMap.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null)
			guid = UUID.randomUUID().toString();
		String result = null;
		if (returnValue != null) {
			result = JSON.toJSONString(returnValue);
		}

		ActionLogHelper.businessLog((String) guid, true, methodName, classFullName, null, useTime, 0, null, result, point.getArgs());
		threadLocal.set(threadMap);
	}

	public void handlerLogThrowing(JoinPoint point, Object throwing) {
		Map<String, Object> threadMap = threadLocal.get();
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) threadMap.get(ELONG_REQUEST_STARTTIME);
		float useTime = System.currentTimeMillis() - start;
		Object guid = threadMap.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null)
			guid = UUID.randomUUID().toString();

		Exception e = null;
		if (throwing instanceof Exception) {
			e = (Exception) throwing;
		}

		ActionLogHelper.businessLog((String) guid, false, methodName, classFullName, e, useTime, -1, e.getMessage(), null, point.getArgs());
		threadLocal.set(threadMap);
	}

}
