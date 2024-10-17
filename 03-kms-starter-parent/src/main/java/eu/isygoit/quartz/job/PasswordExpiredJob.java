package eu.isygoit.quartz.job;

import eu.isygoit.config.AppProperties;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.quartz.service.AbstractQuartzJob;
import eu.isygoit.quartz.service.JobSchedulePovider;
import eu.isygoit.quartz.service.PasswordExpiredService;
import eu.isygoit.quartz.service.QuartzService;
import eu.isygoit.quartz.types.SingleJobData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
public class PasswordExpiredJob extends AbstractQuartzJob {

    /**
     * The constant groupName.
     */
    public static final String groupName = "um_default";
    /**
     * The constant triggerName.
     */
    public static final String triggerName = "password_expired_job_trigger";

    private final AppProperties appProperties;

    @Getter
    @Autowired
    private PasswordExpiredService jobService;

    /**
     * Instantiates a new Password expired job.
     *
     * @param appProperties the app properties
     */
    public PasswordExpiredJob(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Password expired job detail job detail.
     *
     * @param quartzService the quartz service
     * @return the job detail
     */
    @Bean(name = "passwordExpiredJobDetail")
    public JobDetail passwordExpiredJobDetail(@Autowired QuartzService quartzService) {
        return quartzService.createJobDetail(PasswordExpiredJob.class,
                "PasswordExpiredJob",
                groupName,
                new SingleJobData("name", "World"));
    }

    /**
     * Password expired job trigger trigger.
     *
     * @param quartzService            the quartz service
     * @param passwordExpiredJobDetail the password expired job detail
     * @return the trigger
     */
    @Bean
    public Trigger passwordExpiredJobTrigger(@Autowired QuartzService quartzService,
                                             @Autowired @Qualifier("passwordExpiredJobDetail") JobDetail passwordExpiredJobDetail) {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
                .simpleSchedule()
                .withIntervalInHours(this.appProperties.getPwdExpiredCheckPeriodDay() * 24)
                .repeatForever();

        return quartzService.createJobTrigger(passwordExpiredJobDetail
                , triggerName
                , PasswordExpiredJob.groupName
                , scheduleBuilder
                , DateHelper.toDate(LocalDateTime.now().plusMinutes(JobSchedulePovider.getStartDelay())));
    }
}
