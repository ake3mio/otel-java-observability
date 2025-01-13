package com.ake3m.pocs.delivery.quartz;

import com.ake3m.pocs.domain.TemperatureChecksService;
import com.ake3m.pocs.telemetry.Telemetry;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzScheduler {
    private static final Logger log = LoggerFactory.getLogger(QuartzScheduler.class);
    private final TemperatureChecksService temperatureChecksService;
    private final Telemetry telemetry;

    QuartzScheduler(TemperatureChecksService temperatureChecksService, Telemetry telemetry) {
        this.temperatureChecksService = temperatureChecksService;
        this.telemetry = telemetry;
    }

    public void start() throws SchedulerException {
        log.info("Starting QuartzScheduler");
        JobDetail job = JobBuilder.newJob(TemperatureChecksJob.class)
                                  .withIdentity("temperatureChecksJob", "temperatureChecks")
                                  .build();
        job.getJobDataMap().put("temperatureChecksService", temperatureChecksService);
        job.getJobDataMap().put("telemetry", telemetry);

        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                                                           .withIntervalInSeconds(10)
                                                                           .repeatForever();

        Trigger trigger = TriggerBuilder.newTrigger()
                                        .withIdentity("temperatureChecksTrigger", "temperatureChecks")
                                        .startNow()
                                        .withSchedule(simpleScheduleBuilder)
                                        .build();

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }
}
