package com.elong.nb.service.impl;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.elong.nb.dao.IncrSetInfoDao;
import com.elong.nb.model.IncrSetInfo;
import com.elong.nb.service.IIncrSetInfoService;

@Service
public class IncrSetInfoServiceImpl implements IIncrSetInfoService {

	@Resource
	private IncrSetInfoDao dao;

	@Override
	public String get(String key) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("the parameter['key'] must not be null or empty.");
		}
		IncrSetInfo incrSetInfo = dao.queryByKey(key);
		if (incrSetInfo == null || StringUtils.isEmpty(incrSetInfo.getSetVal()))
			return StringUtils.EMPTY;
		return incrSetInfo.getSetVal();
	}

	@Override
	public void put(String key, Object value) {
		if (StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException("the parameter['key'] must not be null or empty.");
		}
		String setValue = value==null ? StringUtils.EMPTY : JSON.toJSONString(value);

		IncrSetInfo incrSetInfo = dao.queryByKey(key);
		if (incrSetInfo == null) {
			incrSetInfo = new IncrSetInfo();
			incrSetInfo.setSetKey(key);
			incrSetInfo.setSetVal(setValue);
			incrSetInfo.setSetDesc(key);
			dao.insert(incrSetInfo);
		} else {
			incrSetInfo.setSetVal(setValue);
			dao.update(incrSetInfo);
		}
	}

}
