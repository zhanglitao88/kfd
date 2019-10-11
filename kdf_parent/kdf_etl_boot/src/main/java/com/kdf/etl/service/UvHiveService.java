package com.kdf.etl.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.hive.HiveClient;
import org.springframework.data.hadoop.hive.HiveClientCallback;
import org.springframework.data.hadoop.hive.HiveTemplate;
import org.springframework.stereotype.Service;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @title: UvHiveService.java 
 * @package com.kdf.etl.service 
 * @description: uv统计的hiveService
 * @author: 、T
 * @date: 2019年10月11日 下午2:50:02 
 * @version: V1.0
 */
@Slf4j
@Service
public class UvHiveService {

	@Autowired
	private HiveTemplate hiveTemplate;
	
	public List<Map<String, String>> getBrowserUv(String yearMonthDayHour) {
		StringBuffer hiveSql = new StringBuffer();
		hiveSql.append("select appid appid, browser_name browserName, count(browser_name) count from pv_log_hive_");
		hiveSql.append(yearMonthDayHour);
		hiveSql.append(" group by browser_name, appid");
		if(log.isInfoEnabled()) {
			log.info("getBrowserUv  hiveSql=[{}]", hiveSql.toString());
		}
		List<Map<String, String>> resultList = hiveTemplate.execute(new HiveClientCallback<List<Map<String, String>>>() {
			@Override
			public List<Map<String, String>> doInHive(HiveClient hiveClient) throws Exception {
				List<Map<String, String>> uvList = Lists.newArrayList();
				Connection conn = hiveClient.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(hiveSql.toString());
				while(rs.next()) {
					String browserName = rs.getString("browserName");
					String count = rs.getString("count");
					String appid = rs.getString("appid");
					Map<String, String> reMap = Maps.newHashMap();
					reMap.put("browserName", browserName);
					reMap.put("count", count);
					reMap.put("appid", appid);
					uvList.add(reMap);
				}
				return uvList;
			}
		});
		log.error("resultList========={}", resultList);
		return resultList;
	}
}