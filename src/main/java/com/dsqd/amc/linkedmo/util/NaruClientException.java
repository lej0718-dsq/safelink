package com.dsqd.amc.linkedmo.util;

/**
 * NARU(외부) 서버가 4xx 를 응답한 경우 사용하는 예외.
 *
 * 4xx 는 서버 장애가 아니라 요청/데이터 문제(예: 404 = 해당 번호 데이터 없음)이므로,
 * 이 예외가 발생해도 {@code RequestSender} 는 서버를 활성 목록에서 제거하지 않는다.
 * (연결 실패·5xx 만 서버 장애로 간주하여 제거)
 */
public class NaruClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;

    public NaruClientException(int statusCode, String body) {
        super("NARU 응답 " + statusCode + (body == null || body.isEmpty() ? "" : " : " + body));
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
