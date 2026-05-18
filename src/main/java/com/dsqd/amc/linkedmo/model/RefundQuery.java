package com.dsqd.amc.linkedmo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 관리자 환불 목록 조회 필터.
 */
@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundQuery {
    private String status;      // REQUESTED / COMPLETED / null(전체)
    private String mobileno;    // 부분 일치
    private String from;        // yyyy-MM-dd  (requestDate 기준)
    private String to;          // yyyy-MM-dd  (requestDate 기준)
}
