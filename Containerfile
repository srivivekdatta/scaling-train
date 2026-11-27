FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest
COPY --chmod=0444 build/libs/oracle-lookup.jar /opt/oracle-lookup.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/oracle-lookup.jar"]
