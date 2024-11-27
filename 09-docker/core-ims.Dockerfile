FROM openjdk:17-alpine
ADD 10-starter-test/20-identity-management/04-ims-service/target/*.jar service.jar
ADD 10-starter-test/20-identity-management/04-ims-service/target/uploads /uploads
RUN ls -al /uploads/*
ADD 10-starter-test/20-identity-management/04-ims-service/target/camel /camel
RUN ls -al /camel/*
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

CMD /wait && java -jar /service.jar
