package com.dsqd.amc.linkedmo.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.OptionalInt;

/**
 * 만 나이 계산 유틸리티.
 * SKT ISICS00021 응답의 SSN_BIRTH_DT(yyyyMMdd) 를 만 나이로 변환할 때 사용한다.
 */
public final class AgeUtil {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private AgeUtil() {}

    /**
     * yyyyMMdd 생년월일 문자열을 만 나이로 변환한다.
     * null / 형식 오류 / 미래 일자 등 부적절한 입력은 OptionalInt.empty() 를 반환한다.
     */
    public static OptionalInt calcKoreanAge(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return OptionalInt.empty();
        }
        try {
            LocalDate birth = LocalDate.parse(yyyymmdd, YYYYMMDD);
            LocalDate today = LocalDate.now();
            if (birth.isAfter(today)) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Period.between(birth, today).getYears());
        } catch (DateTimeParseException e) {
            return OptionalInt.empty();
        }
    }
}
