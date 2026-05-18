package com.dsqd.amc.linkedmo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dsqd.amc.linkedmo.model.RefundCalculation;
import com.dsqd.amc.linkedmo.model.RefundDetail;

/**
 * 환불 금액 일할 계산기.
 *
 * 정책:
 *  - useStartDate = createDate (가입일 포함)
 *  - useEndDate   = cancelDate - 1일  (해지일 당일은 사용일에서 제외)
 *  - 사용 기간을 달력 월 단위로 분할하여 각 월별로 (월정액 / 해당월 총일수) * 사용일수 를 합산
 *  - 반올림 정책: HALF_UP, 원 단위
 *
 * 예시 (월정액 1,650 / createDate 2025-01-02 / cancelDate 2025-02-18):
 *  - useEnd = 2025-02-17
 *  - 2025-01: 30일 / 31일, 일당 53.2258, 1,597원
 *  - 2025-02: 17일 / 28일, 일당 58.9286, 1,002원
 *  - 합계: 2,599원
 */
public class RefundCalculator {

    private static final DateTimeFormatter YM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int DAILY_FEE_SCALE = 10;

    /**
     * 환불 금액을 일할 계산한다.
     *
     * @param createDate 가입일 (포함). null 불가.
     * @param cancelDate 해지일 (당일은 사용일에서 제외). null 불가.
     * @param monthlyFee 월정액. 양수여야 함.
     * @return 월별 내역과 총액을 담은 RefundCalculation
     */
    public RefundCalculation calculate(LocalDate createDate, LocalDate cancelDate, int monthlyFee) {
        if (createDate == null) {
            throw new IllegalArgumentException("createDate is required");
        }
        if (cancelDate == null) {
            throw new IllegalArgumentException("cancelDate is required");
        }
        if (monthlyFee <= 0) {
            throw new IllegalArgumentException("monthlyFee must be positive: " + monthlyFee);
        }

        LocalDate useStart = createDate;
        LocalDate useEnd = cancelDate.minusDays(1);  // 해지일 당일은 사용일에서 제외

        // 사용일수가 0 이하이면 빈 결과 반환 (서비스 레이어에서 4xx 처리)
        if (useEnd.isBefore(useStart)) {
            return RefundCalculation.builder()
                .useStartDate(useStart)
                .useEndDate(useEnd)
                .totalAmount(0)
                .details(new ArrayList<>())
                .build();
        }

        List<RefundDetail> details = new ArrayList<>();
        int total = 0;

        LocalDate cursor = useStart;
        while (!cursor.isAfter(useEnd)) {
            YearMonth ym = YearMonth.from(cursor);
            LocalDate monthLastDay = ym.atEndOfMonth();
            LocalDate monthStart = cursor;
            LocalDate monthEnd = monthLastDay.isBefore(useEnd) ? monthLastDay : useEnd;

            int usedDays = (int) (monthEnd.toEpochDay() - monthStart.toEpochDay()) + 1;
            int totalDays = ym.lengthOfMonth();

            BigDecimal dailyFee = BigDecimal.valueOf(monthlyFee)
                .divide(BigDecimal.valueOf(totalDays), DAILY_FEE_SCALE, RoundingMode.HALF_UP);

            int amount = dailyFee
                .multiply(BigDecimal.valueOf(usedDays))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

            details.add(RefundDetail.builder()
                .ym(ym.format(YM_FORMAT))
                .monthStart(toDate(monthStart))
                .monthEnd(toDate(monthEnd))
                .usedDays(usedDays)
                .totalDays(totalDays)
                .dailyFee(dailyFee.setScale(4, RoundingMode.HALF_UP))
                .amount(amount)
                .build());

            total += amount;
            cursor = monthLastDay.plusDays(1);
        }

        return RefundCalculation.builder()
            .useStartDate(useStart)
            .useEndDate(useEnd)
            .totalAmount(total)
            .details(details)
            .build();
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
