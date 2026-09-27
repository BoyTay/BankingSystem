FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package dependency:copy-dependencies \
    -DincludeScope=runtime -DoutputDirectory=/build/lib

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb x11vnc novnc websockify libgtk-3-0 libgl1 libasound2 \
    libxtst6 libxi6 libxrender1 fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/classes /app/classes
COPY --from=build /build/lib /app/lib
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh \
    && useradd --create-home --uid 10001 banking \
    && mkdir -p /data \
    && mkdir -p /tmp/.X11-unix \
    && chmod 1777 /tmp/.X11-unix \
    && chown banking:banking /data

ENV JAVA_TOOL_OPTIONS="-Dbanking.data.file=/data/banking.db -Dfile.encoding=UTF-8 -Dprism.order=sw"
USER banking
VOLUME ["/data"]
EXPOSE 6080
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["gui"]
