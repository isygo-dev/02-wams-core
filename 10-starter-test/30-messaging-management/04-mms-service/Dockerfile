FROM openjdk:17-alpine
ADD target/*.jar service.jar
ADD target/uploads /uploads
RUN ls -al /uploads/*
ADD target/camel /camel
RUN ls -al /camel/*
ADD target/templates /templates
RUN ls -al /msgtemplate/*
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

CMD /wait && java -jar /service.jar
