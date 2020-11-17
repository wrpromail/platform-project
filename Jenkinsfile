pipeline {
  agent any
  stages {
    stage('检出') {
      steps {
        sh 'ci-init'
        checkout([$class: 'GitSCM', branches: [[name: env.GIT_BUILD_REF]], userRemoteConfigs: [[url: env.GIT_REPO_URL, credentialsId: env.CREDENTIALS_ID]]])
      }
    }

    stage('build:all') {
      steps {
         withCredentials([usernamePassword(credentialsId: '264aecc0-8aa8-44d9-ae06-123fce42a493',
                usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
              sh './build.sh'
         }
      }
    }
  }
}