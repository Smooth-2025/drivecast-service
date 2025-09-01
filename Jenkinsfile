pipeline {
    agent any

    environment {
        AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com"
        ECR_REPOSITORY = "smooth/drivecast-service"
        IMAGE_TAG = "${new Date().format('yyyyMMdd')}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
        AWS_DEFAULT_REGION = "ap-northeast-2"
        K8S_NAMESPACE = "drivecast"
        SECRET_NAME = "drivecast-service-secret"
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: "30", artifactNumToKeepStr: "30"))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build with application.yaml') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building application with application.yaml]"
                    withCredentials([file(credentialsId: 'drivecast-application-yml-file', variable: 'APPLICATION_YAML_FILE')]) {
                        sh "cp ${APPLICATION_YAML_FILE} src/main/resources/application.yaml"
                        sh './gradlew clean build'
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building image: ${env.ECR_REPOSITORY}:${env.IMAGE_TAG}]"
                    def dockerImage = docker.build("${env.ECR_REPOSITORY}:${env.IMAGE_TAG}")
                }
            }
        }

        stage('Push to ECR') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Push Docker Image to ECR]"
                    sh """
                        aws ecr get-login-password --region ${env.AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${env.ECR_REGISTRY}
                        docker tag ${env.ECR_REPOSITORY}:${env.IMAGE_TAG} ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}
                        docker tag ${env.ECR_REPOSITORY}:${env.IMAGE_TAG} ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest
                        docker push ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}
                        docker push ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest
                    """
                }
            }
        }

        stage('Create Kubernetes Secret') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Creating Kubernetes Secret for application.yaml]"
                    withCredentials([
                        file(credentialsId: 'drivecast-application-yml-file', variable: 'SECRET_FILE_PATH'),
                        file(credentialsId: 'kubernetes-kubeconfig', variable: 'KUBE_CONFIG_PATH')
                    ]) {
                        withEnv(["KUBECONFIG=${KUBE_CONFIG_PATH}"]) {
                            sh """
                                kubectl cluster-info || exit 1
                                kubectl get namespace ${env.K8S_NAMESPACE} || kubectl create namespace ${env.K8S_NAMESPACE}

                                kubectl delete secret ${env.SECRET_NAME} -n ${env.K8S_NAMESPACE} --ignore-not-found=true
                                kubectl create secret generic ${env.SECRET_NAME} \
                                    --from-file=application.yaml=\${SECRET_FILE_PATH} \
                                    -n ${env.K8S_NAMESPACE}

                                kubectl get secret ${env.SECRET_NAME} -n ${env.K8S_NAMESPACE} -o yaml
                                echo "Secret ${env.SECRET_NAME} created successfully in namespace ${env.K8S_NAMESPACE}"
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "----------------------------------------------------------------------------------"
                echo "[Cleaning up local Docker images and workspace.]"
                sh """
                    docker rmi ${env.ECR_REPOSITORY}:${env.IMAGE_TAG} || true
                    docker rmi ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG} || true
                    docker rmi ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:latest || true
                """
                cleanWs()
                echo "----------------------------------------------------------------------------------"
                echo "[Sending build result notification to Discord...]"
                def discordFooter = (currentBuild.currentResult == 'SUCCESS') ? "빌드 성공 ✅" : "빌드 실패 ❌"
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
                    discordSend description: "Jenkins Job: ${env.JOB_NAME}",
                        footer: discordFooter,
                        link: env.BUILD_URL,
                        title: "Drivecast Service Build Result",
                        webhookURL: DISCORD_WEBHOOK_URL
                }
            }
        }
        success {
            echo "----------------------------------------------------------------------------------"
            echo "[Pipeline succeeded! Image pushed: ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}]"
            echo "[Secret created: ${env.SECRET_NAME} in namespace: ${env.K8S_NAMESPACE}]"
        }
        failure {
            echo "----------------------------------------------------------------------------------"
            echo "[Pipeline failed!]"
        }
    }
}
