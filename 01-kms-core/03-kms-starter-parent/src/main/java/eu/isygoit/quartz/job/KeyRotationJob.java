package eu.isygoit.quartz.job;

import eu.isygoit.config.AppProperties;
import eu.isygoit.quartz.service.AbstractQuartzJob;
import eu.isygoit.quartz.service.KeyRotationService;
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
 * The type Password expired job.
 * Proactively generate new cryptographic material for active keys to limit the potential impact of a compromised key.
 */
@Slf4j
@Service
public class KeyRotationJob extends AbstractQuartzJob {

    /**
     * The constant groupName.
     */
    public static final String groupName = "um_default";
    /**
     * The constant triggerName.
     */
    public static final String triggerName = "key_rotate_job_trigger";

    private final AppProperties appProperties;

    @Getter
    @Autowired
    private KeyRotationService jobService;

    /**
     * Instantiates a new Password expired job.
     *
     * @param appProperties the app properties
     */
    public KeyRotationJob(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Password expired job detail job detail.
     *
     * @param quartzService the quartz service
     * @return the job detail
     */
    @Bean(name = "keyRotateJobDetail")
    public JobDetail keyRotateJobDetail(@Autowired QuartzService quartzService) {
        return quartzService.createJobDetail(KeyRotationJob.class,
                "keyRotateJobDetail",
                groupName,
                new SingleJobData("name", "World"));
    }

    /**
     * Password expired job trigger trigger.
     *
     * @param quartzService      the quartz service
     * @param keyRotateJobDetail the password expired job detail
     * @return the trigger
     */
    @Bean
    public Trigger keyRotateJobTrigger(@Autowired QuartzService quartzService,
                                       @Autowired @Qualifier("keyRotateJobDetail") JobDetail keyRotateJobDetail) {
        // Run at 00:00 (midnight) every day
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.dailyAtHourAndMinute(2, 0);

        return quartzService.createJobTrigger(
                keyRotateJobDetail,
                triggerName,      // unique trigger name
                KeyRotationJob.groupName,
                scheduleBuilder,
                new Date()                              // start immediately
        );
    }
}
