package com.nxg.mall.analytics.config;

import com.nxg.mall.analytics.job.BusinessEventAnalysisJob;
import com.nxg.mall.analytics.job.SystemAuditAnalysisJob;
import com.nxg.mall.analytics.job.TrafficAnalysisJob;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Component
public class FlinkJobInitializer implements CommandLineRunner {

    // 创建一个包含3个线程的线程池，分别给3个Flink任务使用
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Override
    public void run(String... args) throws Exception {
        // 提取标准的外部数据库配置变量（可以通过 Spring 的 @Value 注入，也可以用默认值）
        String[] jobArgs = new String[]{
                "--db.url", "jdbc:mysql://nxg-mysql:3306/nxg_analytics?useSSL=false&serverTimezone=Asia/Shanghai",
                "--db.user", "nxg_user",
                "--db.pass", "root"
        };

        System.out.println("====== 🌊 Spring Boot 正在通过内部线程安全拉起三大 Flink 作业 ======");

        // 1. 异步启动流量分析 Job
        executorService.submit(() -> {
            try {
                TrafficAnalysisJob.main(jobArgs);
            } catch (Exception e) {
                System.err.println("❌ 流量分析 Job 异常退出了：");
                e.printStackTrace();
            }
        });

        // 2. 异步启动业务事件分析 Job
        executorService.submit(() -> {
            try {
                BusinessEventAnalysisJob.main(jobArgs);
            } catch (Exception e) {
                System.err.println("❌ 业务事件分析 Job 异常退出了：");
                e.printStackTrace();
            }
        });

        // 3. 异步启动系统审计分析 Job
        executorService.submit(() -> {
            try {
                SystemAuditAnalysisJob.main(jobArgs);
            } catch (Exception e) {
                System.err.println("❌ 系统审计分析 Job 异常退出了：");
                e.printStackTrace();
            }
        });
    }
}