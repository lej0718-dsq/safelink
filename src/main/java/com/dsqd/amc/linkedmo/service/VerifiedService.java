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
    private static final int RATE_LIMIT = 5;
    private static final long WINDOW_MS = 1000;

    private final Map<String, Deque<Long>> ipRequestMap = new ConcurrentHashMap<>();
    private final Map<String, String> ipCaptchaMap = new ConcurrentHashMap<>();

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
        ipRequestMap.putIfAbsent(ip, new ArrayDeque<>());
        Deque<Long> timeStamps = ipRequestMap.get(ip);

        // 윈도우 밖의 오래된 요청 제거
        while (!timeStamps.isEmpty() && now - timeStamps.peekFirst() > WINDOW_MS) {
            timeStamps.pollFirst();
        }
        timeStamps.addLast(now);


        if (timeStamps.size() > RATE_LIMIT) {
            // CAPTCHA 필요
            String captcha = generateCaptcha();
            ipCaptchaMap.put(ip, captcha);
            logger.info("Rate limit exceeded for IP: {}, CAPTCHA required: {}", ip, captcha);
            return new PrecheckResult(true, captcha);
        }

        logger.info("Rate limit OK for IP: {}, requests: {}", ip, timeStamps.size());
        return new PrecheckResult(false, null);
    }

    public synchronized boolean verifyCaptcha(String ip, String userInput) {
        String answer = ipCaptchaMap.get(ip);
        if (answer != null && answer.equalsIgnoreCase(userInput)) {
            ipRequestMap.remove(ip);
            ipCaptchaMap.remove(ip);
            logger.info("CAPTCHA verified successfully for IP: {}", ip);
            return true;
        }
        logger.warn("CAPTCHA verification failed for IP: {}, expected: {}, got: {}", ip, answer, userInput);
        return false;
    }

    private String generateCaptcha() {
        return kaptcha.createText();
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
