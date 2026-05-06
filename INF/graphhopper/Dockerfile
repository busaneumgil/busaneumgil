ARG GRAPHHOPPER_VERSION=11.0

FROM eclipse-temurin:21-jre

ARG GRAPHHOPPER_VERSION

WORKDIR /opt/graphhopper

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates osmium-tool python3 python3-psycopg2 \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o graphhopper-web.jar "https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/${GRAPHHOPPER_VERSION}/graphhopper-web-${GRAPHHOPPER_VERSION}.jar"

COPY INF/graphhopper/config-local.yml /opt/graphhopper/config-local.yml
COPY INF/graphhopper/config-runtime.yml /opt/graphhopper/config-runtime.yml
COPY INF/graphhopper/config-build.yml /opt/graphhopper/config-build.yml
COPY scripts/docker/entrypoints/graphhopper.sh /usr/local/bin/graphhopper-entrypoint.sh
COPY scripts/docker/entrypoints/graphhopper-build.sh /usr/local/bin/graphhopper-build-entrypoint.sh
COPY scripts/graphhopper/export_postgis_to_osm.py /usr/local/bin/export-postgis-to-osm.py

RUN chmod +x /usr/local/bin/graphhopper-entrypoint.sh \
    && chmod +x /usr/local/bin/graphhopper-build-entrypoint.sh \
    && chmod +x /usr/local/bin/export-postgis-to-osm.py

EXPOSE 8989 8990

ENTRYPOINT ["graphhopper-entrypoint.sh"]
