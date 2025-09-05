package com.dsqd.amc.linkedmo.util;

import com.dsqd.amc.linkedmo.batch.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

public class SchedulerModule {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerModule.class);
    private static Scheduler scheduler;
    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> scheduledFuture;

    public static void startScheduler() { 
        try {
            // Load properties
            Properties properties = new Properties();
            InputStream inputStream = SchedulerModule.class.getClassLoader().getResourceAsStream("application.properties");
            properties.load(inputStream);
            int triggerCount = Integer.parseInt(properties.getProperty("scheduler.trigger.count"));

            // Initialize the scheduler
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();

            // Define job and tie it to our WorkJob class
            JobDetail job = JobBuilder.newJob(WorkJob.class)
                    .withIdentity("workJob", "group1")
                    .storeDurably()
                    .build();

            // Add custom parameters to JobDataMap
            Map<String, Object> params = new HashMap<>();
            params.put("AMC", "Scheduler Setting");
            job.getJobDataMap().put("params", params);

            // Add job to the scheduler
            scheduler.addJob(job, true);

            // Add triggers
            for (int i = 1; i <= triggerCount; i++) {
                String cronExpression = properties.getProperty("scheduler.trigger.cron." + i);
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity("trigger" + i, "group1")
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                                .withMisfireHandlingInstructionDoNothing())
                        .usingJobData("triggerId", i)
                        .forJob(job)
                        .build();
                scheduler.scheduleJob(trigger);
                logger.info("Scheduler trigger {} started with cron expression: {}", i, cronExpression);
            }

            logger.info("Scheduler started.");
        } catch (Exception e) {
            logger.error("Error starting the scheduler", e);
        }
    }

    public static void stopScheduler() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                executorService.shutdownNow();
                logger.info("Scheduler stopped.");
            }
        } catch (Exception e) {
            logger.error("Error stopping the scheduler", e);
        }
    }

    public static boolean isRunning() {
        return !executorService.isShutdown();
    }

    public static class WorkJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            logger.info("Executing job...");

            // Get job parameters
            Map<String, Object> params = (Map<String, Object>) context.getJobDetail().getJobDataMap().get("params");
            int triggerId = context.getTrigger().getJobDataMap().getInt("triggerId");

            // Submit the work to the executor service
            if (triggerId == 1) { 
            	scheduledFuture = executorService.schedule(() -> work(params, triggerId), 0, TimeUnit.SECONDS); 
            } else if (triggerId == 2) {  
            	scheduledFuture = executorService.schedule(() -> pingQuery(params, triggerId), 0, TimeUnit.SECONDS); 
        	} else if (triggerId == 3) {
                scheduledFuture = executorService.schedule(() -> requestCoupon(params, triggerId), 0, TimeUnit.SECONDS);
            } else if (triggerId == 4) {
                scheduledFuture = executorService.schedule(() -> autoPayBatch(params, triggerId), 0, TimeUnit.SECONDS);
            } else if (triggerId == 5) {
                scheduledFuture = executorService.schedule(() -> requestCouponMega(params, triggerId), 0, TimeUnit.SECONDS);
            }
        }
    }

    public static void work(Map<String, Object> params, int triggerId) {
    	Task task = new Batch01();
    	task.executeTask(params, triggerId); 
    }
    
    public static void pingQuery(Map<String, Object> params, int triggerId) {
    	Task task = new Batch02();
    	task.executeTask(params, triggerId); 
    }

    public static void requestCoupon(Map<String, Object> params, int triggerId) {
        Task task = new Batch03();
        task.executeTask(params, triggerId);
    }

    public static void autoPayBatch(Map<String, Object> params, int triggerId) {
        Task task = new Batch04();
        task.executeTask(params, triggerId);
    }

    public static void requestCouponMega(Map<String, Object> params, int triggerId) {
        Task task = new Batch05();
        task.executeTask(params, triggerId);
    }
}
