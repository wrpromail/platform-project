pipeline {
  agent any
  stages {
    stage('检出') {
      steps {
        sh 'ci-init'
        checkout([$class: 'GitSCM', branches: [[name: env.GIT_BUILD_REF]], userRemoteConfigs: [[url: env.GIT_REPO_URL, credentialsId: env.CREDENTIALS_ID]]])
      }
    }

    stage('构建') {
      steps {
        script {
             try {
                 echo "GIT_TAG: $GIT_TAG"
                 withCredentials([usernamePassword(credentialsId: env.BUILD_CREDENTIALS_ID, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                     echo "Found GIT_TAG value, will replace VERSION_TAG use GIT_TAG build"
                     sh "VERSION_TAG=$GIT_TAG ./build.sh"
                 }
             }catch (Exception e){
                 echo "No GIT_TAG, use normal build"
                 withCredentials([usernamePassword(credentialsId: env.BUILD_CREDENTIALS_ID,usernameVariable: 'REGISTRY_USER',passwordVariable: 'REGISTRY_PASSWORD')]) {
                     sh "./build.sh"
                 }
             }
         }
      }
    }
    stage('版本清单') {
      steps {
        script {
           try {
                echo "GIT_TAG: $GIT_TAG"
                useCustomStepPlugin(key: 'version_message', version: 'latest', params: [target:"master",source:"release",depot_name:"platform-project",tag:"$GIT_TAG"])
            }catch (Exception e){
                echo "No GIT_TAG, skip git version "
            }
        }
     }
    }
  }
}