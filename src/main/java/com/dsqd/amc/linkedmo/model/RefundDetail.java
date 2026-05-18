package com.dsqd.amc.linkedmo.model;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 환불 월별 일할 계산 내역 (refund_detail 테이블)
 */
@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDetail {
    private long id;
    private long refundId;
    private String ym;             // yyyy-MM
    private Date monthStart;
    private Date monthEnd;
    private int usedDays;
    private int totalDays;
    private BigDecimal dailyFee;
    private int amount;
}
