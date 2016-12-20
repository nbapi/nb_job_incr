package com.elong.nb.checklist;

import java.util.UUID;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.elong.nb.common.checklist.Constants;
import com.elong.springmvc_enhance.utilities.ActionLogHelper;

public class ChecklistAspect {

	public static final String ELONG_REQUEST_STARTTIME = "elongRequestStartTime";

	/**
	 * 之前
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogBefore(JoinPoint point) {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		request.setAttribute(ELONG_REQUEST_STARTTIME, System.currentTimeMillis(), ServletRequestAttributes.SCOPE_REQUEST);

		Object guid = request.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);
		if (guid == null) {
			request.setAttribute(Constants.ELONG_REQUEST_REQUESTGUID, UUID.randomUUID().toString(), ServletRequestAttributes.SCOPE_REQUEST);
		}
	}

	/**
	 * 之后
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogAfter(JoinPoint point, Object returnValue) {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) request.getAttribute(ELONG_REQUEST_STARTTIME, ServletRequestAttributes.SCOPE_REQUEST);
		float useTime = System.currentTimeMillis() - start;
		Object guid = request.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);
		if (guid == null)
			guid = UUID.randomUUID().toString();
		String result = null;
		if (returnValue != null) {
			result = JSON.toJSONString(returnValue);
		}

		ActionLogHelper.businessLog((String) guid, true, methodName, classFullName, null, useTime, 0, null, result, point.getArgs());
	}

	public void handlerLogThrowing(JoinPoint point, Object throwing) {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) request.getAttribute(ELONG_REQUEST_STARTTIME, ServletRequestAttributes.SCOPE_REQUEST);
		float useTime = System.currentTimeMillis() - start;
		Object guid = request.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);
		if (guid == null)
			guid = UUID.randomUUID().toString();

		Exception e = null;
		if (throwing instanceof Exception) {
			e = (Exception) throwing;
		}

		ActionLogHelper.businessLog((String) guid, false, methodName, classFullName, e, useTime, -1, e.getMessage(), null, point.getArgs());
	}

}
