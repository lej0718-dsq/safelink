package com.dsqd.amc.linkedmo;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.stop;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.dsqd.amc.linkedmo.controller.*;
import com.dsqd.amc.linkedmo.model.Mobilians;
import com.dsqd.amc.linkedmo.service.CouponService;
import com.dsqd.amc.linkedmo.service.MobiliansService;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.io.Resources;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dsqd.amc.linkedmo.batch.DataCleanser;
import com.dsqd.amc.linkedmo.buzzvil.Event12;
import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.mobiletown.mobiletownSMS;
import com.dsqd.amc.linkedmo.model.Mobiletown;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.naru.SubscribeNaru;
import com.dsqd.amc.linkedmo.service.AdminService;
import com.dsqd.amc.linkedmo.service.MobiletownService;
import com.dsqd.amc.linkedmo.skt.APICall;
import com.dsqd.amc.linkedmo.util.AES256Util;
import com.dsqd.amc.linkedmo.util.ApiExcludeList;
import com.dsqd.amc.linkedmo.util.InterfaceManager;
import com.dsqd.amc.linkedmo.util.JSONHelper;
import com.dsqd.amc.linkedmo.util.JwtUtil;
import com.dsqd.amc.linkedmo.util.SchedulerModule;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import spark.Request;
import spark.Response;

public class RestServer {
	
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private static String env;
    
