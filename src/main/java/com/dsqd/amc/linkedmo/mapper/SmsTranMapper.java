package com.dsqd.amc.linkedmo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.dsqd.amc.linkedmo.model.AgtTran;
import com.dsqd.amc.linkedmo.model.SmsTran;

@Mapper
public interface SmsTranMapper {

    void insertSmsTran(SmsTran data);

    // 장문(LMS/MMS) 발송 — TB_AGT_TRAN
    void insertAgtTran(AgtTran data);

}