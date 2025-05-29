package com.dsqd.amc.linkedmo.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VerifiedService {

    private static final Logger logger = LoggerFactory.getLogger(VerifiedService.class);
    private final DefaultKaptcha kaptcha;
    private static final int RATE_LIMIT = 20;
    private static final long WINDOW_MS = 1000; // 10초
    private static final long CAPTCHA_EXPIRE_MS = 3 * 60 * 1000; // 3분

    private final Map<String, Deque<Long>> ipRequestMap = new ConcurrentHashMap<>();
    private final Map<String, String> ipCaptchaMap = new ConcurrentHashMap<>();
    private final Map<String, Long> captchaStartTimeMap = new ConcurrentHashMap<>(); // CAPTCHA 시작 시간
    private final Map<String, Long> captchaSuccessTimeMap = new ConcurrentHashMap<>(); // CAPTCHA 성공 시간

    public VerifiedService() {
        // Kaptcha 초기화
        kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "200");
        properties.setProperty("kaptcha.image.height", "50");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        properties.setProperty("kaptcha.background.clear.from", "white");
        properties.setProperty("kaptcha.background.clear.to", "white");
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        logger.info("Kaptcha CAPTCHA service initialized");
    }

    public synchronized PrecheckResult precheck(String ip) {
        long now = System.currentTimeMillis();
        
        // CAPTCHA 성공 후 5분간 면제 체크
        Long captchaSuccessTime = captchaSuccessTimeMap.get(ip);
        if (captchaSuccessTime != null && (now - captchaSuccessTime) < 5 * 60 * 1000) {
            logger.info("IP {} is exempt from rate limit due to recent CAPTCHA success", ip);
            return new PrecheckResult(false, null);
        }
        
        // CAPTCHA 만료 체크 (3분 후 자동 해제)
        Long captchaStartTime = captchaStartTimeMap.get(ip);
        if (captchaStartTime != null && (now - captchaStartTime) > CAPTCHA_EXPIRE_MS) {
            // 3분 경과 - CAPTCHA 상태 초기화
            ipRequestMap.remove(ip);
            ipCaptchaMap.remove(ip);
            captchaStartTimeMap.remove(ip);
            logger.info("CAPTCHA expired for IP: {}, resetting to normal state after 3 minutes", ip);
        }
        
        // 이미 CAPTCHA가 필요한 상태인지 확인
        if (ipCaptchaMap.containsKey(ip)) {
            String captcha = ipCaptchaMap.get(ip);
            long remainingTime = CAPTCHA_EXPIRE_MS - (now - captchaStartTimeMap.get(ip));
            logger.info("IP {} still requires CAPTCHA: {}, remaining time: {} seconds", ip, captcha, remainingTime / 1000);
            return new PrecheckResult(true, captcha);
        }
        
        ipRequestMap.putIfAbsent(ip, new ArrayDeque<>());
        Deque<Long> timeStamps = ipRequestMap.get(ip);

        // 윈도우 밖의 오래된 요청 제거
        while (!timeStamps.isEmpty() && now - timeStamps.peekFirst() > WINDOW_MS) {
            timeStamps.pollFirst();
        }
        timeStamps.addLast(now);

        if (timeStamps.size() > RATE_LIMIT) {
            // Rate limit 초과 - CAPTCHA 요구 (3분 타이머 시작)
            String captcha = generateCaptcha();
            ipCaptchaMap.put(ip, captcha);
            captchaStartTimeMap.put(ip, now); // CAPTCHA 시작 시간 기록
            logger.warn("Rate limit exceeded for IP: {}. CAPTCHA required: {}, will auto-reset in 3 minutes", ip, captcha);
            return new PrecheckResult(true, captcha);
        }

        logger.info("Rate limit OK for IP: {}, requests: {}", ip, timeStamps.size());
        return new PrecheckResult(false, null);
    }

    public synchronized boolean verifyCaptcha(String ip, String userInput) {
        String answer = ipCaptchaMap.get(ip);
        if (answer != null && answer.equalsIgnoreCase(userInput)) {
            // CAPTCHA 성공 - 모든 기록 초기화 및 성공 시간 기록
            ipRequestMap.remove(ip);
            ipCaptchaMap.remove(ip);
            captchaStartTimeMap.remove(ip);
            captchaSuccessTimeMap.put(ip, System.currentTimeMillis()); // 성공 시간 기록
            logger.info("CAPTCHA verified successfully for IP: {}", ip);
            return true;
        } else {
            // CAPTCHA 실패 - 새로운 CAPTCHA 생성 (타이머는 유지)
            String newCaptcha = generateCaptcha();
            ipCaptchaMap.put(ip, newCaptcha);
            logger.warn("CAPTCHA verification failed for IP: {}, expected: {}, got: {}. New CAPTCHA: {}", ip, answer, userInput, newCaptcha);
            return false;
        }
    }

    private String generateCaptcha() {
        return kaptcha.createText();
    }

    public String getCurrentCaptcha(String ip) {
        // CAPTCHA가 필요한 상태인지 확인
        return ipCaptchaMap.get(ip);
    }

    public static class PrecheckResult {
        public boolean captchaRequired;
        public String captchaQuestion;

        public PrecheckResult(boolean captchaRequired, String captchaQuestion) {
            this.captchaRequired = captchaRequired;
            this.captchaQuestion = captchaQuestion;
        }
    }
}
