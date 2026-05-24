package eu.isygoit.quartz.job;

import eu.isygoit.config.AppProperties;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.quartz.service.*;
import eu.isygoit.quartz.types.SingleJobData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * The type Password expired job.
 */
@Slf4j
@Service
public class KeyDeletionJob extends AbstractQuartzJob {

    /**
     * The constant groupName.
     */
    public static final String groupName = "um_default";
    /**
     * The constant triggerName.
     */
    public static final String triggerName = "key_deletion_job_trigger";

    private final AppProperties appProperties;

    @Getter
    @Autowired
    private KeyDeletionService jobService;

    /**
     * Instantiates a new Password expired job.
     *
     * @param appProperties the app properties
     */
    public KeyDeletionJob(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Password expired job detail job detail.
     *
     * @param quartzService the quartz service
     * @return the job detail
     */
    @Bean(name = "keyDeletionJobDetail")
    public JobDetail keyDeletionJobDetail(@Autowired QuartzService quartzService) {
        return quartzService.createJobDetail(KeyDeletionJob.class,
                "keyDeletionJobDetail",
                groupName,
                new SingleJobData("name", "World"));
    }

    /**
     * Password expired job trigger trigger.
     *
     * @param quartzService            the quartz service
     * @param keyDeletionJobDetail the password expired job detail
     * @return the trigger
     */
    @Bean
    public Trigger keyDeletionJobTrigger(@Autowired QuartzService quartzService,
                                         @Autowired @Qualifier("keyDeletionJobDetail") JobDetail keyDeletionJobDetail) {
        // Cron expression: run at 00:00 every day
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .dailyAtHourAndMinute(0, 0); // midnight

        return quartzService.createJobTrigger(
                keyDeletionJobDetail,
                triggerName,          // trigger name
                KeyDeletionJob.groupName,         // group name
                scheduleBuilder,
                null                              // no start delay needed for cron
        );
    }
}
