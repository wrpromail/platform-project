pipeline {
  agent any
  stages {
    stage("检出") {
      steps {
        checkout([$class: "GitSCM", branches: [[name: env.GIT_BUILD_REF]], userRemoteConfigs: [[url: env.GIT_REPO_URL, credentialsId: env.CREDENTIALS_ID]]])
      }
    }
    stage("build:platform:project") {
      steps {
        sh "./build.sh"
      }
    }
    stage("push:k8s-resources") {
      steps {
        sh "./push-k8s-resources.sh"
      }
    }
  }
}