	public static void main(String [] args) {
		
		env = "local";
        if (args.length != 1) {
            System.err.println("Usage: java -jar RestServer <env>");
            System.err.println("   Running with local");
            System.exit(1);
        } else {
        	env = args[0];
        	System.setProperty("argEnv", env);
        }
        logger.info("");
        logger.info("");
        logger.info("===========================================================");
        logger.info("Starting the REST server... [" + env + "]");
        logger.info("===========================================================");
        logger.info("");

        MyBatisConfig.init(env);
        logger.info("===========================================================");

        if ("local".equals(env)) {
//		staticFiles.externalLocation("/Users/eunjun/Documents/dsqf/AMCProject/public2"); // Static files
        	//staticFiles.externalLocation("C:\\Users\\silve\\git\\safelink\\public2"); // Static files
			staticFiles.externalLocation("/Users/nurier/project/safelink/public2"); // Static files YangSeyong
        }

        GlobalCache cache = GlobalCache.getInstance();
        Properties properties = new Properties();
        try {
			properties.load(Resources.getResourceAsStream("application.properties"));
			cache.initWithProperties(properties);
			//cache.logCacheContents();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("FAIL Initialize GlobalCache : {}", e.getMessage());
		}
        
		int port = 5000;
		port(port);

		before((request, response) -> {
            String path = request.pathInfo();
            String clientIP = request.ip();
            logger.info("ip:{} - path:{}", clientIP, path);
            
//            if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || ApiExcludeList.isExcluded(path)) {
//                return; // 특정 API와 파일확장자는 JWT 검증을 제외
//            }
            
            if (ApiExcludeList.isExcluded(path)) {
            	logger.debug("excluded path : {}", path);
                return; // 특정 API와 파일확장자는 JWT 검증을 제외
            }
            

            String token = request.headers("Authorization");
            logger.debug("REQEUST AUTHORIZATION : {}", token);
            if (token == null || token.isEmpty()) {
                //response.redirect("/login.html");
                //halt();
                halt(401, "You are not authorized");
            } else {
            	// Bearer Token 추출 (JWT)
            	if (token.length()>10) {
            		token = token.substring(7);
            	} else {
            		logger.error("WRONG TOKEN : []", token);
            	}
            }
            
            try {
                DecodedJWT decodedJWT = JwtUtil.verifyToken(token);
                //String clientIP = request.ip();
                String tokenClientIP = JwtUtil.getClientIP(decodedJWT);
                String tokenUsername = JwtUtil.getUsername(decodedJWT);
                String tokenKorname = JwtUtil.getKorname(decodedJWT);
                String tokenGroup = JwtUtil.getGroup(decodedJWT);

                if (!clientIP.equals(tokenClientIP)) {
                    halt(401, "Token IP mismatch");
                }

                // Token is valid, generate new token
                String newToken = JwtUtil.createToken(clientIP, tokenUsername, tokenKorname, tokenGroup);
                response.header("Authorization", "Bearer " + newToken);
            } catch (JWTVerificationException e) {
                halt(401, "Token is not valid or IP mismatch");

            } catch (Exception e) {
                halt(401, "Unexpected situation");
            }
        });
		
		//get("/api/v1.0/test/summarynet", (req, res) -> adminSummaryNet(req, res));
		
		get("/api/v2.0/test/buzzvil/:mobileno/:bz_tracking_id", (req, res) -> testBuzzvil(req, res));
		
		get("/api/v2.0/test/skt", (req, res) -> "Usage /api/v2.0/test/01012341234");
		
		get("/api/v2.0/test/skt/:mobileno", (req, res) -> ISICS00021(req, res));
		get("/api/v2.0/test/skt/:mobileno/on", (req, res) -> ISSWG00047_on(req, res));
		get("/api/v2.0/test/skt/:mobileno/off", (req, res) -> ISSWG00047_off(req, res));
		get("/api/v2.0/test/skt/:mobileno/status", (req, res) -> ISICS00022(req, res));
		
		get("/api/v2.0/test/naru/:spcode/:mobileno", (req, res) -> adminNaruGetData(req, res));
		get("/api/v2.0/test/naru/:spcode/:mobileno/on", (req, res) -> adminNaruSubscribe(req, res));
		get("/api/v2.0/test/naru/:spcode/:mobileno/off", (req, res) -> adminNaruCancel(req, res));
		
		get("/api/v2.0/test/sms/change/:mobileno", (req, res) -> changeTestmobileno(req, res));
		get("/api/v2.0/test/sms/:mobileno", (req, res) -> mobiletown(req, res));
		get("/api/v2.0/test/sms/:mobileno/:rnumber", (req, res) -> mobiletownOtp(req, res));
		//개발 테스트 - 양세용
		get("/api/v2.0/test/coupon", (req, res) -> couponRequest(req));
		get("/api/v2.0/test/testCoupon", (req, res) -> couponRequestTest(req));
		get("/api/v2.0/test/mobiliansAuto", (req, res) -> mobiliansAuto());
		get("/api/v2.0/test/pgCancel", (req, res) -> pgCancel());

		try {
			String encdata = AES256Util.encrypt("safelinkd&07", "12345678901234567890123456789012");
			//System.out.println("AES : " + encdata);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//DataController.main(args);
		new DataController();
		new SubscribeController();
		new LoginController();
		new SchedulerController();
		new BoardController();
		new AdminController();
		new PartnerController();
		new EventController();
		new QRImageController();
		new VerifiedController();

		SchedulerModule.startScheduler();
		
	}
	
	
	
	// 테스트용
	
	private static String adminSummaryNet(Request req, Response res) {
		// Admin Controller로 이동되었음 
		return "";
	}
	
	private static String adminNaruGetData(Request req, Response res) {
		String spcode = req.params(":spcode");
		String mobileno = req.params(":mobileno");
		Subscribe s = Subscribe.builder().spcode(spcode).mobileno(mobileno).build();
		SubscribeNaru sn = new SubscribeNaru();
		JSONObject retJson = sn.getData(s);
		return retJson.toJSONString();
	}
	
	private static String adminNaruCancel(Request req, Response res) {
		String spcode = req.params(":spcode");
		String mobileno = req.params(":mobileno");
		Subscribe s = Subscribe.builder().spcode(spcode).mobileno(mobileno).build();
		SubscribeNaru sn = new SubscribeNaru();
		JSONObject retJson = sn.cancel(s);
		// 직접 DB에 내용을 기입해야 함
		return retJson.toJSONString();
	}
	
	private static String adminNaruSubscribe(Request req, Response res) {
		String spcode = req.params(":spcode");
		String mobileno = req.params(":mobileno");
		Subscribe s = Subscribe.builder().spcode(spcode).mobileno(mobileno).build();
		SubscribeNaru sn = new SubscribeNaru();
		JSONObject retJson = sn.subscribe(s);
		// 직접 DB에 내용을 기입해야 함
		return retJson.toJSONString();
	}

	private static String testBuzzvil(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		String bz_tracking_id = req.params(":bz_tracking_id");
		String eventcd = "119577650586064";

		Event12 event = new Event12();
        
		event.buzzSafelink(mobileno, bz_tracking_id, mobileno, eventcd, "S2S");
		
		return "testBuzzvil : " + mobileno;
	}
	
	private static String changeTestmobileno(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		itfMgr.setTestMobileno(mobileno);
		return "change testmobileno : " + mobileno;
	}
	
	private static String mobiletown(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		mobiletownSMS api = new mobiletownSMS();
		String rnumber = api.genRandoms();
		String message = api.setMessage1(rnumber);
		JSONObject resp = api.sendSms(mobileno, message);
		int code = (int) resp.get("code");
		if (code == 200) {
			MobiletownService service = new MobiletownService();
			Mobiletown mt = Mobiletown.builder().purpose("S")
									  .spuserid("0000000000")
									  .rcverkey("1").rcverphone(mobileno)
									  .content(message).rnumber(rnumber)
									  .result(resp.toJSONString())
									  .checkcode("T")
									  .build();
			service.insertMobiletown(mt); 
		}
		return "SEND SMS by MOBILETOWN <br/>" + resp;
	}
	
	private JSONObject subscribeMobiletown(String mobileno, String spuserid) {
		return mobiletown("S", mobileno, spuserid);
	}
	
	private JSONObject cancelMobiletown(String mobileno, String spuserid) {
		return mobiletown("C", mobileno, spuserid);
	}
	
	private JSONObject mobiletown(String purpose, String mobileno, String spuserid) {
		JSONObject retObj = new JSONObject();
		String msg = "";
		mobiletownSMS api = new mobiletownSMS();
		String rnumber = api.genRandoms();
		String message = api.setMessage1(rnumber);
		JSONObject resp = api.sendSms(mobileno, message);
		int code = (int) resp.get("code");
		if (code == 200) {
			MobiletownService service = new MobiletownService();
			Mobiletown mt = Mobiletown.builder().purpose(purpose)
									  .spuserid(spuserid)
									  .rcverkey("1").rcverphone(mobileno)
									  .content(message).rnumber(rnumber)
									  .result(resp.toJSONString())
									  .checkcode("T")
									  .build();
			service.insertMobiletown(mt); 
		}
		retObj.put("code", code);
		retObj.put("msg", "");
		return retObj;
	}
	
	private JSONObject subscribeMobiletownOtp(String mobileno, String rnumber) {
		return mobiletownOtp("S", mobileno, rnumber);
	}
	
	private JSONObject cancelMobiletownOtp(String mobileno, String rnumber) {
		return mobiletownOtp("C", mobileno, rnumber);
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
				
		return JSONHelper.assembleResponse(code, msg);
	}
	
	
	private static String mobiletownOtp(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		String rnumber = req.params(":rnumber");
		if (rnumber == null || "".equals(rnumber)) {
			return "인증번호를 입력하십시오. /api/1.0/testsms/{전화번호}/{인증번호}";
		}
		
		MobiletownService service = new MobiletownService();
		String ret = "";
		
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
				ret= "인증번호 발송 후 검증요청을 해주세요."; 
			} else if (currentTimeMillis - sendTimeMillls > threeMinutesMillis) { 
				ret= "인증번호 검증시간은 발송 후 3분입니다. 다시 인증번호를 발송하세요."; 
			} else if (errcnt > 3) {
				ret= "인증번호 검증시도 5회 초과입니다. 다시 인증번호를 발송하세요."; 
			} else {
				String rnumber_send = mt.getRnumber();
				if (rnumber.equals(rnumber_send)) {
					mt.setCheckcode("S");
					service.updateMobiletown(mt);
					ret= "인증번호를 확인했습니다.["+rnumber+"]"; 
				} else {
					mt.setErrcnt(errcnt++);
					service.updateMobiletown(mt);
					ret= "인증번호가 틀립니다.[" + errcnt + "회] 확인 후 다시 입력해보세요."; 
				}
			}
			
		} else {
			ret= "인증번호 발송 후 검증요청을 해주세요."; 
		}
				
