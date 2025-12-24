package com.dsqd.amc.linkedmo.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.dsqd.amc.linkedmo.model.Blocknumber;


@Mapper
public interface BlocknumberMapper {

	List<Blocknumber> getAllBlocknumber();

	List<Blocknumber> searchBlocknumbers(Map<String, Object> params);

	Blocknumber getBlocknumberById(@Param("id") int id);

	Blocknumber getBlocknumberByMobileno(@Param("mobileno") String mobileno); // 현재 status가 A인 데이터만 조회
	
	Blocknumber getBlocknumberByMobileno2(@Param("mobileno") String mobileno); // status 상관없이 조회 

	Blocknumber getBlocknumberByMobilenoSKT(@Param("mobileno") String mobileno); // SKT 전용, 현재 status가 A인 데이터만 조회
	Blocknumber getBlocknumberByMobilenoKT(@Param("mobileno") String mobileno); // KT 전용, 현재 status가 A인 데이터만 조회
	Blocknumber getBlocknumberByMobilenoLGU(@Param("mobileno") String mobileno); // LGU 전용, 현재 status가 A인 데이터만 조회

    void insertBlocknumber(Blocknumber blocknumber);

    void updateBlocknumber(Blocknumber blocknumber);

    void deleteBlocknumber(Blocknumber blocknumber);

    void deleteBlocknumberByMobileno(Blocknumber blocknumber);
}