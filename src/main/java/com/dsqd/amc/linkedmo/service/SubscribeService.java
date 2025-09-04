package com.dsqd.amc.linkedmo.service;

import com.dsqd.amc.linkedmo.mapper.SubscribeMapper;
import com.dsqd.amc.linkedmo.model.Subscribe;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import com.dsqd.amc.linkedmo.config.MyBatisConfig;

public class SubscribeService {

	private SqlSessionFactory sqlSessionFactory;

    public SubscribeService() {
        this.sqlSessionFactory = MyBatisConfig.getSqlSessionFactory();
    }

    public void insertSubscribe(Subscribe data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            mapper.insertSubscribe(data);
            session.commit();
        }
    }

    public void updateSubscribe(Subscribe data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            mapper.updateSubscribe(data);
            session.commit();
        }
    }
    
    public void updateSubscribeStatus(Subscribe data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            mapper.updateSubscribeStatus(data);
            session.commit();
        }
    }

    public Subscribe getSubscribeById(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getSubscribeById(id);
        }
    }
    
    public List <Subscribe> getSubscribeByMobileno(Subscribe data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getSubscribeByMobileno(data);
        }
    }
    
    public List <Subscribe> getTodaySubscribeByMobileno(Subscribe data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getTodaySubscribeByMobileno(data);
        }
    }
    
    public List <Subscribe> getSubscribeAll() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getSubscribeAll();
        }
    }
    
    public List <Subscribe> getSubscribeAllActive() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getSubscribeAllActive();
        }
    }
    
    public List <Subscribe> getCancelList() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getCancelList();
        }
    }

    public void deleteSubscribe(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            mapper.deleteSubscribe(id);
            session.commit();
        }
    }
    
    public void deleteSubscribeT0(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            mapper.deleteSubscribeT0(id);
            session.commit();
        }
    }
    
    public List <Subscribe> getSubscribeAlltoBatch01() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getSubscribeAlltoBatch01();
        }
    }

    public String getFirstSubscriber(String mobileno) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SubscribeMapper mapper = session.getMapper(SubscribeMapper.class);
            return mapper.getFirstSubscriber(mobileno);
        }
    }
}
