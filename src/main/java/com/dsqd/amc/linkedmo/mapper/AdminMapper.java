package com.dsqd.amc.linkedmo.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface AdminMapper {
	
	List<Map<String, Object>> getJsonData(Map<String, Object> jsonData); 

	void insertJsonData(Map<String, Object> jsonData); 
	
	List<Map<String, Object>> summaryNet(Map<String, Object> jsonData); 

	Map<String, Object> dashbdToday();

	List<Map<String, Object>> summaryOffer();

	Map<String, Object> getMonthlyStatistics(Map<String, Object> params);
    List<Map<String, Object>> getDailyStatistics(Map<String, Object> params);
    List<Map<String, Object>> getUserListForExport(Map<String, Object> params);
}