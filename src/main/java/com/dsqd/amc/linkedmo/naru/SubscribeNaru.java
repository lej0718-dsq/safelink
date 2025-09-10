package com.dsqd.amc.linkedmo.naru;

import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.util.InterfaceManager;
import com.dsqd.amc.linkedmo.util.JSONHelper;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class SubscribeNaru {
	
	public JSONObject subscribe(Subscribe data) {
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		
		int code = 998;
		String msg = "";
		
		String method = "POST";
		String uri = "/api/v1.0/linksafe/subscribe";
		JSONObject jsonParam = new JSONObject();
		jsonParam.put("spcode", data.getSpcode());
		jsonParam.put("mobileno", data.getMobileno());
		jsonParam.put("regcd", data.getOffercode());

		JSONObject itfJSON = new JSONObject();
		try {
			itfJSON = (JSONObject) JSONValue.parse(itfMgr.sendRequest(method, uri, jsonParam.toJSONString()));
			code = Integer.parseInt(itfJSON.getAsString("code"));
			msg = itfJSON.getAsString("msg");
		} catch (Exception e) {
			return JSONHelper.assembleResponse(923, "부가서비스 가입이 원활하지 않아요. 잠시후 다시 해주세요.[923]");
		}
		
		return JSONHelper.assembleResponse(code, msg);
	}
	
	public JSONObject cancel(Subscribe data) {
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		
		int code = 998;
		String msg = "";
		
		String method = "POST";
		String uri = "/api/v1.0/linksafe/cancel";
		JSONObject jsonParam = new JSONObject();
		jsonParam.put("spcode", data.getSpcode());
		jsonParam.put("mobileno", data.getMobileno());
		jsonParam.put("status", "C");
		jsonParam.put("regcd", data.getOffercode());

		JSONObject itfJSON = new JSONObject();
		try {
			itfJSON = (JSONObject) JSONValue.parse(itfMgr.sendRequest(method, uri, jsonParam.toJSONString()));
			code = Integer.parseInt(itfJSON.getAsString("code"));
			msg = itfJSON.getAsString("msg");
		} catch (Exception e) {
			return JSONHelper.assembleResponse(923, "부가서비스 해지가 원활하지 않아요. 잠시후 다시 해주세요.[923]");
		}
		
		return JSONHelper.assembleResponse(code, msg);
	}
	
	public JSONObject getData(Subscribe data) {
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		
		int code = 998;
		String msg = "";
		
		String method = "POST";
		String uri = "/api/v1.0/linksafe/getdata";
		JSONObject jsonParam = new JSONObject();
		jsonParam.put("spcode", data.getSpcode());
		jsonParam.put("mobileno", data.getMobileno());

		JSONObject itfJSON = new JSONObject();
		try {
			itfJSON = (JSONObject) JSONValue.parse(itfMgr.sendRequest(method, uri, jsonParam.toJSONString()));
			code = Integer.parseInt(itfJSON.getAsString("code"));
		} catch (Exception e) {
			return JSONHelper.assembleResponse(923, "NARU REST 서비스 중 오류 발생: "+ e.getMessage());
		}
		
		return JSONHelper.assembleResponse(code, itfJSON);
	}
	
	/**
	 * 연결번호 변경 요청
	 * @param spcode 서비스 제공자 코드
	 * @param mobileno 휴대폰 번호
	 * @param linkno 새로운 연결번호
	 * @return 결과 코드와 메시지
	 */
	public JSONObject changeLinkno(Subscribe data) {
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		
		int code = 998;
		String msg = "";
		
		String method = "POST";
		String uri = "/api/v1.0/linksafe/changelinkno";

		JSONObject jsonParam = new JSONObject();
		jsonParam.put("spcode", data.getSpcode());
		jsonParam.put("mobileno", data.getMobileno());
		jsonParam.put("linkno", data.getLinkno());

		JSONObject itfJSON = new JSONObject();
		try {
			itfJSON = (JSONObject) JSONValue.parse(itfMgr.sendRequest(method, uri, jsonParam.toJSONString()));
			code = Integer.parseInt(itfJSON.getAsString("code"));
			msg = itfJSON.getAsString("msg");
		} catch (Exception e) {
			return JSONHelper.assembleResponse(923, "연결번호 변경 서비스 중 오류 발생: " + e.getMessage());
		}
		
		return JSONHelper.assembleResponse(code, msg);
	}
	
}
