FROM openjdk:17-alpine
ADD 10-starter-test/30-messaging-management/04-mms-service/target/*.jar service.jar
ADD 10-starter-test/30-messaging-management/04-mms-service/target/uploads /uploads
RUN ls -al /uploads/*
ADD 10-starter-test/30-messaging-management/04-mms-service/target/camel /camel
RUN ls -al /camel/*
ADD 10-starter-test/30-messaging-management/04-mms-service/target/msgtemplate /msgtemplate
RUN ls -al /msgtemplate/*
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

CMD /wait && java -jar /service.jar
