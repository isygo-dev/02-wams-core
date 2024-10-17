package eu.isygoit.quartz.job;

import eu.isygoit.config.AppProperties;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.quartz.service.AbstractQuartzJob;
import eu.isygoit.quartz.service.InitSuperUserService;
import eu.isygoit.quartz.service.JobSchedulePovider;
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
 * The type Init super user job.
 */
@Slf4j
@Service
public class InitSuperUserJob extends AbstractQuartzJob {

    /**
     * The constant groupName.
     */
    public static final String groupName = "um_default";
    /**
     * The constant triggerName.
     */
    public static final String triggerName = "init_super_user_job_trigger";

    private final AppProperties appProperties;

    @Getter
    @Autowired
    private InitSuperUserService jobService;


    /**
     * Instantiates a new Init super user job.
     *
     * @param appProperties the app properties
     */
    public InitSuperUserJob(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Init super user job detail job detail.
     *
     * @param quartzService the quartz service
     * @return the job detail
     */
    @Bean(name = "initSuperUserJobDetail")
    public JobDetail initSuperUserJobDetail(@Autowired QuartzService quartzService) {
        return quartzService.createJobDetail(InitSuperUserJob.class,
                "InitSuperUserJob",
                groupName,
                new SingleJobData("name", "World"));
    }

    /**
     * Init super user job trigger trigger.
     *
     * @param quartzService          the quartz service
     * @param initSuperUserJobDetail the init super user job detail
     * @return the trigger
     */
    @Bean
    public Trigger initSuperUserJobTrigger(@Autowired QuartzService quartzService,
                                           @Autowired @Qualifier("initSuperUserJobDetail") JobDetail initSuperUserJobDetail) {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
                .simpleSchedule();

        return quartzService.createJobTrigger(initSuperUserJobDetail
                , triggerName
                , InitSuperUserJob.groupName
                , scheduleBuilder
                , DateHelper.toDate(LocalDateTime.now().plusMinutes(JobSchedulePovider.getStartDelay())));
    }
}
