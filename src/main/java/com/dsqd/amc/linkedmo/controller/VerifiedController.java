package com.dsqd.amc.linkedmo.controller;

import com.dsqd.amc.linkedmo.RestServer;
import com.dsqd.amc.linkedmo.service.VerifiedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

import static spark.Spark.path;
import static spark.Spark.post;

public class VerifiedController {
    private static final Logger logger = LoggerFactory.getLogger(VerifiedController.class);

    VerifiedService verifiedService = new VerifiedService();
    ObjectMapper mapper = new ObjectMapper();

    public VerifiedController() {
        setupEndpoints();
    }

    // 인증번호 발송 전 사전 검증(precheck)
    private void setupEndpoints() {
        logger.info("[33 line registerRoutes started ");

        path("/api", () -> {
            path("/v1.0", () -> {
                path("/subscribe", () -> {
                    // 인증번호 발송 전 사전 검증(verifiedCheck)
                    post("/verifiedCheck", (req, res) -> {
                        String ip = getClientIp(req);
                        logger.info("[VerifiedController] verifiedCheck called - ip: {}", ip);
                        VerifiedService.PrecheckResult result = verifiedService.precheck(ip);
                        res.type("application/json");
                        return mapper.writeValueAsString(result);
                    });

                    // CAPTCHA 검증
                    post("/verifyCaptcha", (req, res) -> {
                        String ip = getClientIp(req);
                        String captchaInput = req.queryParams("captcha");
                        logger.info("[VerifiedController] verifyCaptcha called - ip: {}, input: {}", ip, captchaInput);

                        boolean isValid = verifiedService.verifyCaptcha(ip, captchaInput);

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", isValid);
                        response.put("message", isValid ? "CAPTCHA 검증 성공" : "CAPTCHA 검증 실패");

                        res.type("application/json");
                        return mapper.writeValueAsString(response);
                    });
                });
            });
        });
    }

    private String getClientIp(spark.Request req) {
        String xfHeader = req.headers("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }
        return req.ip();
    }
}
