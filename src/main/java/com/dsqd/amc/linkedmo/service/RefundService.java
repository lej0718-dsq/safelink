package com.dsqd.amc.linkedmo.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.GlobalCache;
import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.mapper.RefundMapper;
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

        // 2) 가입 이력 확인 — 가장 마지막 가입 레코드 기준
        Subscribe subscribe = subscribeService.selectLatestByMobileno(phone);
        if (subscribe == null) {
            logger.info("Refund apply: no subscribe history for {}", AccountMasker.maskPhone(phone));
            return ApplyResult.fail(901, "가입 이력이 없는 번호입니다. 콜센터로 문의해주세요.");
        }

        // 3) 상태 검증: 'D' (해지) 만 허용
        if (!"D".equals(subscribe.getStatus())) {
            logger.info("Refund apply: subscribe not cancelled (status={}) for {}",
                subscribe.getStatus(), AccountMasker.maskPhone(phone));
            return ApplyResult.fail(902, "서비스 해지 후 신청 가능합니다.");
        }

        // 4) 가입일/해지일 유효성
        if (subscribe.getCreateDate() == null || subscribe.getCancelDate() == null) {
            logger.warn("Refund apply: missing create/cancel date for subscribe id={}", subscribe.getId());
            return ApplyResult.fail(998, "가입/해지 일자 정보가 없습니다. 콜센터로 문의해주세요.");
        }

        // 5) 중복 신청 차단
        try (SqlSession session = sqlSessionFactory.openSession()) {
            RefundMapper mapper = session.getMapper(RefundMapper.class);
            int requested = mapper.countByMobilenoAndStatus(phone, RefundStatus.REQUESTED.name());
            if (requested > 0) {
                return ApplyResult.fail(903, "환불 신청 처리 중입니다. 완료 후 다시 확인해주세요.");
            }
            int completed = mapper.countByMobilenoAndStatus(phone, RefundStatus.COMPLETED.name());
            if (completed > 0) {
                return ApplyResult.fail(906, "이미 환불이 완료된 번호입니다.");
            }
        }

        // 6) 환불 금액 계산
        LocalDate createLd = toLocalDate(subscribe.getCreateDate());
        LocalDate cancelLd = toLocalDate(subscribe.getCancelDate());
        int monthlyFee = getMonthlyFee();

        RefundCalculation calc = calculator.calculate(createLd, cancelLd, monthlyFee);
        if (calc.getTotalAmount() <= 0) {
            logger.info("Refund apply: amount={} (<=0) for subscribe id={}",
                calc.getTotalAmount(), subscribe.getId());
            return ApplyResult.fail(904, "환불 가능 금액이 없습니다.");
        }

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

        // 9) 재가입 차단 신청 처리 (환불 INSERT 와 분리: 여기서 실패해도 환불은 성공으로 본다)
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
            return updated == 1 ? 200 : 998;
        } catch (Exception e) {
            logger.error("markCompleted failed: id={}", id, e);
            return 998;
        }
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
