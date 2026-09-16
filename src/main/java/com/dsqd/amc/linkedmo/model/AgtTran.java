package com.dsqd.amc.linkedmo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * TB_AGT_TRAN (장문 MMS/LMS 발송 큐) INSERT 용 DTO.
 * 단문 SMS는 {@link SmsTran}/TB_AGT_SMS_TRAN 을 사용하고,
 * 90byte 초과 장문(환불 접수 안내 등)은 이 테이블을 사용한다.
 */
@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgtTran {
	private String trUserId;		// 사용자 ID
	private String trDestAddr;		// 수신자 번호
	private String trCallBack;		// 회신(콜백) 번호
	private String trMsgType;		// 메시지 타입 (S:SMS, L:LMS, M:MMS)
	private int    trContentsCnt;	// 첨부 콘텐츠 수 (첨부 없으면 0)
	private String trSubject;		// 제목 (LMS/MMS)
	private String trMmsMessage;	// 본문 (장문)
	private String trOrigAddr;		// 발신번호
}
