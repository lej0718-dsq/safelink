package com.dsqd.amc.linkedmo.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 환불 신청 마스터 엔티티 (refund 테이블)
 */
@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    private long id;
    private String mobileno;
    private String name;
    private String birth;          // yyyy-MM-dd
    private String bankCode;
    private String bankName;
    private String accountEnc;     // AES256 암호문
    private long subscribeId;
    private Date useStartDate;
    private Date useEndDate;
    private int refundAmount;
    private String status;         // RefundStatus.name()
    private String memo;
    private Date requestDate;
    private Date processedDate;
    private String processedBy;
}
