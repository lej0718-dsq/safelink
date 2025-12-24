package com.dsqd.amc.linkedmo.controller;

import static spark.Spark.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.model.Batch;
import com.dsqd.amc.linkedmo.model.Blocknumber;
import com.dsqd.amc.linkedmo.model.Evententry;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.service.AdminService;
import com.dsqd.amc.linkedmo.util.JSONHelper;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import spark.Request;
import spark.Response;


public class AdminController {
	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
	private final AdminService adminService = new AdminService();

	public AdminController() {
		setupEndpoints();
	}

	private void setupEndpoints() {
        path("/api", () -> {
            path("/v1.0", () -> {
                path("/admin/batch", () -> {
                    // Get all boards
                    get("", this::getBatchAll);
                });
                
                path("/admin/subscribe/status", () -> {
                    // Get all boards
                    post("", this::setAdminStatus);
                });
                
                path("/admin/evententry", () -> {
                    // Get all envententry
                    get("", this::getAllEvententry);
                });
                
                path("/admin/evententry2", () -> {
                    // Get all envententry
                    get("", this::getAllEvententry2);
                });
                
                path("/admin/summary1", () -> {
                    // 통계::가입자순증
                    post("", this::adminSummaryNet);
                });
                
                path("/admin/summary", () -> {
                    // 통계::가입통계
                    post("/offer", this::adminSummaryOffer);
                });

                path("/admin/dashboard", () -> {
                	// 당일현황
                	get("/today", this::adminDashbdToday);
                });

                path("/admin/statistics", () -> {
                    // 당일현황
                    get("/monthly", this::adminStatisticsMonthly);
                    get("/daily", this::adminStatisticsDaily);
                    get("/export", this::adminStatisticsExport);
                });

                path("/admin/blocknumbers", () -> {
                    // 차단 고객 관리
                    get("", this::getBlocknumbers);
                    post("", this::insertBlocknumber);
                    put("/:id", this::cancelBlocknumber);
                });
            });
        });
    }
	
	private String adminSummaryNet(Request req, Response res) {
		AdminService adminService = new AdminService();
		JSONObject param = new JSONObject();
		List<Map<String, Object>> lst = adminService.summaryNet(param);
		JSONArray retArry = new JSONArray();
		for (Map<String, Object> m: lst) {
			JSONObject obj = new JSONObject();
			for (Map.Entry<String, Object> entry : m.entrySet()) {
				String key = entry.getKey(); 
				Object value = entry.getValue(); 
				String valueAsString = value.toString();
				if ("summarydate".equals(key)) {
					obj.put(key, valueAsString);
				} else {
					obj.put(key, value);
				}
				// System.out.println("key : " + key + "   value : " + valueAsString);
			}
			retArry.add(obj);
		}
		return retArry.toJSONString();
	}
	
	private String adminSummaryOffer(Request req, Response res) {
		AdminService adminService = new AdminService();
		JSONObject param = new JSONObject();
		List<Map<String, Object>> lst = adminService.summaryOffer();
		JSONArray retArry = new JSONArray();
		for (Map<String, Object> m: lst) {
			JSONObject obj = new JSONObject();
			for (Map.Entry<String, Object> entry : m.entrySet()) {
				String key = entry.getKey(); 
				Object value = entry.getValue(); 
				String valueAsString = value.toString();
				if ("summarydate".equals(key)) {
					obj.put(key, valueAsString);
				} else {
					obj.put(key, value);
				}
				// System.out.println("key : " + key + "   value : " + valueAsString);
			}
			retArry.add(obj);
		}
		return retArry.toJSONString();
	}

	
	private String adminDashbdToday(Request req, Response res) {
		AdminService adminService = new AdminService();
		JSONObject retObj = new JSONObject();
		Map<String, Object> map = adminService.dashbdToday();
		retObj.put("payedcnt", map.get("payedcnt"));
		retObj.put("todaysubcnt", map.get("todaysubcnt"));
		retObj.put("todaycnlcnt", map.get("todaycnlcnt"));
		return retObj.toJSONString();
	}
	
	private String setAdminStatus(Request req, Response res) {
		/* 데이터를 받아와야 함
		UPDATE subscribe
        SET spcode=#{spcode},
            mobileno=#{mobileno},
			status=#{status},
            updated_at=NOW(),
            cancelreason='관리자가 상태 변경'
        WHERE id=#{id}
		 */
		JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());
		int code = 0;
		String msg = "";
		
