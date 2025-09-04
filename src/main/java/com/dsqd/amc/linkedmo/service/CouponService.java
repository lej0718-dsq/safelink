package com.dsqd.amc.linkedmo.service;

import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.mapper.CouponMapper;
import com.dsqd.amc.linkedmo.mapper.MobiliansAutoMapper;
import com.dsqd.amc.linkedmo.model.CouponRequest;
import com.dsqd.amc.linkedmo.model.CouponRequestCount;
import com.dsqd.amc.linkedmo.model.CouponResponse;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CouponService {
    private final Logger logger = LoggerFactory.getLogger(CouponService.class);
    private SqlSessionFactory sqlSessionFactory;

    public CouponService() {
        this.sqlSessionFactory = MyBatisConfig.getSqlSessionFactory();
    }

    public List<Subscribe> getCouponTargetList(HashMap<String,Object> data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CouponMapper mapper = session.getMapper(CouponMapper.class);
            return mapper.getCouponTargetList(data);
        }
    }
    //allone mega coupon
    public List<Subscribe> getNhCouponTargetList(HashMap<String,Object> data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CouponMapper mapper = session.getMapper(CouponMapper.class);
            return mapper.getMegaCouponTargetList(data);
        }
    }


    public void insertCouponRequest(HashMap<String,Object> data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CouponMapper mapper = session.getMapper(CouponMapper.class);
            mapper.insertCouponRequest(data);
            session.commit();
        }
    }

    public void insertCouponResponse(CouponResponse data) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CouponMapper mapper = session.getMapper(CouponMapper.class);
            mapper.insertCouponResponse(data);
            session.commit();
        }
    }
    public void updateCouponRequestCount(String offercode, String noReq, int couponCount) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CouponMapper mapper = session.getMapper(CouponMapper.class);
            mapper.updateCouponRequestCount(offercode, noReq, couponCount);
            session.commit();
        }
    }
    /*
    * 배치 실행 메소드
    * */
    public void sendCouponRequest() {
        // 모바일 쿠폰 발행 리스트 조회
        LocalDate today = LocalDate.now();
        String targetDate = today.minusMonths(1).toString();
        HashMap<String,Object> params = new HashMap<>();
        //params.put("targetDate", targetDate);
        //params.put("offerCode", "20");
        // 로컬 테스트
        //params.put("targetDate", "2025-02-26");

        List<Subscribe> couponTargetList = getCouponTargetList(params);


        Properties properties = new Properties();
        try {
            properties.load(Resources.getResourceAsStream("application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //for(int i=0;i<20;i++) {
        for(Subscribe target : couponTargetList) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // POST 요청을 보낼 URL 설정
                HttpPost httpPost = new HttpPost(properties.getProperty("coupon.server"));

                // 전송할 파라미터 설정 (x-www-form-urlencoded 형식)
                List<NameValuePair> requestParams = new ArrayList<>();
                requestParams.add(new BasicNameValuePair("ACTION", "CI102_ISSUECPN_TITLE_WITHPAY"));
                requestParams.add(new BasicNameValuePair("COOPER_ID", properties.getProperty("coupon.cooperid")));
                requestParams.add(new BasicNameValuePair("COOPER_PW", properties.getProperty("coupon.cooperpw")));
                requestParams.add(new BasicNameValuePair("SITE_ID", properties.getProperty("coupon.siteid")));
                requestParams.add(new BasicNameValuePair("NO_REQ", properties.getProperty("coupon.noreq")));
                requestParams.add(new BasicNameValuePair("COOPER_ORDER", target.getMobileno()+new Date().getTime()));
                //requestParams.add(new BasicNameValuePair("COOPER_ORDER", "01045273143"+new Date().getTime()));
                requestParams.add(new BasicNameValuePair("ISSUE_COUNT", "1"));
                requestParams.add(new BasicNameValuePair("CALL_CTN", properties.getProperty("coupon.callctn")));
                requestParams.add(new BasicNameValuePair("RCV_CTN", target.getMobileno()));
                //requestParams.add(new BasicNameValuePair("RCV_CTN", "01045273143"));
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
                insertCouponRequest(params);

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
                        insertCouponResponse(xmlParse(responseBody));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

    public CouponResponse xmlParse(String xmlResponse) {
        // XML 문자열을 JSONObject로 변환
        JSONObject jsonObject = XML.toJSONObject(xmlResponse);
        // 최상위 요소 "CJSERVICE"를 가져옴
        JSONObject cjService = jsonObject.getJSONObject("CJSERVICE");

        // CouponResponse 객체 생성 후 값 채워넣기
        CouponResponse couponResponse = new CouponResponse();

        couponResponse.setAction(cjService.optString("ACTION"));
        couponResponse.setEngine_id(cjService.optString("ENGINE_ID"));
        couponResponse.setRt(cjService.optString("RT"));
        couponResponse.setRtmsg(cjService.optString("RTMSG"));
        couponResponse.setCooper_order(cjService.optString("COOPER_ORDER"));
        couponResponse.setIssue_count(cjService.optInt("ISSUE_COUNT", 0));

        // 중첩된 CPN_LIST > CPN 처리
        if (cjService.has("CPN_LIST")) {
            JSONObject cpnList = cjService.getJSONObject("CPN_LIST");
            if (cpnList.has("CPN")) {
                JSONObject cpn = cpnList.getJSONObject("CPN");
                couponResponse.setNo_cpn(cpn.optString("NO_CPN"));
                couponResponse.setNo_auth(cpn.optString("NO_AUTH"));
                couponResponse.setCpn_pw(cpn.optString("CPN_PW"));
                couponResponse.setTs_id(cpn.optString("TS_ID"));
            }
        }

        // 응답 처리일(response_date)은 현재 시간으로 설정 (또는 XML에서 추출)
        couponResponse.setResponse_date(new Date());

        return couponResponse;
    }


    /*
     * 배치 실행 메소드
     * */
    public void sendNHCouponRequest() {
        // 모바일 쿠폰 발행 리스트 조회
        LocalDate today = LocalDate.now();
        String targetDate = today.minusMonths(1).toString();
        HashMap<String,Object> params = new HashMap<>();
        //params.put("targetDate", targetDate);
        //params.put("offerCode", "20");
        // 로컬 테스트
        //params.put("targetDate", "2025-02-26");

        List<Subscribe> NhcouponTargetList = getNhCouponTargetList(params);


        Properties properties = new Properties();
        try {
            properties.load(Resources.getResourceAsStream("application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Subscribe firstTarget = NhcouponTargetList.get(0);
        int available = firstTarget.getCoupon_max_count() - firstTarget.getCoupon_count();
        int todayCount = NhcouponTargetList.size();
        int loopCount = Math.min(todayCount, available);

        for(int i=0;i<loopCount;i++) {
            Subscribe target = NhcouponTargetList.get(i);
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // POST 요청을 보낼 URL 설정
                HttpPost httpPost = new HttpPost(properties.getProperty("coupon.server"));

                // 전송할 파라미터 설정 (x-www-form-urlencoded 형식)
                List<NameValuePair> requestParams = new ArrayList<>();
                requestParams.add(new BasicNameValuePair("ACTION", "CI102_ISSUECPN_TITLE_WITHPAY"));
                requestParams.add(new BasicNameValuePair("COOPER_ID", properties.getProperty("coupon.megacooperid")));
                requestParams.add(new BasicNameValuePair("COOPER_PW", properties.getProperty("coupon.megacooperpw")));
                requestParams.add(new BasicNameValuePair("SITE_ID", properties.getProperty("coupon.megasiteid")));
                requestParams.add(new BasicNameValuePair("NO_REQ", target.getNo_req()));
                requestParams.add(new BasicNameValuePair("COOPER_ORDER", target.getMobileno()+new Date().getTime()));
                //requestParams.add(new BasicNameValuePair("COOPER_ORDER", "01045273143"+new Date().getTime()));
                requestParams.add(new BasicNameValuePair("ISSUE_COUNT", "1"));
                requestParams.add(new BasicNameValuePair("CALL_CTN", properties.getProperty("coupon.callctn")));
                requestParams.add(new BasicNameValuePair("RCV_CTN", target.getMobileno()));
                //requestParams.add(new BasicNameValuePair("RCV_CTN", "01045273143"));
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
                insertCouponRequest(params);

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
                        insertCouponResponse(xmlParse(responseBody));
                    }
                }
                updateCouponRequestCount(target.getOffercode(), target.getNo_req(), firstTarget.getCoupon_count() + (i + 1));


            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

}
