package com.dsqd.amc.linkedmo.mobiletown;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.model.SmsTran;
import com.dsqd.amc.linkedmo.service.SmsTranService;

import net.minidev.json.JSONObject;

public class mobiletownSMS {

	private static final Logger logger = LoggerFactory.getLogger(mobiletownSMS.class);

	private static String callback_number = "15335278";

	private static String subscribe_url = "https://linksafe.kr/subscribe_page.html";
	private static String subscribe_url_bitly_91 = "https://bit.ly/3YVxFzz";



	public mobiletownSMS() {}

	public mobiletownSMS(String callback_number) {
		this.callback_number = callback_number;
	}

	// 테스트용
	public static void main(String[] args) {
		mobiletownSMS sms = new mobiletownSMS();
		JSONObject json = sms.sendSms("01062235635", sms.setMessage1(genRandoms()));
		System.out.println(json.toJSONString());
		System.out.println(sms.setTrKey("01062235635"));
    }

	private String setTrKey(String mobileno) {
		// 현재 시각을 밀리초로 가져오기
		long currentMillis = System.currentTimeMillis();
		// 밀리초를 문자열로 변환
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String currentMillisString = sdf.format(new Date(currentMillis));
		// 두 문자열 결합
		return currentMillisString + mobileno;
	}

	public String setMessage1(String random) {
		return "휴대폰약속번호 서비스 인증번호[" + random +"]을(를) 입력해주세요.";
	}

	public String setMessage101(String offercode) {
		if ("91".equals(offercode)) {
			return "휴대폰약속번호 신청 하시겠습니까?\n\n"+subscribe_url_bitly_91;
		} else {
			return "휴대폰약속번호 신청 하시겠습니까?\n\nhttps://linksafe.kr/subscribe.html";
		}
	}

	public JSONObject sendSms(String receiver_phone, String message) {
		return send(receiver_phone, message);
	}

	private JSONObject send(String receiver_phone, String message) {
        JSONObject retObj = new JSONObject();
        String key = setTrKey(receiver_phone);

        try {
        	SmsTranService service = new SmsTranService();
        	SmsTran smsTran = SmsTran.builder()
        			.trUserId("allmycredit")
        			.trDestAddr(receiver_phone)
        			.trCallBack(callback_number)
        			.trSmsMessage(message)
        			.build();
        	service.insertSmsTran(smsTran);

        	retObj.put("code", 200);
        	retObj.put("msg", "sms send success");
        	retObj.put("key", key);

        } catch (Exception e) {
        	logger.error("SMS 발송 실패 : {}", e.getMessage(), e);
        	retObj.put("code", 9001);
        	retObj.put("msg", e.getMessage());
        }

        logger.info("SMS SEND : {}", retObj.toJSONString());
        return retObj;
	}

	public static String genRandoms() {
		SecureRandom secureRandom = new SecureRandom();
		int randomNumber = secureRandom.nextInt(1000000); // 0부터 999999 사이의 숫자 생성
		return String.format("%06d", randomNumber); // 6자리 문자열로 포맷팅 }
	}
}
