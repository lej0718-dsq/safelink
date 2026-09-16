package com.dsqd.amc.linkedmo.mobiletown;

import java.util.Date;
import java.util.List;

import com.dsqd.amc.linkedmo.model.Mobiletown;
import com.dsqd.amc.linkedmo.service.MobiletownService;
import com.dsqd.amc.linkedmo.util.AES256Util;
import com.dsqd.amc.linkedmo.util.JSONHelper;

import net.minidev.json.JSONObject;

public class SubscribeMobiletown {
	
	// 가입시 인증번호 발송
	public JSONObject subscribeMobiletown(String mobileno, String spuserid) {
		return mobiletown("S", mobileno, spuserid);
	}
	
	// 가입시 인증번호 발송 (테스트폰전용)
	public JSONObject subscribeMobiletownPseudo(String mobileno, String spuserid) {
		return mobiletownPseuo("S", mobileno, spuserid);
	}
	
	// 가입시 인증번호 검증 
	public JSONObject subscribeMobiletownOtp(String mobileno, String rnumber) {
		return mobiletownOtp("S", mobileno, rnumber);
	}
	
	// 취소시 인증번호 발송
	public JSONObject cancelMobiletown(String mobileno, String spuserid) {
		return mobiletown("C", mobileno, spuserid);
	}

	// 취소시 인증번호 발송 (테스트폰전용)
	public JSONObject cancelMobiletownPseudo(String mobileno, String spuserid) {
		return mobiletownPseuo("C", mobileno, spuserid);
	}

	// 취소시 인증번호 검증
	public JSONObject cancelMobiletownOtp(String mobileno, String rnumber) {
		return mobiletownOtp("C", mobileno, rnumber);
	}

	// 환불시 인증번호 발송
	public JSONObject refundMobiletown(String mobileno) {
		return mobiletown("R", mobileno, "");
	}

	// 환불시 인증번호 발송 (테스트폰전용)
	public JSONObject refundMobiletownPseudo(String mobileno) {
		return mobiletownPseuo("R", mobileno, "");
	}

	// 환불시 인증번호 검증
	public JSONObject refundMobiletownOtp(String mobileno, String rnumber) {
		return mobiletownOtp("R", mobileno, rnumber);
	}

	// 가입안내 
	public JSONObject notiMobiletown(String mobileno, String offercode, String type) {
		JSONObject retObj = new JSONObject();
		String msg = "";
		mobiletownSMS api = new mobiletownSMS();
		String message = "";
		if ("101".equals(type)) {
			message = api.setMessage101(offercode);
		}
		JSONObject resp = api.sendSms(mobileno, message);
		int code = (int) resp.get("code");
		String key = resp.getAsString("key");
		msg = resp.getAsString("msg");
		
		if (code == 200) {
			MobiletownService service = new MobiletownService();
			Mobiletown mt = Mobiletown.builder().purpose("N") // 일반 안내 
									  .rcverkey(key).rcverphone(mobileno)
									  .content(message).rnumber("")
									  .result(resp.toJSONString())
									  .checkcode("S")
									  .build();
			service.insertMobiletown(mt); 
			msg = "";
		}
		retObj.put("code", code);
		retObj.put("msg", msg);
		return retObj;
	}
	
	private JSONObject mobiletown(String purpose, String mobileno, String spuserid) {
		JSONObject retObj = new JSONObject();
		String msg = "";
		mobiletownSMS api = new mobiletownSMS();
		String rnumber = api.genRandoms();
		String message = api.setMessage1(rnumber);
		JSONObject resp = api.sendSms(mobileno, message);
		int code = (int) resp.get("code");
		msg = resp.getAsString("msg");
		String key = resp.getAsString("key");
		if (code == 200) {
			MobiletownService service = new MobiletownService();
			Mobiletown mt = Mobiletown.builder().purpose(purpose)
									  .spuserid(spuserid)
									  .rcverkey(key).rcverphone(mobileno)
									  .content(message).rnumber(rnumber)
									  .result(resp.toJSONString())
									  .checkcode("T")
									  .build();
			service.insertMobiletown(mt); 
			msg = "";
		}
		retObj.put("code", code);
		retObj.put("msg", msg);
		return retObj;
	}
	
