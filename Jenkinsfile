pipeline {
    agent any

    environment {
        SERVICE_NAME = "Drivecast Service"
        AWS_DEFAULT_REGION = "ap-northeast-2"
        ECR_REPOSITORY = "smooth/drivecast-service"
        GITOPS_REPO = "https://github.com/Smooth-2025/smooth-gitops.git"
        GITOPS_BRANCH = "main"
        K8S_DEPLOYMENT_FILE = "manifests/drivecast/base/deployment.yaml"
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

        stage('Set Dynamic Env') {
            steps {
                script {
                    env.AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
                    env.ECR_REGISTRY = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_DEFAULT_REGION}.amazonaws.com"
                    env.IMAGE_TAG = new Date().format('yyyyMMdd-HHmmss')
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building application]"
                    sh './gradlew clean build -Dspring.profiles.active=test'
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building image: ${env.ECR_REPOSITORY}:${env.IMAGE_TAG}]"
                    docker.build("${env.ECR_REPOSITORY}:${env.IMAGE_TAG}")
                }
            }
        }

        stage('ECR Push') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Push Docker Image to ECR]"
                    sh """
                        aws ecr get-login-password --region ${env.AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${env.ECR_REGISTRY}
                        docker tag ${env.ECR_REPOSITORY}:${env.IMAGE_TAG} ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}
                        docker push ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deployment Manifest Update') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Updating GitOps repository with new image tag: ${env.IMAGE_TAG}]"
                    withCredentials([usernamePassword(credentialsId: 'github-access-token', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                        sh """
                            git config user.email "mjalswn26@gmail.com"
                            git config user.name "minju26"

                            git clone --branch ${env.GITOPS_BRANCH} https://${GITHUB_TOKEN}@github.com/Smooth-2025/smooth-gitops.git gitops-repo
                            cd gitops-repo

                            sed -i "s|image: .*|image: ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}|g" ${env.K8S_DEPLOYMENT_FILE}

                            git add ${env.K8S_DEPLOYMENT_FILE}
                            git commit -m "feat: 도커 이미지 태그 업데이트(${env.ECR_REPOSITORY}:${env.IMAGE_TAG})"
                            git push origin ${env.GITOPS_BRANCH}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "----------------------------------------------------------------------------------"
                sh """
                    docker rmi ${env.ECR_REPOSITORY}:${env.IMAGE_TAG} || true
                    docker rmi ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG} || true
                """
                cleanWs()

                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'
                def statusIcon = buildStatus == 'SUCCESS' ? '✅' : '❌'
                def statusColor = buildStatus == 'SUCCESS' ? '#00FF00' : '#FF0000'
                def buildDuration = currentBuild.durationString.replace(' and counting', '')

                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
                    discordSend(
                        title: "${env.SERVICE_NAME} Jenkins Build ${buildStatus} ${statusIcon}",
                        description: """
                        **프로젝트**: ${env.JOB_NAME}
                        **브랜치**: ${env.GIT_BRANCH ?: 'main'}
                        **이미지**: ${env.ECR_REPOSITORY}:${env.IMAGE_TAG}
                        **빌드 시간**: ${buildDuration}
                        """.stripIndent(),
                        footer: "Jenkins CI/CD Pipeline",
                        link: env.BUILD_URL,
                        webhookURL: DISCORD_WEBHOOK_URL,
                        color: statusColor
                    )
                }
            }
        }
        success {
            echo "Pipeline succeeded! Image: ${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
