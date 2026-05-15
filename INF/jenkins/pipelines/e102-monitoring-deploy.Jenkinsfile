pipeline {
  agent any

  options {
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'DEPLOY_BRANCH', defaultValue: 'master', description: 'Git branch that contains monitoring/proxy changes')
  }

  environment {
    REPO_URL = 'https://lab.ssafy.com/s14-final/S14P31E102.git'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: params.DEPLOY_BRANCH, credentialsId: 'gitlab-pat', url: env.REPO_URL
      }
    }

    stage('Sync Monitoring To S1') {
      steps {
        sh 'bash scripts/deploy/s1-monitoring-sync.sh'
      }
    }
  }

  post {
    always {
      script {
        deleteDir()
      }
    }
  }
}
