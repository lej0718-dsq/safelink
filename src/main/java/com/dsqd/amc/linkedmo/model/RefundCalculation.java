package com.dsqd.amc.linkedmo.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 환불 금액 계산 결과 DTO (RefundCalculator 의 출력).
 * 비즈니스 로직과 분리하기 위해 LocalDate 사용 — 컨트롤러/서비스 경계에서 java.util.Date 로 변환.
 */
@Getter
@Builder
@AllArgsConstructor
@ToString
public class RefundCalculation {
    private final LocalDate useStartDate;
    private final LocalDate useEndDate;
    private final int totalAmount;
    private final List<RefundDetail> details;

    public List<RefundDetail> getDetails() {
        return details == null ? Collections.emptyList() : Collections.unmodifiableList(details);
    }
}
