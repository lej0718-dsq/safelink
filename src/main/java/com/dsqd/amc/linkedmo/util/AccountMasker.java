package com.dsqd.amc.linkedmo.util;

/**
 * 계좌번호 · 전화번호 마스킹 유틸.
 * 응답·로그에서 PII 노출을 줄이기 위해 사용.
 */
public final class AccountMasker {

    private AccountMasker() {}

    /**
     * 계좌번호 마스킹: 앞 4자리와 뒤 2자리만 노출, 가운데는 '*'.
     *  - 12345678901234 → 1234********34
     *  - 1234567        → 1234***  (길이 짧으면 뒤 마스킹만)
     *  - null/빈문자열   → ""
     */
    public static String maskAccount(String account) {
        if (account == null || account.isEmpty()) return "";
        int len = account.length();
        if (len <= 4) {
            return repeat('*', len);
        }
        if (len <= 6) {
            return account.substring(0, 4) + repeat('*', len - 4);
        }
        String head = account.substring(0, 4);
        String tail = account.substring(len - 2);
        return head + repeat('*', len - 6) + tail;
    }

    /**
     * 전화번호 마스킹: 010-XXXX-XXXX 형식 또는 11자리 숫자 모두 지원.
     *  - 01012341234 → 010-****-1234
     *  - 010-1234-1234 → 010-****-1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() != 11) return phone; // 비표준이면 원본 반환
        return digits.substring(0, 3) + "-****-" + digits.substring(7);
    }

    private static String repeat(char ch, int times) {
        if (times <= 0) return "";
        char[] arr = new char[times];
        for (int i = 0; i < times; i++) arr[i] = ch;
        return new String(arr);
    }
}
