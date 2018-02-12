/**   
 * @(#)ExecutorUtils.java	2016年9月12日	上午11:10:05	   
 *     
 * Copyrights (C) 2016艺龙旅行网保留所有权利
 */
package com.elong.nb.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 线程池工具类
 *
 * <p>
 * 修改历史:											<br>  
 * 修改日期    		修改人员   	版本	 		修改内容<br>  
 * -------------------------------------------------<br>  
 * 2016年9月12日 上午11:10:05   suht     1.0    	初始化创建<br>
 * </p> 
 *
 * @author		suht  
 * @version		1.0  
 * @since		JDK1.7
 */
public class ExecutorUtils {
	
	private static final Log logger = LogFactory.getLog(ExecutorUtils.class);

	private static RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if (!executor.isShutdown()) {
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	};

	/** 
	 * 创建线程池,任务队列满了阻塞
	 *
	 * @param maximumPoolSize 最大线程数
	 * @param queueLenght	最大任务数
	 * @return
	 */
	public static ExecutorService newSelfThreadPool(int maximumPoolSize, int queueLenght) {
		return newSelfThreadPool(maximumPoolSize, queueLenght, rejectedExecutionHandler);
	}
	
	/** 
	 * 创建线程池 
	 *
	 * @param maximumPoolSize 最大线程数
	 * @param queueLenght 最大任务数
	 * @param handler 队列满了处理策略
	 * @return
	 */
	public static ExecutorService newSelfThreadPool(int maximumPoolSize, int queueLenght, RejectedExecutionHandler handler) {
		return new ThreadPoolExecutor(0, maximumPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(queueLenght),
				Executors.defaultThreadFactory(), handler);
	}

}
