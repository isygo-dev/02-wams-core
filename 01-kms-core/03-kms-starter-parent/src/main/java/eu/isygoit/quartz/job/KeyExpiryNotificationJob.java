package eu.isygoit.quartz.job;

import eu.isygoit.config.AppProperties;
import eu.isygoit.quartz.service.AbstractQuartzJob;
import eu.isygoit.quartz.service.KeyExpiryNotificationService;
import eu.isygoit.quartz.service.QuartzService;
import eu.isygoit.quartz.types.SingleJobData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Quartz job that triggers the notification of expiring KMS keys.
 * Runs daily at 09:00 AM (configurable).
 */
@Slf4j
@Service
public class KeyExpiryNotificationJob extends AbstractQuartzJob {

    public static final String groupName = "kms_default";
    public static final String triggerName = "key_expiry_notification_job_trigger";

    private final AppProperties appProperties;

    @Getter
    @Autowired
    private KeyExpiryNotificationService jobService;

    public KeyExpiryNotificationJob(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean(name = "keyExpiryNotificationJobDetail")
    public JobDetail keyExpiryNotificationJobDetail(@Autowired QuartzService quartzService) {
        return quartzService.createJobDetail(
                KeyExpiryNotificationJob.class,
                "keyExpiryNotificationJobDetail",
                groupName,
                new SingleJobData("name", "expiry-check")
        );
    }

    @Bean
    public Trigger keyExpiryNotificationJobTrigger(@Autowired QuartzService quartzService,
                                                   @Autowired @Qualifier("keyExpiryNotificationJobDetail") JobDetail jobDetail) {
        // Run daily at 09:00 AM (you can change this via appProperties if needed)
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.dailyAtHourAndMinute(1, 0);

        return quartzService.createJobTrigger(
                jobDetail,
                triggerName,
                groupName,
                scheduleBuilder,
                new Date() // start immediately
        );
    }
}