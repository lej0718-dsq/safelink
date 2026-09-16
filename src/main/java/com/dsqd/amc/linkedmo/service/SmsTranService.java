package com.dsqd.amc.linkedmo.service;

import com.dsqd.amc.linkedmo.mapper.SmsTranMapper;
import com.dsqd.amc.linkedmo.model.AgtTran;
import com.dsqd.amc.linkedmo.model.SmsTran;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import com.dsqd.amc.linkedmo.config.MyBatisConfig;

public class SmsTranService {

	private SqlSessionFactory sqlSessionFactory;

    public SmsTranService() {
        this.sqlSessionFactory = MyBatisConfig.getSqlSessionFactory();
    }

    public void insertSmsTran(SmsTran data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SmsTranMapper mapper = session.getMapper(SmsTranMapper.class);
            mapper.insertSmsTran(data);
            session.commit();
        }
    }

    /** 장문(LMS/MMS) 발송 — TB_AGT_TRAN INSERT. */
    public void insertAgtTran(AgtTran data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
        	SmsTranMapper mapper = session.getMapper(SmsTranMapper.class);
            mapper.insertAgtTran(data);
            session.commit();
        }
    }
}
