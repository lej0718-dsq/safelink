package com.dsqd.amc.linkedmo.controller;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.dsqd.amc.linkedmo.model.Mobilians;
import com.dsqd.amc.linkedmo.service.MobiliansService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.parser.JSONParser;
import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.mobiletown.SubscribeMobiletown;
import com.dsqd.amc.linkedmo.model.Blocknumber;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.naru.SubscribeNaru;
import com.dsqd.amc.linkedmo.service.BlocknumberService;
import com.dsqd.amc.linkedmo.service.SubscribeService;
import com.dsqd.amc.linkedmo.skt.SubscribeSK;
import com.dsqd.amc.linkedmo.util.AES256Util;
import com.dsqd.amc.linkedmo.util.InterfaceManager;
import com.dsqd.amc.linkedmo.util.JSONHelper;
import com.dsqd.amc.linkedmo.util.TestMobileno;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

public class SubscribeController {
	private static final Logger logger = LoggerFactory.getLogger(SubscribeController.class);
	private SubscribeService service = new SubscribeService();
	private InterfaceManager itfMgr = InterfaceManager.getInstance();
	private MobiliansService mobiliansService = new MobiliansService();

	public SubscribeController() {
		setupEndpoints();
	}

	private void setupEndpoints() {
		path("/api", () -> {
			path("/v1.0", () -> {
				// 연결번호 변경 API
				post("/changelinkno", (req, res) -> {
					try {
						JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
						logger.info("ChangeLinkno Request: {}", jsonObject.toJSONString());
						logger.info(jsonObject.toJSONString());
						Subscribe data = JSONValue.parse(req.body(), Subscribe.class);

						String spcode = jsonObject.getAsString("spcode");
						String mobileno = jsonObject.getAsString("mobileno");
						String linkno = jsonObject.getAsString("linkno");
						
						// 기본적인 null 체크만 수행 (비즈니스 검증은 Naru API에서 처리)
						if (spcode == null || mobileno == null || linkno == null) {
							logger.warn("Required parameters missing - spcode: {}, mobileno: {}, linkno: {}", 
								spcode, mobileno, linkno);
							return JSONHelper.assembleResponse(999, "필수 파라미터가 누락되었습니다.");
						}
						
						// NARU API 호출하여 결과를 그대로 반환
						SubscribeNaru naru = new SubscribeNaru();
						JSONObject response = naru.changeLinkno(data);
						
						logger.info("ChangeLinkno Response: {}", response.toJSONString());
						res.type("application/json");
						return response.toJSONString();
						
					} catch (Exception e) {
						logger.error("ChangeLinkno API Error: ", e);
						return JSONHelper.assembleResponse(999, "서비스 처리 중 오류가 발생했습니다.");
					}
				});
				
				// 콜센터 등 관리자용
				path("/admin", () -> {
					// 전체 가입자 정보 조회
					get("/subscribe", (req, res) -> {
						int code = 999;
						String msg = "";
						JSONObject responseJSON = new JSONObject();
						List<Subscribe> data = service.getSubscribeAll();
						if (data != null && data.size() > 0) {
							logger.info("All Subscribe Data retrieved: count {}", data.size());
							res.status(200);

							code = 200;
							msg = "정상 처리";
							JSONArray arry = new JSONArray();
							for (Subscribe s : data)
								arry.add(s);

							responseJSON.put("data", arry);

						} else {
							logger.warn("No Subscribe Data");
							res.status(404);
							return "Data not found";
						}
						// 문제가 없다면 정상코드 제공
						responseJSON.put("code", code);
						responseJSON.put("msg", msg);

						return responseJSON.toJSONString();
					});

					// Get data by ID
					get("/subscribe/:id", (req, res) -> {
						int id = Integer.parseInt(req.params(":id"));
						Subscribe data = service.getSubscribeById(id);
						if (data != null) {
							logger.info("Data retrieved for ID: {}", id);
							res.status(200);
							return JSONValue.toJSONString(data);
						} else {
							logger.warn("Data not found for ID: {}", id);
							res.status(404);
							return "Data not found";
						}
					});

					// Update existing data
					put("/subscribe/:id", (req, res) -> {
						int id = Integer.parseInt(req.params(":id"));
						JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
						Subscribe data = JSONValue.parse(req.body(), Subscribe.class);
						data.setId(id);
						service.updateSubscribe(data);
						logger.info("Data updated for ID: {}", id);
						res.status(200);
						return JSONValue.toJSONString(data);
					});

					// Delete data by ID
					delete("/subscribe/:id", (req, res) -> {
						int id = Integer.parseInt(req.params(":id"));
						service.deleteSubscribe(id);
						logger.info("Subscribe deleted for ID: {}", id);
						res.status(204);
						return "";
					});

					// 해지자목록 조회
					get("/cancel", (req, res) -> {
						int code = 999;
						String msg = "";
						JSONObject responseJSON = new JSONObject();
						List<Subscribe> data = service.getCancelList();
						if (data != null && data.size() > 0) {
							logger.info("All Cancel Data retrieved: count {}", data.size());
							res.status(200);

							code = 200;
							msg = "정상 처리";
							JSONArray arry = new JSONArray();
							for (Subscribe s : data)
								arry.add(s);

							responseJSON.put("data", arry);

						} else {
							logger.warn("No Cancel Data");
							res.status(404);
							return "Data not found";
						}
						// 문제가 없다면 정상코드 제공
						responseJSON.put("code", code);
						responseJSON.put("msg", msg);

						return responseJSON.toJSONString();
					});

				});





				// 사용자 홈페이지
				path("/subscribe", () -> {
					// 가입자 등록
					post("", (req, res) -> {
						int code = 999;
						String msg = "";
						String sp_uid = "";

						JSONObject responseJSON = new JSONObject();

						JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
						logger.info(jsonObject.toJSONString());
						Subscribe data = JSONValue.parse(req.body(), Subscribe.class);
						String mobileno = data.getMobileno();

						req.session().attribute("allonePhoneNumber", mobileno);

						// checkcode의 전화번호와 요청한 전화번호가 같은지 확인
						String encCheckcode = data.getCheckcode();
						if (encCheckcode != null && !"".equals(encCheckcode)) {
							try {
								String checkcode = AES256Util.decrypt(encCheckcode);
								//logger.info("checkcode:{}", checkcode);
								String[] codes = checkcode.split("\\|");
								if (codes.length > 2) {
									String cmobileno = codes[0];
									// 개발서버에서는 체크하지 말것
									if ((System.getProperty("argEnv")).equals("dev")) cmobileno = data.getMobileno();
									if (!cmobileno.equals(data.getMobileno())) {
										logger.warn("cmobileno:{} - data.getMobileno():{}", cmobileno, data.getMobileno());
										return JSONHelper.assembleResponse(951, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[951]");
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
								logger.error("CHECK CODE 검증시 오류 : [{}]", e.getMessage());
								return JSONHelper.assembleResponse(952, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[952]");
							}
						} else {
							return JSONHelper.assembleResponse(952, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[953]");
						}

						//기존에 동일한 휴대전화번호가 있는지 확인함
						List<Subscribe> checkDup = service.getSubscribeByMobileno(data);

						//오늘 가입한 적이 있는 휴대전화번호인지 확인함
						List<Subscribe> todayMobileno = service.getTodaySubscribeByMobileno(data);

						// TEST 전용폰 확인하여 당일해지자 가입방지 로직 Skeip
						TestMobileno tm = new TestMobileno();

						if (checkDup.size() > 0) {
							return JSONHelper.assembleResponse(901, "이미 가입된 전화번호입니다.[901]");

						} else if (todayMobileno.size() > 0 && !tm.isTestphone(data.getMobileno())) { // 오늘 가입한적이 있음
							return JSONHelper.assembleResponse(902, "입력하신 전화번호는 해지관련 전산처리 중으로 내일 가입이 가능합니다.[902]");

						} else { // 정상 가입 프로세스 시작

							// ========================================================
							// 통신사로 부가서비스 가입요청 -- START
							// ========================================================

							if ("SKT".equals(data.getSpcode())) { // 통신사 코드로 분기처리
								SubscribeSK skt = new SubscribeSK();
								responseJSON = skt.user(data); // 사용자가 있는지 확인
								if ((int) responseJSON.get("code") != 200)
									return responseJSON;
								else
									data.setSpuserid(responseJSON.getAsString("SVC_MGMT_NUM"));

								responseJSON = skt.subscribe(data);

							} else if ("KTF".equals(data.getSpcode())) {

							} else if ("LGT".equals(data.getSpcode())) {

							} else { // 통신사 코드에 이상것이 들어왔을 때
								return JSONHelper.assembleResponse(997, "통신사를 선택하여야 합니다.[997]");
							}
							// 정상완료가 되었다면 code=200 이어야 함
							if ((int) responseJSON.get("code") != 200)
								return responseJSON;

							// ========================================================
							// 통신사로 부가서비스 가입요청 -- END
							// ========================================================


							// ========================================================
							// 부가서비스 제공사로 가입요청 -- START
							// ========================================================
							SubscribeNaru naru = new SubscribeNaru();
							responseJSON = naru.subscribe(data);
							if ((int) responseJSON.get("code") != 200)
								return responseJSON;
							// ========================================================
							// 부가서비스 제공사로 가입요청 -- END
							// ========================================================


							// ========================================================
							// 자체 DB 저장 -- START
							// ========================================================

							if ((int) responseJSON.get("code") == 200) {
								try {
									service.insertSubscribe(data);
									logger.info("Subscribe inserted: {}", data.toString());
									res.status(201);
									code = 200;
									msg = "정상 가입";

									// SVC_MGMT_NUM 업데이트 =>> 시간이 걸려서 바로 처리는 안됨
//									Thread.sleep(3000);
//									SubscribeSK skt = new SubscribeSK();
//									responseJSON = skt.confirm(data);

								} catch (Exception e) {
									e.printStackTrace();
									code = 999;
									msg = "정상적으로 가입이 처리되지 못했습니다.[999]";
								}
							} else {
								code = (int) responseJSON.get("code");
								msg = responseJSON.getAsString("msg");
							}

							// ========================================================
							// 자체 DB 저장 -- END
							// ========================================================
						}

						return JSONHelper.assembleResponse(code, msg);
					});

					path("/mobiletown", () -> {
						// 가입시 SMS 문자발송
						post("/sendsms", (req, res) -> {
							int code = 999;
							String msg = "";

							JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
							logger.info(jsonObject.toJSONString());
							String mobileno = jsonObject.getAsString("mobileno");
							String spcode = jsonObject.getAsString("spcode");

							// 가입이 가능한 사용자인지 확인 2024-12-15 =========
							BlocknumberService bsvc = new BlocknumberService();
							Blocknumber bn = bsvc.getBlocknumberByMobileno(mobileno);
							logger.info("Check Block Number : {}", mobileno);

							if (bn != null) {
								logger.info("Block Number Found : [{}]", mobileno);
								code = 933;
								msg = "가입이 제한된 번호에요. 콜센터로 문의해주세요.";
								return JSONHelper.assembleResponse(code, msg);
							}

							// ===================================================
							// (SKT), (KT,LGU) 인증 번호 요청 분기 처리
							// KT,LGU 통신사는 PG사에서 소액 결제 프로세스로 처리됨에 따라 통신사 검증 하지 않고 문자 인증으로 번호만 검증
							Subscribe data = Subscribe.builder().mobileno(mobileno).build();

							if (jsonObject.getAsString("spcode").equals("SKT")) {
								SubscribeSK skt = new SubscribeSK();
								JSONObject responseJSON = skt.user(data); // 사용자가 있는지 확인
								if ((int) responseJSON.get("code") != 200)
									return responseJSON;
								else
									data.setSpuserid(responseJSON.getAsString("SVC_MGMT_NUM"));
							}

							SubscribeMobiletown smt = new SubscribeMobiletown();
							JSONObject json = new JSONObject();
							TestMobileno tm = new TestMobileno();

							if (tm.isTestphone(mobileno)) {
								// 특정 핸드폰의 경우에는 SMS를 발송하지 않고 저장함
								json = smt.subscribeMobiletownPseudo(mobileno, data.getSpuserid());

							} else {
								// 개발서버일 경우에는 SMS를 다른 폰으로 쏜다. ===================
								//if ((System.getProperty("argEnv")).equals("dev")) mobileno = itfMgr.getTestMobileno();
								// ===============================================================
								json = smt.subscribeMobiletown(mobileno, data.getSpuserid());
							}

							if (200 == (int) json.get("code")) {
								code = 200;
								msg = "";
							} else {
								code = (int) json.get("code");
								msg = json.getAsString("msg");
							}
							return JSONHelper.assembleResponse(code, msg);
						});

						//
						post("/checkotp", (req, res) -> {
							int code = 999;
							String msg = "";

							JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
							logger.info(jsonObject.toJSONString());

							String mobileno = jsonObject.getAsString("mobileno");
							String rnumber = jsonObject.getAsString("rnumber");

							SubscribeMobiletown smt = new SubscribeMobiletown();
							TestMobileno tm = new TestMobileno();

							if (tm.isTestphone(mobileno)) {

							} else {
								// 개발서버일 경우에는 SMS를 다른 폰으로 쏜다. ===================
								//if ((System.getProperty("argEnv")).equals("dev")) mobileno = itfMgr.getTestMobileno();
								// ===============================================================
							}

							JSONObject json = smt.subscribeMobiletownOtp(mobileno, rnumber);
							if (200 == (int) json.get("code")) {
								json.put("code", 200);
								json.put("msg", "");
							} else {
								json.put("code", (int) json.get("code"));
								json.put("msg", json.getAsString("msg"));
							}

							return json;
						});
					});

				});

				path("/cancel", () -> {
					// 사용자의 해지요청 처리
					post("", (req, res) -> {
						int code = 999;
						String msg = "";
						JSONObject responseJSON = new JSONObject();

						JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
						logger.info(jsonObject.toJSONString());
						Subscribe data = JSONValue.parse(req.body(), Subscribe.class);
						logger.info(data.toString());

						List<Subscribe> targets = service.getSubscribeByMobileno(data);
						// ========================================================
						// PG 작업 -- Start
						// ========================================================
						// KG모빌리언스 결제 정보 조회 2025.02.11 양세용
						List<Mobilians> mobiliansList = mobiliansService.getMobiliansPhoneUser(data);
						// ========================================================
						// PG 작업 -- End
						// ========================================================
						if (targets != null && targets.size() > 0) {

							// ========================================================
							// 통신사로 부가서비스 해지요청 -- START
							// ========================================================

							if ("SKT".equals(data.getSpcode())) { // 통신사 코드로 분기처리
								SubscribeSK skt = new SubscribeSK();
								responseJSON = skt.cancel(data);

							} else if ("KTF".equals(data.getSpcode())) {

							} else if ("LGT".equals(data.getSpcode())) {

							} else { // 통신사 코드에 이상것이 들어왔을 때
								return JSONHelper.assembleResponse(997, "통신사를 선택하여야 합니다.[997]");
							}
							// 정상완료가 되었다면 code=200 이어야 함
							//if ((int) responseJSON.get("code") != 200)
							// PG결제 관련 SKT만 해당. 2025.02.11 양세용
							if ("SKT".equals(data.getSpcode()) && (int) responseJSON.get("code") != 200)
								return responseJSON;

							// ========================================================
							// 통신사로 부가서비스 해지요청 -- END
							// ========================================================

							// ========================================================
							// 부가서비스 제공사로 가입요청 -- START
							// ========================================================
							SubscribeNaru naru = new SubscribeNaru();
							responseJSON = naru.cancel(data);
							if ((int) responseJSON.get("code") != 200)
								return responseJSON;
							// ========================================================
							// 부가서비스 제공사로 가입요청 -- END
							// ========================================================


							// ========================================================
							// 자체 DB 저장 -- START
							// ========================================================

							if ((int) responseJSON.get("code") == 200) {
								try {
									int dcnt = 0;
									for (Subscribe d : targets) {
										service.deleteSubscribe(d.getId());
										dcnt++;
										logger.info("[{}] Cancel Service - ID:{} | MOBILENO:{}", dcnt, d.getId(), d.getMobileno());
									}
									// ========================================================
									// PG 작업 -- Start
									// ========================================================
									for (Mobilians d : mobiliansList) {
										mobiliansService.updateMobiliansPhoneUser(d);
										mobiliansService.cancel(d);
									}
									// ========================================================
									// PG 작업 -- End
									// ========================================================
									res.status(201);
									code = 200;
									msg = "정상 해지";

								} catch (Exception e) {
									e.printStackTrace();
									code = 999;
									msg = "해지 중 오류가 발생하였습니다. [999]";
								}
							} else {
								code = (int) responseJSON.get("code");
								msg = responseJSON.getAsString("msg");
							}

							// ========================================================
							// 자체 DB 저장 -- END
							// ========================================================

						} else {
							code = 912;
							msg = "가입정보를 찾을 수 없습니다.[912]";
						}

						return JSONHelper.assembleResponse(code, msg);
					});

					path("/mobiletown", () -> {
						// 해지시 SMS 문자발송
						post("/sendsms", (req, res) -> {
							int code = 999;
							String msg = "";

							JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
							logger.info(jsonObject.toJSONString());

							String mobileno = jsonObject.getAsString("mobileno");
							Subscribe data = Subscribe.builder().mobileno(mobileno).build();

							List<Subscribe> targets = service.getSubscribeByMobileno(data);
							if (targets != null && targets.size() > 0) {
								SubscribeMobiletown smt = new SubscribeMobiletown();
								JSONObject json = new JSONObject();
								TestMobileno tm = new TestMobileno();

								if (tm.isTestphone(mobileno)) {
									// 특정 핸드폰의 경우에는 SMS를 발송하지 않고 저장함
									json = smt.cancelMobiletownPseudo(mobileno, data.getSpuserid());

								} else {
									// 개발서버일 경우에는 SMS를 다른 폰으로 쏜다. ===================
									//if ((System.getProperty("argEnv")).equals("dev"))
									//mobileno = itfMgr.getTestMobileno();
									// ===============================================================
									json = smt.cancelMobiletown(mobileno, data.getSpuserid());
								}
								if (200 == (int) json.get("code")) {
									code = 200;
									msg = "";
								} else {
									code = (int) json.get("code");
									msg = json.getAsString("msg");
								}
							} else {
								code = 912;
								msg = "가입정보를 찾을 수 없습니다.[912]";
							}
							return JSONHelper.assembleResponse(code, msg);
						});

						//
						post("/checkotp", (req, res) -> {
							int code = 999;
							String msg = "";

							JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
							logger.info(jsonObject.toJSONString());

							String mobileno = jsonObject.getAsString("mobileno");
							String rnumber = jsonObject.getAsString("rnumber");

							SubscribeMobiletown smt = new SubscribeMobiletown();
							JSONObject json = new JSONObject();
							TestMobileno tm = new TestMobileno();

							if (tm.isTestphone(mobileno)) {
								// 특정 핸드폰의 경우에는 SMS를 발송하지 않고 저장함

							} else {
								// 개발서버일 경우에는 SMS를 다른 폰으로 쏜다. ===================
								//if ((System.getProperty("argEnv")).equals("dev")) mobileno = itfMgr.getTestMobileno();
								// ===============================================================
							}
							json = smt.cancelMobiletownOtp(mobileno, rnumber);

							if (200 == (int) json.get("code")) {
								json.put("code", 200);
								json.put("msg", "");
							} else {
								json.put("code", (int) json.get("code"));
								json.put("msg", json.getAsString("msg"));
							}

							return json;
						});
					});
				});

				// allone 가입 완료 페이지용 - 세션에서 휴대폰 번호 가져오기 API
				get("/allone/phone", (req, res) -> {
					try {
						logger.debug("=== allone 휴대폰 번호 조회 API 호출 ===");

						// 세션에서 휴대폰 번호 가져오기
						String phoneNumber = req.session().attribute("allonePhoneNumber");
						logger.debug("세션에서 가져온 휴대폰 번호: {}", phoneNumber);

						JSONObject responseJSON = new JSONObject();
						if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
							responseJSON.put("code", 200);
							responseJSON.put("phoneNumber", phoneNumber);
							responseJSON.put("msg", "성공");

							// 세션에서 제거 (한 번만 사용)
							req.session().removeAttribute("allonePhoneNumber");
							logger.debug("세션에서 휴대폰 번호 제거 완료");
						} else {
							responseJSON.put("code", 404);
							responseJSON.put("msg", "휴대폰 번호가 없습니다");
						}

						res.type("application/json");
						return responseJSON.toJSONString();
					} catch (Exception e) {
						logger.error("allone 휴대폰 번호 조회 중 오류", e);
						JSONObject errorJSON = new JSONObject();
						errorJSON.put("code", 500);
						errorJSON.put("msg", "서버 오류");
						res.type("application/json");
						return errorJSON.toJSONString();
					}
				});

				// 세션에서 핸드폰 번호 가져오기 (1회성)
				get("/session/phone", (req, res) -> {
					int code = 200;
					String msg = "성공";
					String phoneNumber = null;

					try {
						// 세션에서 핸드폰 번호 가져오기
						phoneNumber = req.session().attribute("allonePhoneNumber");
						logger.debug("세션에서 가져온 핸드폰 번호: {}", phoneNumber);

						if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
							// 1회성 사용이므로 세션에서 삭제
							req.session().removeAttribute("allonePhoneNumber");
							logger.debug("세션에서 핸드폰 번호 삭제 완료");

							JSONObject responseJSON = new JSONObject();
							responseJSON.put("code", code);
							responseJSON.put("msg", msg);
							responseJSON.put("phoneNumber", phoneNumber);

							res.status(200);
							return responseJSON.toJSONString();
						} else {
							code = 404;
							msg = "세션에 핸드폰 번호가 없습니다";
							logger.warn("세션에 핸드폰 번호가 없음");
						}
					} catch (Exception e) {
						logger.error("세션에서 핸드폰 번호 가져오기 실패: {}", e.getMessage());
						code = 500;
						msg = "서버 오류";
					}

					JSONObject responseJSON = new JSONObject();
					responseJSON.put("code", code);
					responseJSON.put("msg", msg);
					responseJSON.put("phoneNumber", phoneNumber);

					if (code == 200) {
						res.status(200);
					} else if (code == 404) {
						res.status(404);
					} else {
						res.status(500);
					}					return responseJSON.toJSONString();
				});

				//최초 가입자
				path("/subscribeEvent", () -> {
					post("/firstUser", (req, res) -> {
						res.type("application/json");

						try {
							// 요청 파라미터 추출
							String mobileno = req.queryParams("phoneNumber");
							if (mobileno == null || mobileno.trim().isEmpty()) {
								res.status(400); // Bad Request
								return "{ \"code\": 400, \"message\": \"phoneNumber parameter is required\" }";
							}

							// DB 조회
							String result = service.getFirstSubscriber(mobileno);

							if ("false".equalsIgnoreCase(result)) {  // DB에 없음 → 최초 가입자
								req.session().attribute("allonePhoneNumber", mobileno);
								logger.debug("firstUser 성공: 세션에 allonePhoneNumber 저장 = {}", mobileno);
							}

							// 정상 응답
							res.status(200);
							return "{ \"code\": 200, \"mobileno\": \"" + mobileno + "\", \"result\": " + result + " }";

						} catch (Exception e) {
							e.printStackTrace(); // 서버 로그에 찍기
							res.status(500); // Internal Server Error
							return "{ \"code\": 500, \"message\": \"Internal server error: " + e.getMessage() + "\" }";
						}
					});
				});



				// KG모빌리언스
				path("/mobilians", () -> {
					post("/savephone", (req, res) -> {
						try {
							JSONObject jsonObject = new JSONObject();
							String[] params = req.body().split("&");
							for (String param : params) {
								// =로 분리하여 key와 value를 추출
								String[] keyValue = param.split("=");
								if (keyValue.length == 2) {
									String key = URLDecoder.decode(keyValue[0], "EUC-KR");
									String value = URLDecoder.decode(keyValue[1], "EUC-KR");
									jsonObject.put(key, value);
								}
							}
							// 휴대폰 결제 완료 후 서비스 가입 프로세스 시작
							logger.info(jsonObject.toJSONString());

							Mobilians mobilians = mobiliansService.setMobilians(jsonObject);

							// 거래 번호 중북 요청 확인 로직
							List<Mobilians> tradeidList = mobiliansService.getTradeidList(mobilians);

							if (!tradeidList.isEmpty()) {
								logger.info("이미 처리된 결제 정보 : {}", mobilians.getTradeid());
								return "SUCCESS";
							}

							Subscribe tempData = new Subscribe();
							mobiliansService.splitMstr(mobilians.getMstr(), tempData);
							String spcode = tempData.getSpcode();

							logger.info("spcode : {}", spcode);

							JSONObject responseJSON = new JSONObject();
							Subscribe data = Subscribe.builder().mobileno(mobilians.getNo()).spcode(spcode).build();
							String msg = null;
							String result = null;    // KG모빌리언스 notiurl에서 리턴 값은 FAIL / SUCCESS 문자만 리턴.


							// ========================================================
							// 부가서비스 제공사로 가입요청 -- START
							// ========================================================
							SubscribeNaru naru = new SubscribeNaru();
							responseJSON = naru.subscribe(data);

							// ========================================================
							// 부가서비스 제공사로 가입요청 -- END
							// ========================================================


							// ========================================================
							// 자체 DB 저장 -- START
							// ========================================================

							if ((int) responseJSON.get("code") == 200) {
								try {
									mobiliansService.splitMstr(mobilians.getMstr(), data);
									service.insertSubscribe(data);
									logger.info("Subscribe inserted: {}", data.toString());
									res.status(201);

									if (mobilians.getResultcd().equals("0000")) {
										result = "SUCCESS";
									} else {
										result = "FAIL";
									}

								} catch (Exception e) {
									e.printStackTrace();
									result = "FAIL";
								}
							} else {
								result = "FAIL";
							}

							// ========================================================
							// 자체 DB 저장 -- END
							// ========================================================


							// ========================================================
							// 휴대폰 모빌리언스 결제 완료 후 사용자 결제 내역 저장 -- START
							// USERKEY, AutoBillKey, 거래번호 등등....
							// 서비스 가입 여부 FAIL / SUCCESS
							// ========================================================
							mobilians.setMsg(msg);
							mobilians.setResult(result);
							mobilians.setAutobillDate(mobiliansService.setAutobillDate(mobilians.getSigndate()));

							mobiliansService.insertPhone(mobilians);

							// ========================================================
							// 휴대폰 모빌리언스 결제 완료 후 사용자 결제 내역 저장 -- END
							// ========================================================

							//return result;
							return "SUCCESS";
						} catch (Exception e) {
							e.printStackTrace();
							return "SUCCESS";
						}

					});






					post("/precheck", (req, res) -> {
						int code = 999;
						String msg = "";
						String sp_uid = "";

						JSONObject responseJSON = new JSONObject();

						JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
						logger.info(jsonObject.toJSONString());
						Subscribe data = JSONValue.parse(req.body(), Subscribe.class);

						// checkcode의 전화번호와 요청한 전화번호가 같은지 확인
						String encCheckcode = data.getCheckcode();
						if (encCheckcode != null && !"".equals(encCheckcode)) {
							try {
								String checkcode = AES256Util.decrypt(encCheckcode);
								//logger.info("checkcode:{}", checkcode);
								String[] codes = checkcode.split("\\|");
								if (codes.length > 2) {
									String cmobileno = codes[0];
									// 개발서버에서는 체크하지 말것
									if ((System.getProperty("argEnv")).equals("dev")) cmobileno = data.getMobileno();
									if (!cmobileno.equals(data.getMobileno())) {
										logger.warn("cmobileno:{} - data.getMobileno():{}", cmobileno, data.getMobileno());
										return JSONHelper.assembleResponse(951, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[951]");
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
								logger.error("CHECK CODE 검증시 오류 : [{}]", e.getMessage());
								return JSONHelper.assembleResponse(952, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[952]");
							}
						} else {
							return JSONHelper.assembleResponse(952, "정상적인 방법으로 인증번호를 입력하고 가입하여 주세요.[953]");
						}

						//기존에 동일한 휴대전화번호가 있는지 확인함
						List<Subscribe> checkDup = service.getSubscribeByMobileno(data);

						//오늘 가입한 적이 있는 휴대전화번호인지 확인함
						List<Subscribe> todayMobileno = service.getTodaySubscribeByMobileno(data);

						// TEST 전용폰 확인하여 당일해지자 가입방지 로직 Skeip
						TestMobileno tm = new TestMobileno();

						if (checkDup.size() > 0) {
							return JSONHelper.assembleResponse(901, "이미 가입된 전화번호입니다.[901]");

						} else if (todayMobileno.size() > 0 && !tm.isTestphone(data.getMobileno())) { // 오늘 가입한적이 있음
							return JSONHelper.assembleResponse(902, "입력하신 전화번호는 해지관련 전산처리 중으로 내일 가입이 가능합니다.[902]");

						} else { // 정상 가입 프로세스 시작
							code = 200;
						}

						return JSONHelper.assembleResponse(code, msg);
					});
					// 휴대폰결제 완료 페이지 리다이렉트
					post("/paycomplete", (req, res) -> {
						logger.debug("=== paycomplete 엔드포인트 시작 ===");

						Properties properties = new Properties();
						try {
							properties.load(Resources.getResourceAsStream("application.properties"));
						} catch (IOException e) {
							logger.error("Properties 파일 로딩 실패", e);
						}

						// 기본 리다이렉트 URL
						String redirectUrl = properties.getProperty("mobilians.paycomplete.url", "https://linksafe.kr/subscribe_done.html");
						logger.debug("기본 리다이렉트 URL: {}", redirectUrl);

						try {
							// POST Body 파싱
							String body = req.body();
							if (body != null && !body.trim().isEmpty()) {
								// JSON 파싱 시도
								if (body.trim().startsWith("{")) {
									// JSON 형태 데이터 파싱
									JSONObject jsonObject = (JSONObject) JSONValue.parse(body);
									logger.debug("JSON 파싱 성공: {}", jsonObject.toJSONString());

									String mstr = jsonObject.getAsString("MSTR");
									String phoneNumber = jsonObject.getAsString("No");
									logger.debug("MSTR 값: {}", mstr);
									logger.debug("휴대폰 번호: {}", phoneNumber);

									if (mstr != null && !mstr.trim().isEmpty()) {
										// mobiliansService.splitMstr를 사용해서 tempData에 정보 추출
										Subscribe tempData = new Subscribe();
										mobiliansService.splitMstr(mstr, tempData);

										String offercode = tempData.getOffercode();
										logger.debug("tempData.getOffercode(): {}", offercode);

										if ("allone".equals(offercode)) {
											// 세션에 휴대폰 번호 저장
											if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
												req.session().attribute("allonePhoneNumber", phoneNumber);
												logger.debug("세션에 휴대폰 번호 저장: {}", phoneNumber);
											}
											redirectUrl = "https://linksafe.kr/subscribe_allone_done.html";
											logger.debug("allone offercode 감지! 리다이렉트 URL 변경: {}", redirectUrl);
										}
									}
								} else {
									// URL 인코딩된 폼 데이터 파싱
									logger.debug("URL 인코딩된 폼 데이터로 파싱 시도");
									String[] params = body.split("&");
									String mstr = null;
									String phoneNumber = null;

									for (String param : params) {
										String[] keyValue = param.split("=");
										if (keyValue.length >= 2) {
											logger.debug("폼 파라미터: {} = {}", keyValue[0], keyValue[1]);
											if ("MSTR".equals(keyValue[0])) {
												mstr = URLDecoder.decode(keyValue[1], "UTF-8");
												logger.debug("폼에서 추출한 MSTR: {}", mstr);
											} else if ("No".equals(keyValue[0])) {
												phoneNumber = URLDecoder.decode(keyValue[1], "UTF-8");
												logger.debug("폼에서 추출한 휴대폰 번호: {}", phoneNumber);
											}
										}
									}

									if (mstr != null && !mstr.trim().isEmpty()) {
										Subscribe tempData = new Subscribe();
										mobiliansService.splitMstr(mstr, tempData);

										String offercode = tempData.getOffercode();
										logger.debug("폼에서 tempData.getOffercode(): {}", offercode);

										if ("allone".equals(offercode)) {
											// 세션에 휴대폰 번호 저장
											if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
												req.session().attribute("allonePhoneNumber", phoneNumber);
												logger.debug("폼에서 세션에 휴대폰 번호 저장: {}", phoneNumber);
											}
											redirectUrl = "https://linksafe.kr/subscribe_allone_done.html";
											logger.debug("폼에서 allone offercode 감지! 리다이렉트 URL 변경: {}", redirectUrl);
										}
									}
								}
							}

						} catch (Exception e) {
							logger.error("paycomplete 처리 중 오류 발생", e);
							// 오류 발생시에도 기본 URL로 리다이렉트
						}

						logger.debug("최종 리다이렉트 URL: {}", redirectUrl);
						res.redirect(redirectUrl);
						logger.debug("=== paycomplete 엔드포인트 완료 ===");
						return null;

					});
				});
			});
		});


	}
}


