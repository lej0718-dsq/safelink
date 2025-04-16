package com.dsqd.amc.linkedmo.batch;

import com.dsqd.amc.linkedmo.config.MyBatisConfig;
import com.dsqd.amc.linkedmo.service.CouponService;
import com.dsqd.amc.linkedmo.util.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class Batch03 implements Task {
	// PING Query 수행
	
	private static final Logger logger = LoggerFactory.getLogger(Batch03.class);
	
	
	public static void main(String [] args) {
		Batch03 bat = new Batch03();
	}
	
	@Override
    public void executeTask(Map<String, Object> params, int triggerId) {
		Properties prop = MyBatisConfig.getApplicationProperties();
		String env = System.getProperty("argEnv"); // local, dev, prod
		String batch_name = "Batch03-requestCoupon";

		CouponService couponService = new CouponService();
		
        // 여기에 실제 작업 로직 작성
		logger.info("Executing {} with params: {} and triggerId: {}",batch_name, params, triggerId);
        couponService.sendCouponRequest();
    	logger.info("{} executeTask END", batch_name);
	}
}
