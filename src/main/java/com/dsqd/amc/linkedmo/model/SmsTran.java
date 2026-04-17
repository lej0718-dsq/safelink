package com.dsqd.amc.linkedmo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SmsTran {
	private String trUserId;		// 사용자 ID
	private String trInstDate;		// 등록일시
	private String trSendDate;		// 발송일시
	private String trDestAddr;		// 수신자 번호
	private String trCallBack;		// 회신번호
	private String trSmsMessage;	// 메시지 내용
}