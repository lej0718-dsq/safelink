package com.dsqd.amc.linkedmo.controller;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.dsqd.amc.linkedmo.model.Refund;
import com.dsqd.amc.linkedmo.model.RefundCalculation;
import com.dsqd.amc.linkedmo.model.RefundDetail;
import com.dsqd.amc.linkedmo.model.RefundQuery;
import com.dsqd.amc.linkedmo.model.RefundRequest;
import com.dsqd.amc.linkedmo.service.RefundService;
import com.dsqd.amc.linkedmo.util.AccountMasker;
import com.dsqd.amc.linkedmo.util.JSONHelper;
import com.dsqd.amc.linkedmo.util.JwtUtil;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import spark.Request;
import spark.Response;

/**
 * 환불 API 라우터.
 *  - POST /api/v1.0/refund                       : 사용자 환불 신청 (JWT 미인증, ApiExcludeList 에 등록)
 *  - GET  /api/v1.0/admin/refund                 : 관리자 목록
 *  - GET  /api/v1.0/admin/refund/:id             : 관리자 상세 + breakdown
 *  - PUT  /api/v1.0/admin/refund/:id/complete    : 관리자 환불 완료 처리
 */
public class RefundController {

    private static final Logger logger = LoggerFactory.getLogger(RefundController.class);
    private static final SimpleDateFormat DATE_ONLY = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final RefundService refundService = new RefundService();

    public RefundController() {
        setupEndpoints();
    }

    private void setupEndpoints() {
        path("/api", () -> {
            path("/v1.0", () -> {
                // ─── 사용자 (JWT 미인증)
                path("/refund", () -> {
                    post("", this::createRefund);
                });

                // ─── 관리자 (JWT 필수)
                path("/admin/refund", () -> {
                    get("",                  this::listRefunds);
                    get("/:id",              this::getRefund);
                    put("/:id/complete",     this::completeRefund);
                });
            });
        });
    }

    // ============================================================
    // 사용자: 환불 신청
    // ============================================================

    private Object createRefund(Request req, Response res) {
        res.type("application/json");
        try {
            RefundRequest input = JSONValue.parse(req.body(), RefundRequest.class);
            logger.info("Refund create request: phone={}, bank={}",
                input == null ? null : AccountMasker.maskPhone(input.getPhone()),
                input == null ? null : input.getBankCode());

            RefundService.ApplyResult result = refundService.apply(input);
            if (result.code != 200) {
                return JSONHelper.assembleResponse(result.code, result.msg).toJSONString();
            }

            JSONObject data = new JSONObject();
            data.put("refundId",      result.refund.getId());
            data.put("status",        result.refund.getStatus());
            data.put("refundAmount",  result.refund.getRefundAmount());
            data.put("useStartDate",  formatDate(result.refund.getUseStartDate()));
            data.put("useEndDate",    formatDate(result.refund.getUseEndDate()));
            data.put("breakdown",     toBreakdownArray(result.calc));

            JSONObject body = JSONHelper.assembleResponse(200, "");
            body.put("data", data);
            return body.toJSONString();

        } catch (Exception e) {
            logger.error("Refund create error", e);
            return JSONHelper.assembleResponse(998, "서버 오류가 발생했습니다.").toJSONString();
        }
    }

    // ============================================================
    // 관리자: 목록
    // ============================================================

    private Object listRefunds(Request req, Response res) {
        res.type("application/json");
        try {
            RefundQuery q = RefundQuery.builder()
                .status(req.queryParams("status"))
                .mobileno(req.queryParams("mobileno"))
                .from(req.queryParams("from"))
                .to(req.queryParams("to"))
                .build();

            List<Refund> rows = refundService.list(q);
            JSONArray arr = new JSONArray();
            for (Refund r : rows) {
                arr.add(toListItemJson(r));
            }

            JSONObject body = JSONHelper.assembleResponse(200, "");
            body.put("data", arr);
            body.put("total", rows.size());
            return body.toJSONString();

        } catch (Exception e) {
            logger.error("Admin refund list error", e);
            return JSONHelper.assembleResponse(998, "조회 중 오류가 발생했습니다.").toJSONString();
        }
    }

    // ============================================================
    // 관리자: 상세
    // ============================================================

    private Object getRefund(Request req, Response res) {
        res.type("application/json");
        try {
            long id = Long.parseLong(req.params(":id"));
            Refund r = refundService.get(id);
            if (r == null) {
                return JSONHelper.assembleResponse(404, "환불 신청 내역을 찾을 수 없습니다.").toJSONString();
            }
            List<RefundDetail> details = refundService.getDetails(id);

            JSONObject data = toDetailJson(r, details);
            JSONObject body = JSONHelper.assembleResponse(200, "");
            body.put("data", data);
            return body.toJSONString();

        } catch (NumberFormatException e) {
            return JSONHelper.assembleResponse(400, "잘못된 ID 입니다.").toJSONString();
        } catch (Exception e) {
            logger.error("Admin refund detail error", e);
            return JSONHelper.assembleResponse(998, "조회 중 오류가 발생했습니다.").toJSONString();
        }
    }

    // ============================================================
    // 관리자: 환불 완료 처리
    // ============================================================