	// 모바일로 인증번호를 보내지 않음.
	private JSONObject mobiletownPseuo(String purpose, String mobileno, String spuserid) {
		JSONObject retObj = new JSONObject();
		String msg = "";
		mobiletownSMS api = new mobiletownSMS();
		//String rnumber = api.genRandoms();
		String rnumber = "999123";
		String message = "테스트를 위한 Pseudo 메시지입니다.";
		//JSONObject resp = api.sendSms(mobileno, message);
		JSONObject resp = new JSONObject();
		resp.put("code", 200);
		resp.put("msg", "테스트를 위한 Pseudo 메시지입니다.");
		resp.put("key", "0000000000000000000000000000");
		
		int code = (int) resp.get("code");
		msg = resp.getAsString("msg");
		String key = resp.getAsString("key");
		if (code == 200) {
			MobiletownService service = new MobiletownService();
			Mobiletown mt = Mobiletown.builder().purpose(purpose)
									  .spuserid(spuserid)
									  .rcverkey(key).rcverphone(mobileno)
									  .content(message).rnumber(rnumber)
									  .result(resp.toJSONString())
									  .checkcode("T")
									  .build();
			service.insertMobiletown(mt); 
			msg = "";
		}
		retObj.put("code", code);
		retObj.put("msg", msg);
		return retObj;
	}
	
	private static JSONObject mobiletownOtp(String purpose, String mobileno, String rnumber) {
		if (rnumber == null || "".equals(rnumber)) {
			return JSONHelper.assembleResponse(3109, "인증번호 입력 후 다시 해주세요.");
		}
		
		MobiletownService service = new MobiletownService();
		int code = 200;
		String msg = "";
		
		List <Mobiletown> lst = service.getMobiletownByMobileid(mobileno);
		if (lst != null && lst.size()>0) {
			Mobiletown mt = lst.get(0); // 무조건 첫번째거만

			// 시간체크
			Date send = mt.getSendtime();
			int errcnt = mt.getErrcnt();
			
			long currentTimeMillis = System.currentTimeMillis();
			long sendTimeMillls = send.getTime();
			long threeMinutesMillis = 3 * 60 * 1000 + 30 * 1000;
			if ("S".equals(mt.getCheckcode())) {
				code = 3102;
				msg= "인증번호 발송 후 검증요청을 해주세요."; 
			} else if (currentTimeMillis - sendTimeMillls > threeMinutesMillis) { 
				code = 3103;
				msg= "인증번호 검증시간은 발송 후 3분입니다. 다시 인증번호를 발송하세요."; 
			} else if (errcnt > 3) { 
				code = 3104;
				msg= "인증번호 검증시도 5회 초과입니다. 다시 인증번호를 발송하세요."; 
			} else if (!purpose.equals(mt.getPurpose())) {
				code = 3105;
				msg= "인증번호 발송 후 검증요청을 해주세요.[3105]"; 
			} else {
				String rnumber_send = mt.getRnumber();
				if (rnumber.equals(rnumber_send)) {
					mt.setCheckcode("S"); 
					service.updateMobiletown(mt);
					code = 200;
					msg= "인증번호를 확인했습니다.["+rnumber+"]"; 
				} else {
					mt.setErrcnt(errcnt++);
					service.updateMobiletown(mt);
					code = 3101;
					msg= "인증번호가 틀립니다.[" + errcnt + "회] 확인 후 다시 입력해보세요."; 
				}
			}
			
		} else {
			code = 3106;
			msg= "인증번호 발송 후 검증요청을 해주세요."; 
		}
		
		if (code == 200) {
			String checkcode="";
			try {
				checkcode = AES256Util.encrypt(mobileno +"|"+rnumber+"|"+System.currentTimeMillis());
			} catch (Exception e) {
				e.printStackTrace();
			}

			JSONObject obj = new JSONObject();
			obj.put("msg", msg);
			obj.put("checkcode", checkcode);
			return JSONHelper.assembleResponse(code, obj);
		} else {
			return JSONHelper.assembleResponse(code, msg);
		}
	}
}
