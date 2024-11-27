FROM openjdk:17-alpine
ADD 10-starter-test/60-calendar-management/04-cms-service/target/*.jar service.jar
ADD 10-starter-test/60-calendar-management/04-cms-service/target/uploads /uploads
RUN ls -al /uploads/*
ADD 10-starter-test/60-calendar-management/04-cms-service/target/camel /camel
RUN ls -al /camel/*
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

CMD /wait && java -jar /service.jar
