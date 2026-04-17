package com.dsqd.amc.linkedmo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.dsqd.amc.linkedmo.model.SmsTran;

@Mapper
public interface SmsTranMapper {

    void insertSmsTran(SmsTran data);

}