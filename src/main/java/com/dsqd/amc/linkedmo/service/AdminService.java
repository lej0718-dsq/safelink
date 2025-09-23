package com.dsqd.amc.linkedmo.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.mapper.AdminMapper;
import com.dsqd.amc.linkedmo.mapper.BatchMapper;
import com.dsqd.amc.linkedmo.mapper.EvententryMapper;
import com.dsqd.amc.linkedmo.mapper.SubscribeMapper;
import com.dsqd.amc.linkedmo.model.Batch;
import com.dsqd.amc.linkedmo.model.Evententry;
import com.dsqd.amc.linkedmo.model.Subscribe;

import net.minidev.json.JSONObject;

public class AdminService {

	private SqlSessionFactory sqlSessionFactory;

    public AdminService() {
        this.sqlSessionFactory = MyBatisConfig.getSqlSessionFactory();
    }
    
    public void updateSubscribeStatus(Subscribe data) { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		SubscribeMapper mapper = session.getMapper(SubscribeMapper.class); 
    		mapper.updateSubscribeStatus(data); 
    		session.commit(); 
    	} 
    } 
    
    public void insertJsonData(JSONObject jsonData) { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		AdminMapper mapper = session.getMapper(AdminMapper.class); 
    		mapper.insertJsonData(jsonData); 
    		session.commit(); 
    	} 
    } 
    
    public List<Map<String, Object>> getJsonData(JSONObject jsonData) { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		AdminMapper mapper = session.getMapper(AdminMapper.class); 
    		return mapper.getJsonData(jsonData); 
    	}
    }
    
    public List<Map<String, Object>> summaryNet(JSONObject jsonData) { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		AdminMapper mapper = session.getMapper(AdminMapper.class); 
    		return mapper.summaryNet(jsonData); 
    	}
    }
    
    public List<Map<String, Object>> summaryOffer() { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		AdminMapper mapper = session.getMapper(AdminMapper.class); 
    		return mapper.summaryOffer(); 
    	}
    }
    
    public Map<String, Object> dashbdToday() { 
    	try (SqlSession session = sqlSessionFactory.openSession()) { 
    		AdminMapper mapper = session.getMapper(AdminMapper.class); 
    		return mapper.dashbdToday(); 
    	}
    }
    
    public List<Batch> getBatchAll() {
    	try (SqlSession session = sqlSessionFactory.openSession()) {
    		BatchMapper mapper = session.getMapper(BatchMapper.class);
    		return mapper.getBatchAll();
    	}
    }
    
    public List<Evententry> getAllEvententry() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	EvententryMapper mapper = session.getMapper(EvententryMapper.class);
            return mapper.getAllEvententry();
        }
    }
    
    public List<Evententry> getAllEvententry2() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	EvententryMapper mapper = session.getMapper(EvententryMapper.class);
            return mapper.getAllEvententry2();
        }
    }
    
    public int pingQuery() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
    		BatchMapper mapper = session.getMapper(BatchMapper.class);
    		return mapper.pingQuery();
        }
    }

    public Map<String, Object> getMonthlyStatistics(JSONObject params) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AdminMapper mapper = session.getMapper(AdminMapper.class);
            return mapper.getMonthlyStatistics(params);
        }
    }

    public List<Map<String, Object>> getDailyStatistics(JSONObject params) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AdminMapper mapper = session.getMapper(AdminMapper.class);
            return mapper.getDailyStatistics(params);
        }
    }

    public List<Map<String, Object>> getUserListForExport(JSONObject params) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AdminMapper mapper = session.getMapper(AdminMapper.class);
            return mapper.getUserListForExport(params);
        }
    }
}
