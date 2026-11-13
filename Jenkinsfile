// Pipeline CI/CD: Build & Test -> Jacoco 100% -> Trivy -> Package -> Publish -> Deploy.
// Prasyarat agent: JDK 21, Trivy, dan (untuk Deploy) ssh. Parameter/credential dikonfigurasi di Jenkins.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'ifs25045-p5-pbo2026-springboot-cashflow'
        MVN      = './mvnw -B -ntp'
    }

    parameters {
        string(name: 'DEPLOY_HOST', defaultValue: '', description: 'Host tujuan deploy (kosong = lewati Deploy)')
        string(name: 'DEPLOY_USER', defaultValue: 'deploy', description: 'User SSH')
        string(name: 'DEPLOY_DIR',  defaultValue: '/opt/cashflow', description: 'Folder aplikasi di server')
        string(name: 'DEPLOY_CRED', defaultValue: 'cashflow-deploy-ssh', description: 'ID credential SSH di Jenkins')
    }

    stages {
        stage('Build & Test') {
            steps {
                sh 'chmod +x mvnw'
                // Profil "test" memakai H2 sehingga tidak butuh PostgreSQL di CI.
                sh '${MVN} clean test'
            }
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }

        stage('Jacoco 100%') {
            steps {
                // verify menjalankan jacoco:check (LINE 100%; framework/view + framework/auth dikecualikan di pom).
                sh '${MVN} verify'
            }
            post {
                always { archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true }
            }
        }

        stage('Trivy Scan') {
            steps {
                // Gagal bila ada kerentanan HIGH/CRITICAL pada dependensi atau konfigurasi.
                sh 'trivy fs --scanners vuln,secret,misconfig --severity HIGH,CRITICAL --exit-code 1 --skip-dirs target --skip-dirs node_modules .'
            }
        }

        stage('Package') {
            steps {
                // Profil production membangun bundle frontend Vaadin.
                sh '${MVN} -Pproduction -DskipTests package'
            }
        }

        stage('Publish') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { return params.DEPLOY_HOST?.trim() }
                }
            }
            steps {
                sshagent(credentials: [params.DEPLOY_CRED]) {
                    sh '''
                        JAR=$(ls target/${APP_NAME}-*.jar | grep -v original | head -n 1)
                        scp -o StrictHostKeyChecking=accept-new "$JAR" $DEPLOY_USER@${DEPLOY_HOST}:${DEPLOY_DIR}/app.jar
                        ssh -o StrictHostKeyChecking=accept-new $DEPLOY_USER@${DEPLOY_HOST} "sudo systemctl restart cashflow"
                    '''
                }
            }
        }
    }

    post {
        failure { echo 'Pipeline gagal: periksa tahap yang ditandai merah.' }
    }
}
