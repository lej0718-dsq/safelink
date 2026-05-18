package com.dsqd.amc.linkedmo.model;

/**
 * 환불 처리 상태. 사용자 입장에서 단순한 2단계로 관리.
 *  - REQUESTED : 사용자가 환불 신청한 직후, 관리자 처리 대기
 *  - COMPLETED : 관리자가 환불(송금) 완료 처리
 */
public enum RefundStatus {
    REQUESTED,
    COMPLETED;

    public static RefundStatus from(String name) {
        if (name == null) return null;
        try {
            return RefundStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
