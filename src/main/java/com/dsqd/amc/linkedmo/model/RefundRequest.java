package com.dsqd.amc.linkedmo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용자가 refund.html 폼에서 제출하는 환불 신청 입력 DTO.
 * 계좌번호(account)는 평문이며 서비스 레이어에서 AES256 암호화하여 저장한다.
 */
@ToString(exclude = {"account"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    private String name;
    private String birth;     // yyyy-MM-dd
    private String phone;     // 01012341234 (하이픈 없음)
    private String bankCode;
    private String bankName;
    private String account;   // 평문 — 로그 노출 금지
}
