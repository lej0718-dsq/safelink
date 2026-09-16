package com.dsqd.amc.linkedmo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.GlobalCache;
import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.mapper.RefundMapper;
import com.dsqd.amc.linkedmo.mobiletown.mobiletownSMS;
import com.dsqd.amc.linkedmo.model.Blocknumber;
import com.dsqd.amc.linkedmo.model.Refund;
import com.dsqd.amc.linkedmo.model.RefundCalculation;
import com.dsqd.amc.linkedmo.model.RefundDetail;
import com.dsqd.amc.linkedmo.model.RefundQuery;
import com.dsqd.amc.linkedmo.model.RefundRequest;
import com.dsqd.amc.linkedmo.model.RefundStatus;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.util.AES256Util;
import com.dsqd.amc.linkedmo.util.AccountMasker;

import net.minidev.json.JSONObject;

/**
 * 환불 신청·조회·완료 처리 비즈니스 로직.
 *
 * 응답 코드 (JSONHelper.assembleResponse 의 code 로 사용):
 *  - 200: 정상
 *  - 901: 가입 이력 없음
 *  - 902: 활성/보류 가입자 (status != 'D')
 *  - 903: 이미 환불 신청 중 (REQUESTED 존재)
 *  - 904: 계산된 환불 금액 0원 이하
 *  - 905: 입력 검증 실패
 *  - 906: 이미 환불 완료
 *  - 909: 이미 완료 처리된 건 (관리자 측)
 *  - 998: 서버 오류
 */
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);
    private static final int DEFAULT_MONTHLY_FEE = 1650;
    /** 번호인증(checkcode) 유효시간 — 발급 후 30분 이내만 신청 허용. */
    private static final long CHECKCODE_TTL_MILLIS = 30 * 60 * 1000L;
    /** 접수 SMS 내 날짜 표기 형식 (예: 2026.07.07). */
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    /** 환불 예정일 = 접수일 기준 영업일 수 (접수일 포함 카운트). */
    private static final int REFUND_BUSINESS_DAYS = 7;

    private final SqlSessionFactory sqlSessionFactory;
    private final SubscribeService subscribeService;
    private final BlocknumberService blocknumberService;
    private final RefundCalculator calculator;

    public RefundService() {
        this.sqlSessionFactory = MyBatisConfig.getSqlSessionFactory();
        this.subscribeService = new SubscribeService();
        this.blocknumberService = new BlocknumberService();
        this.calculator = new RefundCalculator();
    }

    /** application.properties 의 service.fee.monthly 를 읽음. 없으면 1650. */
    public static int getMonthlyFee() {
        Object v = GlobalCache.getInstance().get("service.fee.monthly");
        if (v == null) return DEFAULT_MONTHLY_FEE;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return DEFAULT_MONTHLY_FEE;
        }
    }

    // ============================================================
    // 환불 신청 (사용자)
    // ============================================================

    /**
     * 환불 신청 처리 결과 (서비스 → 컨트롤러).
     */
    public static class ApplyResult {
        public final int code;
        public final String msg;
        public final Refund refund;            // 성공 시 채워짐
        public final RefundCalculation calc;   // 성공 시 채워짐

        public ApplyResult(int code, String msg, Refund refund, RefundCalculation calc) {
            this.code = code;
            this.msg = msg;
            this.refund = refund;
            this.calc = calc;
        }

        public static ApplyResult fail(int code, String msg) {
            return new ApplyResult(code, msg, null, null);
        }

        public static ApplyResult ok(Refund refund, RefundCalculation calc) {
            return new ApplyResult(200, "", refund, calc);
        }
    }

    public ApplyResult apply(RefundRequest req) {
        // 1) 입력 검증
        String validationError = validate(req);
        if (validationError != null) {
            logger.info("Refund validation failed: {}", validationError);
            return ApplyResult.fail(905, validationError);
        }

        String phone = req.getPhone();

        // 1.5) 번호인증(checkcode) 서버 재검증 — OTP 인증을 거친 요청인지 확인
        String authError = verifyCheckcode(req.getCheckcode(), phone);
        if (authError != null) {
            logger.info("Refund apply: checkcode invalid for {} - {}",
                AccountMasker.maskPhone(phone), authError);
            return ApplyResult.fail(907, authError);
        }

        // 2~6) 환불 자격 검증 + 금액 계산 (precheck 와 동일 로직 공용)
        Eligibility el = checkEligibility(phone);
        if (el.code != 200) {
            return ApplyResult.fail(el.code, el.msg);
        }
        Subscribe subscribe = el.subscribe;
        RefundCalculation calc = el.calc;

        // 7) 계좌번호 암호화
        String accountEnc;
        try {
            accountEnc = AES256Util.encrypt(req.getAccount());
        } catch (Exception e) {
            logger.error("Refund apply: account encryption failed", e);
            return ApplyResult.fail(998, "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        // 8) 저장 (Refund + RefundDetail) — 트랜잭션
        Refund refund = Refund.builder()
            .mobileno(phone)
            .name(req.getName().trim())
            .birth(req.getBirth())
            .bankCode(req.getBankCode())
            .bankName(req.getBankName())
            .accountEnc(accountEnc)
            .subscribeId(subscribe.getId())
            .useStartDate(toDate(calc.getUseStartDate()))
            .useEndDate(toDate(calc.getUseEndDate()))
            .refundAmount(calc.getTotalAmount())
            .status(RefundStatus.REQUESTED.name())
            .build();

        try (SqlSession session = sqlSessionFactory.openSession()) {
            RefundMapper mapper = session.getMapper(RefundMapper.class);
            mapper.insertRefund(refund);
            long refundId = refund.getId();

            for (RefundDetail d : calc.getDetails()) {
                d.setRefundId(refundId);
                mapper.insertRefundDetail(d);
            }
            session.commit();
        } catch (Exception e) {
            logger.error("Refund apply: insert failed", e);
            return ApplyResult.fail(998, "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        logger.info("Refund applied: id={}, mobileno={}, amount={}",
            refund.getId(), AccountMasker.maskPhone(phone), calc.getTotalAmount());

        // 9) 접수 완료 SMS 발송 (best-effort — 실패해도 환불 접수는 성공으로 본다)
        sendReceiptSms(phone, refund);

        // 10) 재가입 차단 신청 처리 (환불 INSERT 와 분리: 여기서 실패해도 환불은 성공으로 본다)
        //    status / createat 컬럼은 DB DEFAULT 값을 사용하므로 명시하지 않음
        if (req.isBlockRejoin()) {
            try {
                Blocknumber bn = Blocknumber.builder()
                    .spcode("SKT")
                    .mobileno(phone)
                    .usernameofoper("admin")
                    .remark("미인지")
                    .build();
                blocknumberService.insertBlocknumber(bn);
                logger.info("Block-rejoin registered: mobileno={}, refundId={}",
                    AccountMasker.maskPhone(phone), refund.getId());
            } catch (Exception e) {
                logger.error("Block-rejoin insert failed (refund still OK): mobileno={}, refundId={}",
                    AccountMasker.maskPhone(phone), refund.getId(), e);
            }
        }

        return ApplyResult.ok(refund, calc);
    }

    // ============================================================
    // 번호인증 직후 환불 자격 미리보기 (레코드 생성 없음)
    // ============================================================

    /** precheck 결과 (서비스 → 컨트롤러). code==200 이면 금액/기간 채워짐. */
    public static class PrecheckResult {
        public final int code;
        public final String msg;
        public final Integer refundAmount;   // 200 일 때만
        public final Date useStartDate;
        public final Date useEndDate;

        public PrecheckResult(int code, String msg, Integer refundAmount, Date useStartDate, Date useEndDate) {
            this.code = code;
            this.msg = msg;
            this.refundAmount = refundAmount;
            this.useStartDate = useStartDate;
            this.useEndDate = useEndDate;
        }
    }

    /**
     * 번호인증 성공 직후 환불 자격/금액 미리보기 (계좌 입력 전 조기 반려용).
     * 레코드를 생성하지 않으며 apply() 와 동일한 자격 로직을 공유한다.
     */
    public PrecheckResult precheck(String phoneRaw) {
        String phone = phoneRaw == null ? "" : phoneRaw.replaceAll("[^0-9]", "");
        if (!phone.matches("^010\\d{8}$")) {
            return new PrecheckResult(905, "휴대폰번호가 올바르지 않습니다.", null, null, null);
        }
        Eligibility el = checkEligibility(phone);
        if (el.code != 200) {
            return new PrecheckResult(el.code, el.msg, null, null, null);
        }
        return new PrecheckResult(200, "",
            el.calc.getTotalAmount(),
            toDate(el.calc.getUseStartDate()),
            toDate(el.calc.getUseEndDate()));
    }

    // ============================================================
    // 자격 검증 (apply / precheck 공용) + 번호인증 검증 + 접수 SMS
    // ============================================================

    /** 자격 검증 결과 내부 전달용. code==200 이면 subscribe/calc 채워짐. */
    private static class Eligibility {
        final int code;
        final String msg;
        final Subscribe subscribe;
        final RefundCalculation calc;

        private Eligibility(int code, String msg, Subscribe subscribe, RefundCalculation calc) {
            this.code = code;
            this.msg = msg;
            this.subscribe = subscribe;
            this.calc = calc;
        }

        static Eligibility fail(int code, String msg) {
            return new Eligibility(code, msg, null, null);
        }

        static Eligibility ok(Subscribe subscribe, RefundCalculation calc) {
            return new Eligibility(200, "", subscribe, calc);
        }
    }

    /**
     * 환불 자격 검증 + 금액 계산 (레코드 생성 없음).
     * apply() 와 precheck() 가 공유하여 미리보기 금액과 실제 신청 금액이 항상 일치하도록 한다.
     */
    private Eligibility checkEligibility(String phone) {
        // 가입 이력 확인 — 가장 마지막 가입 레코드 기준
        Subscribe subscribe = subscribeService.selectLatestByMobileno(phone);
        if (subscribe == null) {
            return Eligibility.fail(901, "가입 이력이 없는 번호입니다. 콜센터로 문의해주세요.");
        }
        // 상태 검증: 'D' (해지) 만 허용
        if (!"D".equals(subscribe.getStatus())) {
            return Eligibility.fail(902, "서비스 해지 후 신청 가능합니다.");
        }
        // 가입일/해지일 유효성
        if (subscribe.getCreateDate() == null || subscribe.getCancelDate() == null) {
            logger.warn("Refund eligibility: missing create/cancel date for subscribe id={}", subscribe.getId());
            return Eligibility.fail(998, "가입/해지 일자 정보가 없습니다. 콜센터로 문의해주세요.");
        }
        // 중복 신청 차단
        try (SqlSession session = sqlSessionFactory.openSession()) {
            RefundMapper mapper = session.getMapper(RefundMapper.class);
            int requested = mapper.countByMobilenoAndStatus(phone, RefundStatus.REQUESTED.name());
            if (requested > 0) {
                return Eligibility.fail(903, "환불 신청 처리 중입니다. 완료 후 다시 확인해주세요.");
            }
            int completed = mapper.countByMobilenoAndStatus(phone, RefundStatus.COMPLETED.name());
            if (completed > 0) {
                return Eligibility.fail(906, "이미 환불이 완료된 번호입니다.");
            }
        }
        // 환불 금액 계산
        LocalDate createLd = toLocalDate(subscribe.getCreateDate());
        LocalDate cancelLd = toLocalDate(subscribe.getCancelDate());
        RefundCalculation calc = calculator.calculate(createLd, cancelLd, getMonthlyFee());
        if (calc.getTotalAmount() <= 0) {
            return Eligibility.fail(904, "환불 가능 금액이 없습니다.");
        }
        return Eligibility.ok(subscribe, calc);
    }

    /**
     * 번호인증 토큰(checkcode) 검증. 정상이면 null, 실패면 사용자 메시지 반환.
     * checkcode = AES256(mobileno|rnumber|issuedAtMillis) — checkotp 성공 시 발급됨.
     */
    private String verifyCheckcode(String encCheckcode, String phone) {
        if (encCheckcode == null || encCheckcode.trim().isEmpty()) {
            return "휴대폰 번호 인증 후 신청해주세요.";
        }
        try {
            String decoded = AES256Util.decrypt(encCheckcode);
            String[] parts = decoded.split("\\|");
            if (parts.length < 3) {
                return "인증 정보가 올바르지 않습니다. 다시 인증해주세요.";
            }
            String cmobileno = parts[0];
            // 개발환경(argEnv=dev)에서는 전화번호 일치 검사를 생략
            boolean isDev = "dev".equals(System.getProperty("argEnv"));
            if (!isDev && !cmobileno.equals(phone)) {
                logger.warn("Refund checkcode phone mismatch: token={}, req={}",
                    AccountMasker.maskPhone(cmobileno), AccountMasker.maskPhone(phone));
                return "인증한 번호와 신청 번호가 일치하지 않습니다. 다시 인증해주세요.";
            }
            long issuedAt = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() - issuedAt > CHECKCODE_TTL_MILLIS) {
                return "인증 유효시간이 만료되었습니다. 다시 인증해주세요.";
            }
            return null;
        } catch (Exception e) {
            logger.error("Refund checkcode decrypt error: {}", e.getMessage());
            return "인증 정보가 올바르지 않습니다. 다시 인증해주세요.";
        }
    }

    /** 환불 접수 완료 SMS 발송 (TB_AGT_SMS_TRAN insert, best-effort — 실패해도 환불 접수는 성공). */
    private void sendReceiptSms(String phone, Refund refund) {
        try {
            LocalDate today = LocalDate.now();
            String receiptDate  = DOT_DATE.format(today);
            String expectedDate = DOT_DATE.format(nthBusinessDay(today, REFUND_BUSINESS_DAYS));

            String message =
                  "휴대폰약속번호 서비스 환불 신청이 정상적으로 접수되었습니다.\n"
                + "- 접수일 : " + receiptDate + "\n"
                + "- 환불 예정일 : " + expectedDate + " (영업일 기준 7일 이내)\n"
                + "- 입금자명 : 올마이크레딧주식회사\n"
                + "신청 내용은 제휴사에서 확인 후 처리되며, 추가 확인이 필요한 경우 별도로 연락드릴 수 있습니다.\n"
                + "\n"
                + "(1533-5278) 휴대폰약속번호 고객센터";

            // 장문(90byte 초과)이라 기존 SMS 발송기(mobiletownSMS)의 MMS 경로(TB_AGT_TRAN)로 발송
            new mobiletownSMS().sendMms(phone, "휴대폰약속번호 환불 접수 안내", message);
            logger.info("Refund receipt MMS queued: mobileno={}, refundId={}",
                AccountMasker.maskPhone(phone), refund.getId());
        } catch (Exception e) {
            logger.error("Refund receipt SMS failed (refund still OK): mobileno={}, refundId={}",
                AccountMasker.maskPhone(phone), refund.getId(), e);
        }
    }

    /** 관리자 환불 완료 처리 시 입금 예정 안내 MMS 발송 (best-effort — 실패해도 완료 처리는 성공). */
    private void sendCompletedSms(Refund refund) {
        try {
            String amount  = String.format("%,d", refund.getRefundAmount());
            String payDate = DOT_DATE.format(LocalDate.now());

            String message =
                  "신청하신 휴대폰약속번호서비스 환불 예정 안내 드립니다.\n"
                + "- 환불금액 : " + amount + "원\n"
                + "- 입금일 : " + payDate + "\n"
                + "- 입금자명 : 올마이크레딧주식회사\n"
                + "\n"
                + "(1533-5278) 휴대폰약속번호 고객센터";

            new mobiletownSMS().sendMms(refund.getMobileno(), "휴대폰약속번호 환불 예정 안내", message);
            logger.info("Refund completed MMS queued: mobileno={}, refundId={}",
                AccountMasker.maskPhone(refund.getMobileno()), refund.getId());
        } catch (Exception e) {
            logger.error("Refund completed MMS failed (complete still OK): refundId={}", refund.getId(), e);
        }
    }

    /**
     * 접수일 기준 N번째 영업일(월~금, 주말 제외)을 반환한다.
     * 접수일이 영업일이면 접수일을 1번째로 카운트한다 (예: 월요일 접수 → 7영업일째 = 다음 주 화요일).
     * ※ 법정 공휴일은 반영하지 않는다.
     */
    private static LocalDate nthBusinessDay(LocalDate start, int n) {
        LocalDate d = start;
        int counted = isBusinessDay(d) ? 1 : 0;
        while (counted < n) {
            d = d.plusDays(1);
            if (isBusinessDay(d)) counted++;
        }
        return d;
    }

    private static boolean isBusinessDay(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    // ============================================================
    // 관리자 — 조회
    // ============================================================

    public List<Refund> list(RefundQuery q) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(RefundMapper.class).selectByQuery(q);
        }
    }

    public Refund get(long id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(RefundMapper.class).selectById(id);
        }
    }

    public List<RefundDetail> getDetails(long refundId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(RefundMapper.class).selectDetailsByRefundId(refundId);
        }
    }

    /** 저장된 암호문에서 평문 계좌번호 복호화. 실패 시 null. */
    public String decryptAccount(String accountEnc) {
        if (accountEnc == null || accountEnc.isEmpty()) return null;
        try {
            return AES256Util.decrypt(accountEnc);
        } catch (Exception e) {
            logger.warn("Account decrypt failed: {}", e.getMessage());
            return null;
        }
    }

    // ============================================================
    // 관리자 — 환불 완료 처리
    // ============================================================

    /** 결과 코드: 200(성공) / 909(이미 완료) / 404(존재 안함) / 998(오류) */
    public int markCompleted(long id, String adminId, String memo) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            RefundMapper mapper = session.getMapper(RefundMapper.class);
            Refund existing = mapper.selectById(id);
            if (existing == null) return 404;
            if (!RefundStatus.REQUESTED.name().equals(existing.getStatus())) return 909;

            int updated = mapper.updateToCompleted(id, adminId, memo);
            session.commit();
            if (updated == 1) {
                sendCompletedSms(existing);   // 환불 완료(입금 예정) 안내 MMS
                return 200;
            }
            return 998;
        } catch (Exception e) {
            logger.error("markCompleted failed: id={}", id, e);
            return 998;
        }
    }

    /**
     * 관리자: 환불 정보 확인 요청 안내 MMS 발송 (계좌번호 오류 등으로 확인이 필요할 때).
     * 상태는 변경하지 않고 안내 문자만 발송한다. 결과: 200(성공) / 404(내역 없음) / 998(발송 실패).
     */
    public int notifyInfoCheck(long id) {
        Refund refund;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            refund = session.getMapper(RefundMapper.class).selectById(id);
        }
        if (refund == null) return 404;

        String message =
              "[올마이크레딧]\n"
            + "\n"
            + "신청하신 환불 건의 정보 확인이 필요합니다.\n"
            + "\n"
            + "제휴사에서 연락드릴 예정이오니 확인 부탁드립니다.\n"
            + "\n"
            + "(1533-5278) 휴대폰약속번호 고객센터";

        JSONObject r = new mobiletownSMS().sendMms(refund.getMobileno(), "휴대폰약속번호 환불 정보 확인 요청", message);
        if (200 == (int) r.get("code")) {
            logger.info("Refund info-check MMS queued: mobileno={}, refundId={}",
                AccountMasker.maskPhone(refund.getMobileno()), refund.getId());
            return 200;
        }
        logger.error("Refund info-check MMS failed: refundId={}, resp={}", id, r.toJSONString());
        return 998;
    }

    // ============================================================
    // 입력 검증
    // ============================================================

    private static String validate(RefundRequest req) {
        if (req == null) return "요청 본문이 없습니다.";

        String name = trim(req.getName());
        if (name.isEmpty() || !name.matches("[가-힣a-zA-Z\\s]{2,20}")) {
            return "이름을 정확히 입력해주세요.";
        }
        String birth = trim(req.getBirth());
        if (!birth.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return "생년월일을 YYYY-MM-DD 형식으로 입력해주세요.";
        }
        try {
            LocalDate b = LocalDate.parse(birth);
            if (b.getYear() < 1900 || b.isAfter(LocalDate.now())) {
                return "생년월일이 유효하지 않습니다.";
            }
        } catch (Exception e) {
            return "생년월일이 유효하지 않습니다.";
        }
        String phone = req.getPhone() == null ? "" : req.getPhone().replaceAll("[^0-9]", "");
        req.setPhone(phone); // 정규화
        if (!phone.matches("^010\\d{8}$")) {
            return "010으로 시작하는 휴대폰번호를 입력해주세요.";
        }
        String bankCode = trim(req.getBankCode());
        if (bankCode.isEmpty() || bankCode.length() > 20) {
            return "은행을 선택해주세요.";
        }
        String bankName = trim(req.getBankName());
        if (bankName.isEmpty()) {
            return "은행명이 누락되었습니다.";
        }
        String account = req.getAccount() == null ? "" : req.getAccount().replaceAll("[^0-9]", "");
        req.setAccount(account); // 정규화
        if (!account.matches("^\\d{8,20}$")) {
            return "계좌번호를 정확히 입력해주세요. (숫자 8~20자리)";
        }
        return null;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