		return "CHECK OTP by MOBILETOWN <br/>" + ret;
	}
	
	private static String ISICS00021(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		APICall api = new APICall(env);
		String resp = api.ISICS00021(mobileno);
		return resp;
	}
	
	private static String ISICS00022(Request req, Response res) {
		String mobileno = req.params(":mobileno");
		APICall api = new APICall(env);
		String resp = api.ISICS00022(mobileno);
		return resp;
	}
	
	private static String ISSWG00047(Request req, Response res) {
		APICall api = new APICall(env);
		
		// Map을 복제함 
		Map<String, String> param = new HashMap<String, String>();
		for (String key: req.queryParams()) {
			param.put(key, (String) req.queryParams(key));
		}
		
		// 테스트용 파라미터값 등록 
		String mobileno = req.params(":mobileno");
		if (mobileno != null && !"".equals(mobileno)) {
			param.put("mobileno", mobileno);
		}
		
		// 추가적으로 attribute에 action이 있는지 확인하여 Map에 추가함 
		String action = req.attribute("action");
		if ("subscribe".equals(action) || "cancel".equals(action)) {
			param.put("action", action);
		}
		
		String resp = api.ISSWG00047(param);
		return resp;
	}
	
	private static String ISSWG00047_on(Request req, Response res) {
		req.attribute("action", "subscribe");
		return ISSWG00047(req, res);
	}
	
	private static String ISSWG00047_off(Request req, Response res) {
		req.attribute("action", "cancel");
		return ISSWG00047(req, res);
	}
	
	private static String dataclean(Request req, Response res) {
		DataCleanser dc = new DataCleanser();
		dc.batch();
		return "OK";
	}

	private static String couponRequest(Request req) {
		CouponService couponService = new CouponService();
		//couponService.sendCouponRequest();

		LocalDate today = LocalDate.now();
		HashMap<String,Object> params = new HashMap<>();

		Properties properties = new Properties();
		try {
			properties.load(Resources.getResourceAsStream("application.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			// POST 요청을 보낼 URL 설정
			HttpPost httpPost = new HttpPost(properties.getProperty("coupon.server"));

			String mobileNumber = req.queryParams("mobileNumber");
			// 전송할 파라미터 설정 (x-www-form-urlencoded 형식)
			List<NameValuePair> requestParams = new ArrayList<>();
			requestParams.add(new BasicNameValuePair("ACTION", "CI102_ISSUECPN_TITLE_WITHPAY"));
			requestParams.add(new BasicNameValuePair("COOPER_ID", properties.getProperty("coupon.cooperid")));
			requestParams.add(new BasicNameValuePair("COOPER_PW", properties.getProperty("coupon.cooperpw")));
			requestParams.add(new BasicNameValuePair("SITE_ID", properties.getProperty("coupon.siteid")));
			requestParams.add(new BasicNameValuePair("NO_REQ", properties.getProperty("coupon.noreq")));
			requestParams.add(new BasicNameValuePair("COOPER_ORDER", mobileNumber+new Date().getTime()));
			requestParams.add(new BasicNameValuePair("ISSUE_COUNT", "1"));
			requestParams.add(new BasicNameValuePair("CALL_CTN", properties.getProperty("coupon.callctn")));
			requestParams.add(new BasicNameValuePair("RCV_CTN", mobileNumber));
			requestParams.add(new BasicNameValuePair("SEND_MSG", "휴대폰약속번호 서비스 신규가입 이벤트"));
			requestParams.add(new BasicNameValuePair("VALID_START", today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
			requestParams.add(new BasicNameValuePair("VALID_END", today.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
			requestParams.add(new BasicNameValuePair("PAY_ID","1"));
			requestParams.add(new BasicNameValuePair("BOOKING_NO","1"));
			requestParams.add(new BasicNameValuePair("SITE_URL","1"));
			requestParams.add(new BasicNameValuePair("TITLE","[휴대폰약속번호 서비스 신규가입 이벤트]"));

			for (NameValuePair pair : requestParams) {
				// 동일한 키가 여러 번 등장하면 마지막 값이 저장됩니다.
				params.put(pair.getName(), pair.getValue());
			}

			// 모바일 쿠폰 발송 API 요청 파라미터 저장
			couponService.insertCouponRequest(params);

			// 파라미터를 UrlEncodedFormEntity로 인코딩 (UTF-8)
			httpPost.setEntity(new UrlEncodedFormEntity(requestParams, "UTF-8"));

			// POST 요청 실행 및 응답 받기
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				// 응답 상태 코드 출력
				System.out.println("Response Code: " + response.getStatusLine().getStatusCode());

				// 응답 본문 처리
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String responseBody = EntityUtils.toString(entity, "UTF-8");
					couponService.insertCouponResponse(couponService.xmlParse(responseBody));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private static String couponRequestTest(Request req) {
		CouponService couponService = new CouponService();
		//couponService.sendCouponRequest();

		LocalDate today = LocalDate.now();
		HashMap<String,Object> params = new HashMap<>();

		Properties properties = new Properties();
		try {
			properties.load(Resources.getResourceAsStream("application.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			// POST 요청을 보낼 URL 설정
			HttpPost httpPost = new HttpPost("https://stg-atom.donutbook.co.kr/b2ccoupon/b2cService.aspx");

			String mobileNumber = req.queryParams("mobileNumber");
			// 전송할 파라미터 설정 (x-www-form-urlencoded 형식)
			List<NameValuePair> requestParams = new ArrayList<>();
			requestParams.add(new BasicNameValuePair("ACTION", "CC02_DOWN_SINGLE_GOODSINFO"));
			requestParams.add(new BasicNameValuePair("COOPER_ID", "SC2319"));
			requestParams.add(new BasicNameValuePair("COOPER_PW", "ogxg79!@"));
			requestParams.add(new BasicNameValuePair("SITE_ID", "10003466"));
			requestParams.add(new BasicNameValuePair("NO_REQ", "999999"));

			for (NameValuePair pair : requestParams) {
				// 동일한 키가 여러 번 등장하면 마지막 값이 저장됩니다.
				params.put(pair.getName(), pair.getValue());
			}

			// 모바일 쿠폰 발송 API 요청 파라미터 저장
			//couponService.insertCouponRequest(params);

			// 파라미터를 UrlEncodedFormEntity로 인코딩 (UTF-8)
			httpPost.setEntity(new UrlEncodedFormEntity(requestParams, "UTF-8"));

			// POST 요청 실행 및 응답 받기
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				// 응답 상태 코드 출력
				System.out.println("Response Code: " + response.getStatusLine().getStatusCode());

				// 응답 본문 처리
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String responseBody = EntityUtils.toString(entity, "UTF-8");
					System.out.println(responseBody);
					//couponService.xmlParse(responseBody);
					//couponService.insertCouponResponse(couponService.xmlParse(responseBody));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private static String mobiliansAuto() {
		MobiliansService mobiliansService = new MobiliansService();
		mobiliansService.autoPayBatch();
		return "";
	}

	private static String pgCancel() {
		MobiliansService mobiliansService = new MobiliansService();
		Mobilians m = new Mobilians();
		/*m.setUserkey("uk1076500250317");
		m.setAutobillkey("bk2091079250317");
		m.setMrchid("24110815");
		m.setSvcid("241108151152");
		m.setTradeid("010308334801742176369559");
		m.setSigndate("20250317105449");
		m.setMobilid("3049701902");

		mobiliansService.cancel(m);*/
		return "";
	}
}