    private Object completeRefund(Request req, Response res) {
        res.type("application/json");
        try {
            long id = Long.parseLong(req.params(":id"));
            JSONObject reqBody = (JSONObject) JSONValue.parse(req.body() == null ? "{}" : req.body());
            String memo = reqBody == null ? null : reqBody.getAsString("memo");

            String adminId = extractAdminId(req);
            int code = refundService.markCompleted(id, adminId, memo);

            switch (code) {
                case 200:
                    Refund updated = refundService.get(id);
                    JSONObject data = new JSONObject();
                    data.put("id", updated.getId());
                    data.put("status", updated.getStatus());
                    data.put("processedDate", formatDateTime(updated.getProcessedDate()));
                    data.put("processedBy", updated.getProcessedBy());
                    JSONObject body = JSONHelper.assembleResponse(200, "");
                    body.put("data", data);
                    return body.toJSONString();
                case 404:
                    return JSONHelper.assembleResponse(404, "환불 신청 내역을 찾을 수 없습니다.").toJSONString();
                case 909:
                    return JSONHelper.assembleResponse(909, "이미 완료 처리된 건입니다.").toJSONString();
                default:
                    return JSONHelper.assembleResponse(998, "처리 중 오류가 발생했습니다.").toJSONString();
            }
        } catch (NumberFormatException e) {
            return JSONHelper.assembleResponse(400, "잘못된 ID 입니다.").toJSONString();
        } catch (Exception e) {
            logger.error("Admin refund complete error", e);
            return JSONHelper.assembleResponse(998, "처리 중 오류가 발생했습니다.").toJSONString();
        }
    }

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    private static String extractAdminId(Request req) {
        try {
            String token = req.headers("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token == null || token.isEmpty()) return null;
            DecodedJWT jwt = JwtUtil.verifyToken(token);
            return JwtUtil.getUsername(jwt);
        } catch (Exception e) {
            logger.warn("extractAdminId failed: {}", e.getMessage());
            return null;
        }
    }

    private static JSONObject toListItemJson(Refund r) {
        JSONObject o = new JSONObject();
        o.put("id",            r.getId());
        o.put("mobileno",      AccountMasker.maskPhone(r.getMobileno()));
        o.put("mobilenoRaw",   r.getMobileno());
        o.put("name",          r.getName());
        o.put("bankName",      r.getBankName());
        o.put("account",       "");  // 목록에서는 계좌 노출 안함
        o.put("refundAmount",  r.getRefundAmount());
        o.put("status",        r.getStatus());
        o.put("useStartDate",  formatDate(r.getUseStartDate()));
        o.put("useEndDate",    formatDate(r.getUseEndDate()));
        o.put("requestDate",   formatDateTime(r.getRequestDate()));
        o.put("processedDate", formatDateTime(r.getProcessedDate()));
        o.put("processedBy",   r.getProcessedBy());
        return o;
    }

    private JSONObject toDetailJson(Refund r, List<RefundDetail> details) {
        JSONObject o = new JSONObject();
        o.put("id",            r.getId());
        o.put("mobileno",      r.getMobileno());
        o.put("name",          r.getName());
        o.put("birth",         r.getBirth());
        o.put("bankCode",      r.getBankCode());
        o.put("bankName",      r.getBankName());

        String accountPlain = refundService.decryptAccount(r.getAccountEnc());
        o.put("account",       accountPlain);
        o.put("accountMasked", AccountMasker.maskAccount(accountPlain));

        o.put("subscribeId",   r.getSubscribeId());
        o.put("useStartDate",  formatDate(r.getUseStartDate()));
        o.put("useEndDate",    formatDate(r.getUseEndDate()));
        o.put("refundAmount",  r.getRefundAmount());
        o.put("status",        r.getStatus());
        o.put("memo",          r.getMemo());
        o.put("requestDate",   formatDateTime(r.getRequestDate()));
        o.put("processedDate", formatDateTime(r.getProcessedDate()));
        o.put("processedBy",   r.getProcessedBy());

        JSONArray arr = new JSONArray();
        if (details != null) {
            for (RefundDetail d : details) {
                JSONObject dj = new JSONObject();
                dj.put("ym",         d.getYm());
                dj.put("monthStart", formatDate(d.getMonthStart()));
                dj.put("monthEnd",   formatDate(d.getMonthEnd()));
                dj.put("usedDays",   d.getUsedDays());
                dj.put("totalDays",  d.getTotalDays());
                dj.put("dailyFee",   d.getDailyFee() == null ? null : d.getDailyFee().toPlainString());
                dj.put("amount",     d.getAmount());
                arr.add(dj);
            }
        }
        o.put("breakdown", arr);
        return o;
    }

    private static JSONArray toBreakdownArray(RefundCalculation calc) {
        JSONArray arr = new JSONArray();
        if (calc == null || calc.getDetails() == null) return arr;
        for (RefundDetail d : calc.getDetails()) {
            JSONObject dj = new JSONObject();
            dj.put("ym",         d.getYm());
            dj.put("usedDays",   d.getUsedDays());
            dj.put("totalDays",  d.getTotalDays());
            dj.put("dailyFee",   d.getDailyFee() == null ? null : d.getDailyFee().toPlainString());
            dj.put("amount",     d.getAmount());
            arr.add(dj);
        }
        return arr;
    }

    private static synchronized String formatDate(Date d) {
        return d == null ? null : DATE_ONLY.format(d);
    }

    private static synchronized String formatDateTime(Date d) {
        return d == null ? null : DATE_TIME.format(d);
    }
}