		try {
			int id = Integer.parseInt(jsonObject.getAsString("id"));
			String mobileno = jsonObject.getAsString("mobileno");
			String status = jsonObject.getAsString("status");
			String before_status = jsonObject.getAsString("bstatus");
			Subscribe data = Subscribe.builder()
					.id(id)
					.mobileno(mobileno)
					.status(status)
					.build();
			adminService.updateSubscribeStatus(data);
			code = 200;
			msg = "Status Changer SUCCESS - " + before_status + "->" + status;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("DB UPDATE ERROR [{}]", e.getMessage());
			code = 901;
			msg = "Status Changer FAILED - " + e.getMessage();
		}
		return JSONHelper.assembleResponse(code, msg).toJSONString();
	}
	
    private String getJsonData(Request req, Response res) {
    	JSONObject jsonData = new JSONObject();
    	JSONArray lstJson = new JSONArray();
    	
    	List<Map<String, Object>> result = adminService.getJsonData(jsonData); 
    	for (Map<String, Object> row : result) { 
    		JSONObject json = new JSONObject(row); 
    		lstJson.add(json); 
    	}        
    	return lstJson.toJSONString();  // JSON Array 형식으로 반환
    }
	
    private String getBatchAll(Request req, Response res) {
        List<Batch> json_list = adminService.getBatchAll();
        return json_list.stream()
                     .map(Batch::toJSONString)
                     .reduce("[", (acc, json) -> acc + json + ",")
                     .replaceAll(",$", "]");  // JSON Array 형식으로 반환
    }
    
    private String getAllEvententry(Request req, Response res) {
    	List<Evententry> json_list = adminService.getAllEvententry();
        return json_list.stream()
                     .map(Evententry::toJSONString)
                     .reduce("[", (acc, json) -> acc + json + ",")
                     .replaceAll(",$", "]");  // JSON Array 형식으로 반환
    }
    
    private String getAllEvententry2(Request req, Response res) {
    	List<Evententry> json_list = adminService.getAllEvententry2();
        return json_list.stream()
                     .map(Evententry::toJSONString)
                     .reduce("[", (acc, json) -> acc + json + ",")
                     .replaceAll(",$", "]");  // JSON Array 형식으로 반환
    }

    private String adminStatisticsMonthly(Request req, Response res) {
        try {
            // 요청 파라미터 추출
            String partner = req.queryParams("partner");
            String year = req.queryParams("year");
            String month = req.queryParams("month");

            // 기본값 설정
            if (partner == null) partner = "all";
            if (year == null) year = "2025";
            if (month == null) month = "09";

            // 파라미터 검증
            if (!isValidYear(year) || !isValidMonth(month)) {
                res.status(400);
                return JSONHelper.assembleResponse(400, "Invalid year or month parameter").toJSONString();
            }

            JSONObject params = new JSONObject();
            params.put("partner", partner);
            params.put("year", year);
            params.put("month", month);

            // 월별 통계 데이터 조회
            Map<String, Object> monthlyStats = adminService.getMonthlyStatistics(params);

            if (monthlyStats == null) {
                res.status(404);
                return JSONHelper.assembleResponse(404, "No data found for the specified period").toJSONString();
            }

            // 응답 JSON 구성
            JSONObject response = new JSONObject();
            response.put("success", true);

            JSONObject data = new JSONObject();
            data.put("partner", partner);
            data.put("year", year);
            data.put("month", month);

            // monthlyStatistics 객체 구성
            JSONObject monthlyStatistics = new JSONObject();
            monthlyStatistics.put("newSubscribers", monthlyStats.get("newSubscribers"));
            monthlyStatistics.put("monthlyCancellations", monthlyStats.get("monthlyCancellations"));
            monthlyStatistics.put("totalSubscribers", monthlyStats.get("totalSubscribers"));
            monthlyStatistics.put("usageDays", monthlyStats.get("usageDays"));
            monthlyStatistics.put("settlementAmount", monthlyStats.get("settlementAmount"));

            // 변화율 정보 추가
            JSONObject newSubscribersChange = new JSONObject();
            newSubscribersChange.put("value", monthlyStats.get("newSubscribersChangeValue"));
            newSubscribersChange.put("period", "전월 대비");
            monthlyStatistics.put("newSubscribersChange", newSubscribersChange);

            JSONObject monthlyCancellationsChange = new JSONObject();
            monthlyCancellationsChange.put("value", monthlyStats.get("monthlyCancellationsChangeValue"));
            monthlyCancellationsChange.put("period", "전월 대비");
            monthlyStatistics.put("monthlyCancellationsChange", monthlyCancellationsChange);

            JSONObject totalSubscribersChange = new JSONObject();
            totalSubscribersChange.put("value", monthlyStats.get("totalSubscribersChangeValue"));
            totalSubscribersChange.put("period", "전월 대비");
            monthlyStatistics.put("totalSubscribersChange", totalSubscribersChange);

            data.put("monthlyStatistics", monthlyStatistics);

            response.put("data", data);

            // 메타데이터 추가
            response.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date()));

            res.type("application/json");
            return response.toJSONString();

        } catch (Exception e) {
            logger.error("Error in adminStatisticsMonthly: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }

    private boolean isValidYear(String year) {
        try {
            int y = Integer.parseInt(year);
            return y >= 2020 && y <= 2030;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidMonth(String month) {
        try {
            int m = Integer.parseInt(month);
            return m >= 1 && m <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String adminStatisticsDaily(Request req, Response res) {
        try {
            // 요청 파라미터 추출
            String partner = req.queryParams("partner");
            String year = req.queryParams("year");
            String month = req.queryParams("month");

            // 기본값 설정
            if (partner == null) partner = "all";
            if (year == null) year = "2025";
            if (month == null) month = "09";

            // 파라미터 검증
            if (!isValidYear(year) || !isValidMonth(month)) {
                res.status(400);
                return JSONHelper.assembleResponse(400, "Invalid year or month parameter").toJSONString();
            }

            JSONObject params = new JSONObject();
            params.put("partner", partner);
            params.put("year", year);
            params.put("month", month);

            // 일별 상세 통계 데이터 조회
            List<Map<String, Object>> dailyStats = adminService.getDailyStatistics(params);

            if (dailyStats == null || dailyStats.isEmpty()) {
                res.status(404);
                return JSONHelper.assembleResponse(404, "No daily statistics data found for the specified period").toJSONString();
            }

            // 응답 JSON 구성
            JSONObject response = new JSONObject();
            response.put("success", true);

            JSONObject data = new JSONObject();
            data.put("partner", partner);
            data.put("year", year);
            data.put("month", month);

            // dailyDetails 배열 구성
            JSONArray dailyDetails = new JSONArray();
            for (Map<String, Object> dailyStat : dailyStats) {
                JSONObject dailyDetail = new JSONObject();
                dailyDetail.put("date", dailyStat.get("date"));
                dailyDetail.put("dailyNewSubscribers", dailyStat.get("dailyNewSubscribers"));
                dailyDetail.put("dailyCancellations", dailyStat.get("dailyCancellations"));
                dailyDetail.put("totalSubscribers", dailyStat.get("totalSubscribers"));
                dailyDetail.put("dailyUsageDays", dailyStat.get("dailyUsageDays"));
                dailyDetail.put("dailySettlementAmount", dailyStat.get("dailySettlementAmount"));
                dailyDetails.add(dailyDetail);
            }

            data.put("dailyDetails", dailyDetails);
            response.put("data", data);

            // 메타데이터 추가
            response.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date()));

            res.type("application/json");
            return response.toJSONString();

        } catch (Exception e) {
            logger.error("Error in adminStatisticsDaily: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }

    // 차단 고객 목록 조회
    private String getBlocknumbers(Request req, Response res) {
        try {
            String mobileno = req.queryParams("mobileno");
            String spcode = req.queryParams("spcode");
            String status = req.queryParams("status");

            List<Blocknumber> list;

            // 검색 조건이 있는 경우
            if ((mobileno != null && !mobileno.isEmpty()) ||
                (spcode != null && !spcode.isEmpty()) ||
                (status != null && !status.isEmpty())) {

                Map<String, Object> params = new HashMap<>();
                params.put("mobileno", mobileno);
                params.put("spcode", spcode);
                params.put("status", status);
                list = adminService.searchBlocknumbers(params);
            } else {
                list = adminService.getAllBlocknumbers();
            }

            JSONArray jsonArray = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Blocknumber item : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("spcode", item.getSpcode());
                obj.put("status", item.getStatus());
                obj.put("mobileno", item.getMobileno());
                obj.put("createat", item.getCreateat() != null ? sdf.format(item.getCreateat()) : null);
                obj.put("canceledat", item.getCanceledat() != null ? sdf.format(item.getCanceledat()) : null);
                obj.put("usernameofoper", item.getUsernameofoper());
                obj.put("remark", item.getRemark());
                jsonArray.add(obj);
            }

            res.type("application/json");
            return jsonArray.toJSONString();

        } catch (Exception e) {
            logger.error("Error in getBlocknumbers: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }

    // 차단 고객 등록
    private String insertBlocknumber(Request req, Response res) {
        try {
            JSONObject jsonObject = (JSONObject) JSONValue.parse(req.body());

            String mobileno = jsonObject.getAsString("mobileno");
            String spcode = jsonObject.getAsString("spcode");
            String remark = jsonObject.getAsString("remark");

            if (mobileno == null || mobileno.isEmpty()) {
                res.status(400);
                return JSONHelper.assembleResponse(400, "전화번호는 필수 입력값입니다.").toJSONString();
            }

            // 조작자 정보 (JWT에서 추출하거나 기본값 사용)
            String username = req.attribute("username");
            if (username == null) {
                username = "admin";
            }

            Blocknumber blocknumber = Blocknumber.builder()
                    .mobileno(mobileno)
                    .spcode(spcode != null ? spcode : "ALL")
                    .usernameofoper(username)
                    .remark(remark)
                    .build();

            adminService.insertBlocknumber(blocknumber);

            res.type("application/json");
            return JSONHelper.assembleResponse(200, "차단 고객이 등록되었습니다.").toJSONString();

        } catch (Exception e) {
            logger.error("Error in insertBlocknumber: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }

    // 차단 해제
    private String cancelBlocknumber(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));

            // 조작자 정보
            String username = req.attribute("username");
            if (username == null) {
                username = "admin";
            }

            Blocknumber blocknumber = Blocknumber.builder()
                    .id(id)
                    .usernameofoper(username)
                    .remark("관리자에 의해 차단 해제")
                    .build();

            adminService.cancelBlocknumber(blocknumber);

            res.type("application/json");
            return JSONHelper.assembleResponse(200, "차단이 해제되었습니다.").toJSONString();

        } catch (NumberFormatException e) {
            res.status(400);
            return JSONHelper.assembleResponse(400, "Invalid ID format").toJSONString();
        } catch (Exception e) {
            logger.error("Error in cancelBlocknumber: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }

    private String adminStatisticsExport(Request req, Response res) {
        try {
            // 요청 파라미터 추출
            String partner = req.queryParams("partner");
            String year = req.queryParams("year");
            String month = req.queryParams("month");

            // 기본값 설정
            if (partner == null) partner = "all";
            if (year == null) year = "2025";
            if (month == null) month = "09";

            // 파라미터 검증
            if (!isValidYear(year) || !isValidMonth(month)) {
                res.status(400);
                return JSONHelper.assembleResponse(400, "Invalid year or month parameter").toJSONString();
            }

            JSONObject params = new JSONObject();
            params.put("partner", partner);
            params.put("year", year);
            params.put("month", month);

            // 사용자 리스트 데이터 조회
            List<Map<String, Object>> userList = adminService.getUserListForExport(params);

            if (userList == null || userList.isEmpty()) {
                res.status(404);
                return JSONHelper.assembleResponse(404, "No user data found for the specified period").toJSONString();
            }

            // 응답 JSON 구성
            JSONObject response = new JSONObject();
            response.put("success", true);

            JSONObject data = new JSONObject();
            data.put("partner", partner);
            data.put("year", year);
            data.put("month", month);

            // 사용자 리스트 데이터 추가
            JSONArray userArray = new JSONArray();
            for (Map<String, Object> user : userList) {
                JSONObject userObj = new JSONObject();
                userObj.put("no", user.get("no"));
                userObj.put("spcode", user.get("spcode"));
                userObj.put("mobileno", user.get("mobileno"));
                userObj.put("status", user.get("status"));
                userObj.put("offercode", user.get("offercode"));
                userObj.put("created_date", user.get("created_date"));
                userObj.put("canceled_date", user.get("canceled_date"));
                userObj.put("usage_days", user.get("usage_days"));
                userArray.add(userObj);
            }

            data.put("userList", userArray);
            response.put("data", data);

            // 메타데이터 추가
            response.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date()));

            res.type("application/json");
            return response.toJSONString();

        } catch (Exception e) {
            logger.error("Error in adminStatisticsExport: {}", e.getMessage(), e);
            res.status(500);
            return JSONHelper.assembleResponse(500, "Internal server error: " + e.getMessage()).toJSONString();
        }
    }
}
