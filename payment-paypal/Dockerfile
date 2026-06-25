FROM eclipse-temurin:25-jre-alpine
COPY target/payment-paypal-*.jar /tmp/
RUN set -eux; \
    cp "$(find /tmp -maxdepth 1 -type f -name 'payment-paypal-*.jar' ! -name '*-tests.jar' | head -n 1)" /app.jar; \
    rm -rf /tmp/*.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
