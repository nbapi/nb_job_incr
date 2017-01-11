package com.elong.nb.checklist;

import java.util.UUID;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.JoinPoint;

import com.elong.nb.common.checklist.Constants;
import com.elong.nb.util.ThreadLocalUtil;
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
		ThreadLocalUtil.set(point.toString() + "_" + ELONG_REQUEST_STARTTIME, System.currentTimeMillis());

		Object guid = ThreadLocalUtil.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null) {
			ThreadLocalUtil.set(Constants.ELONG_REQUEST_REQUESTGUID, UUID.randomUUID().toString());
		}
	}

	/**
	 * 之后
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogAfter(JoinPoint point, Object returnValue) {
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) ThreadLocalUtil.get(point.toString() + "_" + ELONG_REQUEST_STARTTIME);
		float useTime = System.currentTimeMillis() - start;
		Object guid = ThreadLocalUtil.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null)
			guid = UUID.randomUUID().toString();

		ActionLogHelper.businessLog((String) guid, true, methodName, classFullName, null, useTime, 0, null, returnValue, point.getArgs());
	}

	public void handlerLogThrowing(JoinPoint point, Object throwing) {
		String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
		String methodName = point.getSignature().getName();
		long start = (Long) ThreadLocalUtil.get(point.toString() + "_" + ELONG_REQUEST_STARTTIME);
		float useTime = System.currentTimeMillis() - start;
		Object guid = ThreadLocalUtil.get(Constants.ELONG_REQUEST_REQUESTGUID);
		if (guid == null)
			guid = UUID.randomUUID().toString();

		Exception e = null;
		if (throwing instanceof Exception) {
			e = (Exception) throwing;
		}

		ActionLogHelper.businessLog((String) guid, false, methodName, classFullName, e, useTime, -1, e.getMessage(), null, point.getArgs());
	}

}
