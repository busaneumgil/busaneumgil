pipeline {
  agent any

  options {
    disableConcurrentBuilds()
  }

  environment {
    REPO_URL = 'https://lab.ssafy.com/s14-final/S14P31E102.git'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'develop', credentialsId: 'gitlab-pat', url: env.REPO_URL
      }
    }

    stage('Prepare Server Config') {
      steps {
        withCredentials([file(credentialsId: 'e102-dev-env-file', variable: 'E102_DEV_ENV')]) {
          sh '''
            cp "$E102_DEV_ENV" .env.dev
            cp /opt/e102-server/docker-compose.s1.override.yml docker-compose.s1.override.yml
            chmod 600 .env.dev
          '''
        }
      }
    }

    stage('Compose Config') {
      steps {
        sh 'docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml --profile graphhopper-build config --quiet'
      }
    }

    stage('Infra Up') {
      steps {
        sh 'docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml up -d postgres redis minio minio-init ai'
      }
    }

    stage('GraphHopper Cache') {
      steps {
        sh '''
          set -eu
          CACHE_VOLUME="s14p31e102-dev_graphhopper-dev-data"
          CACHE_FINGERPRINT="$({
            sha256sum INF/graphhopper/Dockerfile
            sha256sum INF/graphhopper/config-build.yml
            sha256sum INF/graphhopper/config-runtime.yml
            find INF/graphhopper/custom_models -type f | LC_ALL=C sort | while read -r file; do sha256sum "$file"; done
            find INF/graphhopper/plugin/src -type f | LC_ALL=C sort | while read -r file; do sha256sum "$file"; done
            sha256sum scripts/graphhopper/export_postgis_to_osm.py
          } | sha256sum | awk '{print $1}')"

          docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml build graphhopper

          if docker volume inspect "$CACHE_VOLUME" >/dev/null 2>&1 \
            && docker run --rm -e EXPECTED_FINGERPRINT="$CACHE_FINGERPRINT" -v "$CACHE_VOLUME:/graphhopper/data" alpine:3.20 sh -ceu '
              [ -s /graphhopper/data/.ieum-graphhopper-cache-fingerprint ]
              [ "$(cat /graphhopper/data/.ieum-graphhopper-cache-fingerprint)" = "$EXPECTED_FINGERPRINT" ]
              test -n "$(find /graphhopper/data -mindepth 1 -maxdepth 1 ! -name .ieum-graphhopper-cache-fingerprint ! -name .ieum-graphhopper-cache-built-at 2>/dev/null)"
            '; then
            echo "GraphHopper graph-cache fingerprint matches current image/config. Skipping rebuild."
          else
            docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml --profile graphhopper-build build graphhopper-build
            docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml --profile graphhopper-build run --rm \
              -e GRAPHHOPPER_CACHE_FINGERPRINT="$CACHE_FINGERPRINT" \
              graphhopper-build
            docker run --rm \
              -e CACHE_FINGERPRINT="$CACHE_FINGERPRINT" \
              -e CACHE_BUILT_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
              -v "$CACHE_VOLUME:/graphhopper/data" alpine:3.20 sh -ceu '
                printf "%s\n" "$CACHE_FINGERPRINT" > /graphhopper/data/.ieum-graphhopper-cache-fingerprint
                printf "%s\n" "$CACHE_BUILT_AT" > /graphhopper/data/.ieum-graphhopper-cache-built-at
              '
          fi
          docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml up -d graphhopper
        '''
      }
    }

    stage('Backend Deploy') {
      steps {
        sh 'docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml up -d --build --no-deps backend'
      }
    }

    stage('Smoke Test') {
      steps {
        sh '''
          for i in $(seq 1 24); do
            docker run --rm --network s14p31e102-dev_default curlimages/curl:latest -fsS http://backend:8080/v3/api-docs >/tmp/e102-api-docs.json \
              && docker run --rm --network s14p31e102-dev_default curlimages/curl:latest -fsS http://graphhopper:8990/healthcheck >/tmp/e102-graphhopper-health.txt \
              && exit 0
            sleep 5
          done
          docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml logs --tail=120 backend graphhopper
          exit 1
        '''
      }
    }

    stage('Status') {
      steps {
        sh 'docker compose --env-file .env.dev -f docker-compose.dev.yml -f docker-compose.s1.override.yml ps'
      }
    }
  }
}
