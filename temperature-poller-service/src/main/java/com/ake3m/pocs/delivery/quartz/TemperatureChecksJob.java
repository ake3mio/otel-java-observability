package com.ake3m.pocs.delivery.quartz;

import com.ake3m.pocs.domain.TemperatureChecksService;
import com.ake3m.pocs.telemetry.Telemetry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemperatureChecksJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(TemperatureChecksJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        TemperatureChecksService temperatureChecksService = (TemperatureChecksService) dataMap.get("temperatureChecksService");
        Telemetry telemetry = (Telemetry) dataMap.get("telemetry");

        var taskName = "CHECK_TEMPERATURES";
        telemetry.profile(taskName, () -> {
            telemetry.trace(taskName, () -> {
                log.info("Temperature checks started");
                temperatureChecksService.checkTemperatures();
                log.info("Temperature checks finished");
            });
        });

    }
}
