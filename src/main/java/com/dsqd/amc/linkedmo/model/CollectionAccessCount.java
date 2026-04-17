package com.dsqd.amc.linkedmo.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionAccessCount {

	private long id; // 자동생성
	private String page; // 페이지 경로
	private String description; // 설명
	private int count; // 접속 수
	private String logMonth; // 집계월 YYYYMM
	private Date collectedAt; // 집계일시
}