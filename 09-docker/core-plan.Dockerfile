# Build context: 02-wams-core (same as the other core-*.Dockerfile files, e.g. core-mms.Dockerfile).
# The jar is NOT built here; build it first from 02-wams-core:
#   mvn -pl 10-starter-test/80-plan-management/04-plan-service -am package -DskipTests
#
# Differences from core-mms.Dockerfile (deliberate):
#  - glibc based JRE image: the OR-Tools (CP-SAT) native libraries need glibc, so do NOT switch to Alpine;
#  - JRE only (no JDK needed at runtime), apt-get instead of apk for curl;
#  - actuator HEALTHCHECK and JVM memory headroom for the native CP-SAT memory.
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR and additional folders from the host build (same layout as the MMS/pilot host: target/uploads, target/camel)
COPY 10-starter-test/80-plan-management/04-plan-service/target/plan-management-service-*.jar service.jar
COPY 10-starter-test/80-plan-management/04-plan-service/target/uploads /uploads
COPY 10-starter-test/80-plan-management/04-plan-service/target/camel /camel

# Download docker-compose-wait
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

# Keep headroom for the native CP-SAT memory that is outside the Java heap
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60"
ENV SERVER_PORT=40417

EXPOSE 40417

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
    CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health || exit 1

CMD /wait && exec java $JAVA_OPTS -jar service.jar
