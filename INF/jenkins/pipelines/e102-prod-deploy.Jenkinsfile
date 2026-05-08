pipeline {
  agent any

  options {
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'DEPLOY_BRANCH', defaultValue: 'master', description: 'Git branch to deploy to S2 prod')
    booleanParam(name: 'BUILD_GRAPHHOPPER', defaultValue: false, description: 'Build graph-cache from PostgreSQL before deploying GraphHopper')
    booleanParam(name: 'DEPLOY_GRAPHHOPPER', defaultValue: false, description: 'Start GraphHopper runtime after graph-cache is ready')
    booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Run rollback instead of deploy')
  }

  environment {
    REPO_URL = 'https://lab.ssafy.com/s14-final/S14P31E102.git'
    REMOTE_DIR = '/home/ubuntu/e102/prod'
    S2_HOST = credentials('e102-s2-host')
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: params.DEPLOY_BRANCH, credentialsId: 'gitlab-pat', url: env.REPO_URL
      }
    }

    stage('Package Workspace') {
      steps {
        script {
          env.APP_IMAGE_TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
          env.GRAPHHOPPER_IMAGE_TAG = env.APP_IMAGE_TAG
        }
        sh '''
          rm -f e102-prod-workspace.tar.gz
          git archive --format=tar.gz --output=e102-prod-workspace.tar.gz HEAD
        '''
      }
    }

    stage('Upload To S2') {
      steps {
        withCredentials([
          file(credentialsId: 'e102-prod-env-file', variable: 'PROD_ENV'),
          sshUserPrivateKey(credentialsId: 'e102-s2-ssh-key', keyFileVariable: 'S2_KEY', usernameVariable: 'S2_USER')
        ]) {
          sh '''
            ssh -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$S2_USER@$S2_HOST" "mkdir -p '$REMOTE_DIR'"
            scp -i "$S2_KEY" -o StrictHostKeyChecking=accept-new e102-prod-workspace.tar.gz "$S2_USER@$S2_HOST:$REMOTE_DIR/"
            scp -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$PROD_ENV" "$S2_USER@$S2_HOST:$REMOTE_DIR/.env.prod"
            ssh -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$S2_USER@$S2_HOST" "cd '$REMOTE_DIR' && mkdir -p .deploy-state && find . -mindepth 1 -maxdepth 1 ! -name .deploy-state ! -name .env.prod ! -name e102-prod-workspace.tar.gz -exec rm -rf {} + && tar -xzf e102-prod-workspace.tar.gz"
          '''
        }
      }
    }

    stage('Deploy Or Rollback') {
      steps {
        withCredentials([
          sshUserPrivateKey(credentialsId: 'e102-s2-ssh-key', keyFileVariable: 'S2_KEY', usernameVariable: 'S2_USER')
        ]) {
          sh '''
            if [ "$ROLLBACK" = "true" ]; then
              REMOTE_CMD='DEPLOY_GRAPHHOPPER='"$DEPLOY_GRAPHHOPPER"' bash scripts/deploy/prod-rollback.sh'
            else
              REMOTE_CMD='APP_IMAGE_TAG='"$APP_IMAGE_TAG"' GRAPHHOPPER_IMAGE_TAG='"$GRAPHHOPPER_IMAGE_TAG"' BUILD_GRAPHHOPPER='"$BUILD_GRAPHHOPPER"' DEPLOY_GRAPHHOPPER='"$DEPLOY_GRAPHHOPPER"' bash scripts/deploy/prod-deploy.sh'
            fi
            ssh -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$S2_USER@$S2_HOST" "cd '$REMOTE_DIR' && $REMOTE_CMD"
          '''
        }
      }
    }
  }

  post {
    always {
      withCredentials([
        sshUserPrivateKey(credentialsId: 'e102-s2-ssh-key', keyFileVariable: 'S2_KEY', usernameVariable: 'S2_USER')
      ]) {
        sh '''
          ssh -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$S2_USER@$S2_HOST" "cd '$REMOTE_DIR' && docker compose --env-file .env.prod -f docker-compose.prod.yml ps" || true
        '''
      }
    }
  }
}
