ARG GRAPHHOPPER_VERSION=11.0

FROM eclipse-temurin:21-jre

ARG GRAPHHOPPER_VERSION

WORKDIR /opt/graphhopper

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o graphhopper-web.jar "https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/${GRAPHHOPPER_VERSION}/graphhopper-web-${GRAPHHOPPER_VERSION}.jar"

COPY INF/graphhopper/config-local.yml /opt/graphhopper/config-local.yml
COPY scripts/docker/entrypoints/graphhopper.sh /usr/local/bin/graphhopper-entrypoint.sh

RUN chmod +x /usr/local/bin/graphhopper-entrypoint.sh

EXPOSE 8989 8990

ENTRYPOINT ["graphhopper-entrypoint.sh"]
