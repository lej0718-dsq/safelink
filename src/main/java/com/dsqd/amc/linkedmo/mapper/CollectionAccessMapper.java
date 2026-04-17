package com.dsqd.amc.linkedmo.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.dsqd.amc.linkedmo.model.CollectionAccessCount;
import com.dsqd.amc.linkedmo.model.CollectionAccessPage;

@Mapper
public interface CollectionAccessMapper {

	// 접속 통계 조회
	List<CollectionAccessCount> getAllAccessCounts();

	List<CollectionAccessCount> searchAccessCounts(Map<String, Object> params);

	// 집계 대상 페이지 관리
	List<CollectionAccessPage> getAllAccessPages();

	List<CollectionAccessPage> getActiveAccessPages();

	CollectionAccessPage getAccessPageById(@Param("id") long id);

	void insertAccessPage(CollectionAccessPage page);

	void updateAccessPage(CollectionAccessPage page);
}