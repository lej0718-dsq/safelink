package com.dsqd.amc.linkedmo.skt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.util.JSONHelper;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class SubscribeSK {
	
	private static final Logger logger = LoggerFactory.getLogger(SubscribeSK.class);
	
	public JSONObject user(Subscribe data) {
		logger.info("DATA : {}", data.toString());
		Map<String, String> param = new HashMap<String, String>();
		
		return ISICS00021(param, data);
	}
	
	public JSONObject subscribe(Subscribe data) {
		logger.info("DATA : {}", data.toString());
		Map<String, String> param = new HashMap<String, String>();
		param.put("action", "subscribe"); // 가입요청 
		param.put("mobileno", data.getMobileno()); // 전화번호 연결
		
		return ISSWG00047(param, data);
	}
	
	public JSONObject cancel(Subscribe data) {
		logger.info("DATA : {}", data.toString());
		Map<String, String> param = new HashMap<String, String>();
		param.put("action", "cancel"); // 가입요청 
		param.put("mobileno", data.getMobileno()); // 전화번호 연결
		
		return ISSWG00047(param, data);
	}
	
	public JSONObject prove(Subscribe data) {
		logger.info("DATA : {}", data.toString());
		Map<String, String> param = new HashMap<String, String>();
		
		return ISICS00022(param, data);
	}
	
	public JSONObject prove(String mobileno) {
		return prove(Subscribe.builder().mobileno(mobileno).build());
	}
	
	private JSONObject ISSWG00047(Map<String, String> param, Subscribe data) {
		
		APICall api = new APICall();
		
		int code = 999;
		String msg = "";
		
		/**
		 * JSON : {"RESPONSE":
		 * 			{"HEADER":
		 * 				{"RESULT_CODE":"00",
		 * 				"RESULT":"S",
		 * 				"RESULT_MESSAGE":"서비스가 정상적으로 처리되었습니다."},
		 * 			"BODY":"암호화된 String 내용"}
		 * 		  }
		 */
		String resp = api.ISSWG00047(param);
		if (resp != null && !"".equals(resp)) {
			JSONObject itfJSON = (JSONObject) JSONValue.parse(resp);
			JSONObject jsonHeader = (JSONObject) itfJSON.get("HEADER");
			
			String RESPONSE_CODE = jsonHeader.getAsString("RESPONSE_CODE");
			String RESULT_CODE = jsonHeader.getAsString("RESULT_CODE"); //S - 00
			String RESULT = jsonHeader.getAsString("RESULT"); // S or F
			
			String RESULT_MESSAGE = jsonHeader.getAsString("RESULT_MESSAGE");
			if (RESULT_MESSAGE == null) RESULT_MESSAGE = "";
			
			if ("F".equals(RESULT)) { // 오류인 경우 
				if ("ZNGME0005".equals(RESPONSE_CODE)) {
				//"RESULT_MESSAGE": "ZNGME0005|부가서비스(휴대폰약속번호)  가입처리중 중복으로 처리할 수 없습니다."
					code = 901;
					
				} else if ("ZNGME0007".equals(RESPONSE_CODE)) {
				//"RESULT_MESSAGE": "ZNGME0007|서비스번호 입력이 잘못되었으니 확인하십시오."
					code = 912;
				
				} else if ("ZINVE8101".equals(RESPONSE_CODE)) {
				//"RESULT_MESSAGE": "ZINVE8101|가입신청 상품[휴대폰약속번호]은 이미 사용중입니다."
					code = 901;
				
				} else {
					code = 998;
					msg = "통신사 부가서비스 가입이 원활하지 않아요. 잠시후 다시 해주세요.[998]";
				}
				api = null; // 자원반납
				
				// 에러 후 반환처리 
				String[] parts = RESULT_MESSAGE.split("\\|");
				if (parts.length > 1) msg = parts[1]; else msg = RESULT_MESSAGE;
				return JSONHelper.assembleResponse(code, msg);
				
			} else if ("S".equals(RESULT)) { // 정상인 경우
				api = null; // 자원반납
				code = 200; // 정상코드
				msg = "";
			} else {
				msg = "RESULT 이상 : " + RESULT + "/" + RESULT_CODE;
			}
			
		} else {
			api = null;
			return JSONHelper.assembleResponse(914, "통신사 부가서비스 가입이 원활하지 않아요. 잠시후 다시 해주세요.[914]");
		}
		api = null;
		return JSONHelper.assembleResponse(code, msg);
	}

	//가입상태를 조회하여 SVC_MGMT_NUM을 읽어옴
	private JSONObject ISICS00022(Map<String, String> param, Subscribe data) {
		
		APICall api = new APICall();
		
		int code = 999;
		String msg = "";
		
		/**
		 * JSON : {"RESPONSE":
		 * 			{"HEADER":
		 * 				{"RESULT_CODE":"00",
		 * 				"RESULT":"S",
		 * 				"RESULT_MESSAGE":"서비스가 정상적으로 처리되었습니다."},
		 * 			"BODY":"암호화된 String 내용"}
		 * 		  }
		 */
		String resp = api.ISICS00022(data.getMobileno());
		if (resp != null && !"".equals(resp)) {
			JSONObject itfJSON = (JSONObject) JSONValue.parse(resp);
			JSONObject jsonHeader = (JSONObject) itfJSON.get("HEADER");
			
			String RESULT_CODE = jsonHeader.getAsString("RESULT_CODE"); //S - 00
			String RESULT = jsonHeader.getAsString("RESULT"); // S or F
			
			String RESULT_MESSAGE = jsonHeader.getAsString("RESULT_MESSAGE");
			if (RESULT_MESSAGE == null) RESULT_MESSAGE = "";
			
			if ("F".equals(RESULT)) { // 오류인 경우 
				if ("PCI_DTS_E3162".equals(RESULT_CODE)) {
					//"RESULT_MESSAGE": "조회된 회선 정보가 없습니다."
						code = 902;
						
					} else if ("ZNGME0007".equals(RESULT_CODE)) {
					//"RESULT_MESSAGE": "ZNGME0007|서비스번호 입력이 잘못되었으니 확인하십시오."
						code = 912;
					
					} else if ("ZINVE8101".equals(RESULT_CODE)) {
					//"RESULT_MESSAGE": "ZINVE8101|가입신청 상품[휴대폰약속번호]은 이미 사용중입니다."
						code = 901;
					
					} else {
						code = 998;
						msg = "통신사 서비스가입상태 조회가 원활하지 않아요. 잠시후 다시 해주세요.[998]";
					}
					api = null; // 자원반납
					
					// 에러 후 반환처리 
					String[] parts = RESULT_MESSAGE.split("\\|");
					if (parts.length > 1) msg = parts[1]; else msg = RESULT_MESSAGE;
					return JSONHelper.assembleResponse(code, msg);
				
			} else if ("S".equals(RESULT)) { // 정상인 경우
				api = null; // 자원반납
				code = 200; // 정상코드
				msg = "";
				JSONObject jsonBody = (JSONObject) itfJSON.get("BODY");
				String SVC_MGMT_NUM = jsonBody.getAsString("SVC_MGMT_NUM");
				logger.info("SVC_MGMT_NUM : {}", SVC_MGMT_NUM);
			} else {
				msg = "RESULT 이상 : " + RESULT + "/" + RESULT_CODE;
			}
			
		} else {
			api = null;
			return JSONHelper.assembleResponse(914, "통신사 부가서비스 가입이 정상적이 완료되지 못했어요. 콜센터(전화 1533-5278)를 통하여 문의주세요.[999]");
		}
		
		api = null;
		return JSONHelper.assembleResponse(code, msg);
	}

	//번호로 사용자 정보를 조회함 
	private JSONObject ISICS00021(Map<String, String> param, Subscribe data) {
		
		APICall api = new APICall();
		
		int code = 999;
		String msg = "";
		
		/**
		 * JSON : {"RESPONSE":
		 * 			{"HEADER":
		 * 				{"RESULT_CODE":"00",
		 * 				"RESULT":"S",
		 * 				"RESULT_MESSAGE":"서비스가 정상적으로 처리되었습니다."},
		 * 			"BODY":"암호화된 String 내용"}
		 * 		  }
		 */
		String resp = api.ISICS00021(data.getMobileno());
		if (resp != null && !"".equals(resp)) {
			JSONObject itfJSON = (JSONObject) JSONValue.parse(resp);
			JSONObject jsonHeader = (JSONObject) itfJSON.get("HEADER");
			
			String RESULT_CODE = jsonHeader.getAsString("RESULT_CODE"); //S - 00
			String RESULT = jsonHeader.getAsString("RESULT"); // S or F
			
			String RESULT_MESSAGE = jsonHeader.getAsString("RESULT_MESSAGE");
			if (RESULT_MESSAGE == null) RESULT_MESSAGE = "";
			
			if ("F".equals(RESULT)) { // 오류인 경우 
				code = 999;
				msg = "서비스가 원활하지 않습니다. 잠시 후 다시 시도해주세요.[999]";
				
				if ("PCI_DTS_E3162".equals(RESULT_CODE)) { // "조회된 회선 정보가 없습니다."
					code = 912;
					msg = RESULT_MESSAGE;
				}
				api = null; // 자원반납
				return JSONHelper.assembleResponse(code, msg);
				
			} else if ("S".equals(RESULT)) { // 정상인 경우
				api = null; // 자원반납
				code = 200; // 정상코드
				msg = "";
				JSONObject jsonBody = (JSONObject) itfJSON.get("BODY");
				String SVC_MGMT_NUM = jsonBody.getAsString("SVC_MGMT_NUM");
				String SSN_BIRTH_DT = jsonBody.getAsString("SSN_BIRTH_DT"); // 75+ 가입 확인용 (yyyyMMdd)
				logger.info("SVC_MGMT_NUM : {}, SSN_BIRTH_DT : {}", SVC_MGMT_NUM, SSN_BIRTH_DT);
				JSONObject obj = new JSONObject();
				obj.put("SVC_MGMT_NUM", SVC_MGMT_NUM);
				obj.put("SSN_BIRTH_DT", SSN_BIRTH_DT);
				api = null;
				return JSONHelper.assembleResponse(code, obj);
			} else {
				msg = "RESULT 이상 : " + RESULT + "/" + RESULT_CODE;
			}

		} else {
			api = null;
			return JSONHelper.assembleResponse(999, "서비스가 원활하지 않습니다. 잠시 후 다시 시도해주세요.[999]");
		}

		api = null;
		return JSONHelper.assembleResponse(code, msg);
	}
}
