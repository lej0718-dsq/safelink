package com.dsqd.amc.linkedmo.batch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.model.Batch;
import com.dsqd.amc.linkedmo.model.Batchlog;
import com.dsqd.amc.linkedmo.model.Board;
import com.dsqd.amc.linkedmo.model.Subscribe;
import com.dsqd.amc.linkedmo.naru.SubscribeNaru;
import com.dsqd.amc.linkedmo.service.BatchService;
import com.dsqd.amc.linkedmo.service.BatchlogService;
import com.dsqd.amc.linkedmo.service.BoardService;
import com.dsqd.amc.linkedmo.service.SubscribeService;
import com.dsqd.amc.linkedmo.skt.APICall;
import com.dsqd.amc.linkedmo.skt.SubscribeSK;
import com.dsqd.amc.linkedmo.util.InterfaceManager;
import com.dsqd.amc.linkedmo.util.Task;

import ch.qos.logback.core.recovery.ResilientSyslogOutputStream;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class Batch01 implements Task {
	// 일배치 수행
	
	private static final Logger logger = LoggerFactory.getLogger(Batch01.class);
	
	
	public static void main(String [] args) {
		Batch01 bat = new Batch01();
		bat.runBatch();
	}
	
    public int checkToday(Date date) {
        // LocalDate로 변환
    	if (date == null) return -1;
        LocalDate recordDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // 오늘과 어제 날짜를 가져옴
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 날짜 비교
        if (recordDate.isEqual(today)) {
            return 1;
        } else if (recordDate.isEqual(yesterday)) {
            return 0;
        } else {
            return -1;
        }
    }

	@Override
    public void executeTask(Map<String, Object> params, int triggerId) {
		Properties prop = MyBatisConfig.getApplicationProperties();
		String env = System.getProperty("argEnv"); // local, dev, prod
		String batch_name = "Batch01-PayUserCheck";
		
		SubscribeService service = new SubscribeService();
		BatchService bService = new BatchService();
		BatchlogService blogService = new BatchlogService();
		
		InterfaceManager itfMgr = InterfaceManager.getInstance();
		
        // 여기에 실제 작업 로직 작성
		logger.info("Executing {} with params: {} and triggerId: {}",batch_name, params, triggerId);
        // 예시로, long running task 시뮬레이션
			Batch batch01 = Batch.builder().batchid(batch_name).code("000").result("").build();
			bService.insertBatch(batch01);
			logger.info("Batch txid:{}", batch01.getTxid());
        	List <Subscribe> data = service.getSubscribeAlltoBatch01();
        	int total_user = 0;
        	int subscribe_user = 0;
        	int subscribe_yesterday_user = 0; // 어제 가입한 도수 
        	int cancel_user = 0;
        	int cancel_yesterday_user = 0; // 어제 해지한 도수 
        	
    		if (data != null && data.size() > 0) {
    			total_user = data.size();
    			logger.info("BatchName : [{}/{}] - Work Size : {}", batch_name, env, data.size());
    			for (Subscribe u: data ) {
					try {
						if ("A".equals(u.getStatus()) ) { // 유효한 사용자 목록
							subscribe_user++;
							String category = "";
							String mobileno = u.getMobileno();
							APICall api = new APICall();

							//어제 가입자 분류
							if (checkToday(u.getCreateDate()) == 0) {
								subscribe_yesterday_user++;
								logger.info("YESTERDAY DESCRIBE USER : {} | {}", u.getMobileno(), u.getCreateDate() );
							}

							// SK 사용자 확인
							SubscribeSK skt = new SubscribeSK();
							JSONObject responseJSON = skt.prove(u.getMobileno());
							if (200 != (int) responseJSON.get("code")) {
								//type : T01 - SK콜센터 취소 , T02 - KT콜센터 취소 , T03 - LGU콜센터 취소
								category = "T0";
								Batchlog blog = Batchlog.builder().batch_txid(batch01.getTxid()).type("T01").comment(u.getSpcode() + ":" + u.getMobileno()).actcode("000").build();
								logger.info("[{}] = [{}]CALLCENTER CANCELED : MOBILENO - {}", blog.getType(), u.getSpcode(), u.getMobileno());
								blogService.insertBatchlog(blog); // 배치DB에 남김
								service.deleteSubscribeT0(u.getId()); // 삭제처리
							}
							//String resp = api.ISICS00022(mobileno);
							//해지된 사용자라면 우리쪽도 해지처리함 -> RESULT가 F이면 안되는 것임
							// @TODO


							// 나루의 사용자 확인
							String method = "POST";
							String uri = "/api/v1.0/linksafe/getdata";

							SubscribeNaru naru = new SubscribeNaru();
							responseJSON = naru.getData(u);
							logger.info("({}) NARU DATA STATUS: [{}][{}]", String.format("%06d", subscribe_user), responseJSON.getAsString("nowstatutsut"), u.getMobileno());

							if ("T0".equals(category)) {
								Batchlog blog = Batchlog.builder().batch_txid(batch01.getTxid()).type("T01").comment("NARU" + ":" + u.getMobileno()).actcode("000").build();
								logger.info("[{}] = [{}]-RELAYED CANCEL TO NARU : MOBILENO - {}", "T0", u.getSpcode(), u.getMobileno());
								naru.cancel(u);
							}

						} else {
							// 해지자
							cancel_user++;
							//System.out.println(u.getStatus());
							//어제 해지자 분류 (0)
							String just = "";
							if (checkToday(u.getCancelDate()) == -1) {

							} else if (checkToday(u.getCancelDate()) == 0) {
								just="** YESTERDAY ";
								cancel_yesterday_user++;
								// 어제 해지한 사람이 정상적으로 해지되었는지 상태 확인 필요 -> 안되어 있다면 다시 해지
								SubscribeSK skt = new SubscribeSK();
								JSONObject responseJSON = skt.prove(u.getMobileno());
								if (200 == (int) responseJSON.get("code")) { // 서비스가 가입되어 있다면 문제 => 관리자에 알려야 함
									// actcode : A01 -> 관리자 처리사항
									//type : U01 - SK 시스템과 싱크가 맞지 않음, 확인 필요
									Batchlog blog = Batchlog.builder().batch_txid(batch01.getTxid()).type("T01").comment("NOT SYNC:" + u.getSpcode() + ":" + u.getMobileno()).actcode("A01").build();
									logger.info("[{}] = [{}] SYNC FAULT : MOBILENO - {}", blog.getType(), u.getSpcode(), u.getMobileno());
									blogService.insertBatchlog(blog); // 배치DB에 남김
								}

							} if (checkToday(u.getCancelDate()) == 1) {
								just="TODAY ";
							}
							logger.info("{}CANCEL USER : {} [{}] at {}", just, u.getMobileno(), u.getCncode(), u.getCancelDate() );
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
    				
    			}
    			//return;
    		} else {
    			logger.info("No described User");
//    			System.out.println("No described User");
    		}
    		
            String result = String.format("TOTAL BATCH USER COUNT (%d) / PAYED USER COUNT (%d) / CANCELED USER COUNT (%d)", total_user, subscribe_user, cancel_user);
            result = result + "\n" + String.format("YESTERDAY SUMMARY : PAYED USER COUNT (%d) / CANCELED USER COUNT (%d)", subscribe_yesterday_user, cancel_yesterday_user);
    		batch01.setResult(result);
    		bService.updateBatch(batch01);
    		logger.info("{} RESULT: TOTAL BATCH COUNT {}, subscribes {}, cancels {}", batch_name, total_user, subscribe_user, cancel_user);
    		logger.info("{} executeTask END", batch_name);
    		
//        System.out.println("Task completed.");
	}
    
	
	public void runBatch() {
		logger.info("Start Batch01");
		// 1. DB에서 유효한 사용자 목록을 가져온다. 

		// 2. 목록의 전화번호로 SKT에 서비스 가입여부 조회를 함 (ISICS00022) -- loop
		
		// 2-1. 만약 가입이 안되어 있다면, 서비스관리번호(svc_mgmt_num)로 (ISICS00021) 조회를 함
		
		// 2-2. 수집한 전화번호가 현재 번호와 같다면, 나루에 해지 전문 발송
		
		// 2-3. 수집한 전화번호가 현재 번호와 다르다면, 수집한 전화번호로 SKT에 서비스 가입여부 조회를 함 (ISICS00022)
		
		// 2-4. 서비스가 가입되어 있다면, 작업로그에 남겨서 관리자가 조회할 수 있도록 함 (전화번호 변경)
		
		// 2-5. 서비스가 가입되어 있지 않다면, 나루에 해지 전문 발송
		
		// 3. 나루에 해지전문을 호출하여, 정상처리되었으면 작업로그에 남겨서 관리자가 조회할 수 있도록 함 (통신사 해지) 

		// 4. DB에서 서비스 해지처리를 함 -- loop end
		
		// 5. 작업수행통계를 작업로그에 남겨서 관리자가 조회할 수 있도록 함 
		
	}
}
