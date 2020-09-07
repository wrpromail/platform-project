pipeline {
    agent any
    stages {

        stage('检出') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: env.GIT_BUILD_REF]], userRemoteConfigs: [[url: env.GIT_REPO_URL, credentialsId: env.CREDENTIALS_ID]]])
            }
        }

        stage("build:platform:starter") {
            steps {
                sh "./build.sh"
            }
        }

        stage("push:k8s-resources") {
            steps {
                // 需要定义环境变量
                // AUTO_DEPLOY_USER  必选项  项目令牌 username  必选项  git 拉取代码的时候使用
                // AUTO_DEPLOY_PASS  必选项  项目令牌 password  必选项  git 拉取代码的时候使用
                // VERSION           必选项  把内容打包到制品库时，需要指定日期
                // SERVICE_NAME      可选项  用于提交到 auto-deploy 项目的文件夹名称，脚本可以自动从 Deployment | StatefulSet 读取
                sh "./push-k8s-resources.sh"
            }
        }

    }
}
