package com.elong.nb.checklist;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.elong.nb.common.checklist.Constants;
import com.elong.nb.common.checklist.EnumNBLogType;
import com.elong.nb.common.checklist.NBActionLogHelper;
import com.elong.nb.util.ThreadLocalUtil;

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
			if (guid == null) {// Controller请求回到此处
				guid = UUID.randomUUID().toString();
				request.setAttribute(Constants.ELONG_REQUEST_REQUESTGUID, guid, ServletRequestAttributes.SCOPE_REQUEST);
				ThreadLocalUtil.set(Constants.ELONG_REQUEST_REQUESTGUID, guid);
				
				Object[] args = point.getArgs();
				for (Object arg : args) {
					if (arg == null)
						continue;
					if (arg instanceof HttpServletRequest) {
						ThreadLocalUtil.set(Constants.ELONG_REQUEST_USERNAME, ((HttpServletRequest) arg).getHeader("userName"));
						break;
					}
				}
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

			Object userName = ThreadLocalUtil.get(Constants.ELONG_REQUEST_USERNAME);
			String userNameStr = userName == null?null:(String)userName;
			EnumNBLogType logType = StringUtils.contains(classFullName, "Controller")?EnumNBLogType.JOB_CONTROLLER:EnumNBLogType.DAO;
			NBActionLogHelper.businessLog((String) guid, true, methodName, classFullName, null, useTime, 0, null,
					null, JSON.toJSONString(point.getArgs()), userNameStr, logType);
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

			Object userName = ThreadLocalUtil.get(Constants.ELONG_REQUEST_USERNAME);
			String userNameStr = userName == null?null:(String)userName;
			EnumNBLogType logType = StringUtils.contains(classFullName, "Controller")?EnumNBLogType.JOB_CONTROLLER:EnumNBLogType.DAO;
			if (throwing instanceof Exception) {
				Exception e = (Exception) throwing;
				NBActionLogHelper.businessLog((String) guid, false, methodName, classFullName, e, useTime, -1, e.getMessage(), null,
						JSON.toJSONString(point.getArgs()), userNameStr, logType);
			}
		} catch (Exception e) {
		}
	}

}
