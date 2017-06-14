package com.elong.nb.checklist;

import java.util.UUID;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
		try {
			ThreadLocalUtil.set(point.toString() + "_" + ELONG_REQUEST_STARTTIME, System.currentTimeMillis());
			RequestAttributes request = RequestContextHolder.getRequestAttributes();
			// 调用链新起的线程，拦截方法会在此返回
			if (request == null)
				return;

			// 1、Controller 2、调用链只有一个线程的所有拦截方法
			Object guid = request.getAttribute(Constants.ELONG_REQUEST_REQUESTGUID, ServletRequestAttributes.SCOPE_REQUEST);
			//
			if (guid == null) {// Controller请求回到此处
				guid = UUID.randomUUID().toString();
				request.setAttribute(Constants.ELONG_REQUEST_REQUESTGUID, guid, ServletRequestAttributes.SCOPE_REQUEST);
				ThreadLocalUtil.set(Constants.ELONG_REQUEST_REQUESTGUID, guid);
			}
		} catch (Exception e) {
		}
	}

	/**
	 * 之后
	 * 
	 * @param point
	 * @throws Throwable
	 */
	public void handlerLogAfter(JoinPoint point, Object returnValue) {
		try {
			String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
			String methodName = point.getSignature().getName();
			long start = (Long) ThreadLocalUtil.get(point.toString() + "_" + ELONG_REQUEST_STARTTIME);
			float useTime = System.currentTimeMillis() - start;
			Object guid = ThreadLocalUtil.get(Constants.ELONG_REQUEST_REQUESTGUID);
			if (guid == null)
				guid = UUID.randomUUID().toString();

			ActionLogHelper.businessLog((String) guid, true, methodName, classFullName, null, useTime, 0, null, returnValue,
					point.getArgs());
		} catch (Exception e) {
		}
	}

	public void handlerLogThrowing(JoinPoint point, Object throwing) {
		try {
			String classFullName = ClassUtils.getShortClassName(point.getSignature().getDeclaringTypeName());
			String methodName = point.getSignature().getName();
			long start = (Long) ThreadLocalUtil.get(point.toString() + "_" + ELONG_REQUEST_STARTTIME);
			float useTime = System.currentTimeMillis() - start;
			Object guid = ThreadLocalUtil.get(Constants.ELONG_REQUEST_REQUESTGUID);
			if (guid == null)
				guid = UUID.randomUUID().toString();

			if (throwing instanceof Exception) {
				Exception e = (Exception) throwing;
				ActionLogHelper.businessLog((String) guid, false, methodName, classFullName, e, useTime, -1, e.getMessage(), null,
						point.getArgs());
			}
		} catch (Exception e) {
		}
	}

}
