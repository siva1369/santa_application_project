pipeline {
    agent any
    
    tools{
        jdk 'jdk17'
        maven 'maven3'
    }
    environment{
        SCANNER_HOME= tool 'sonar-scanner'
    }

    stages {
        stage('git checkout') {
            steps {
                git changelog: false, poll: false, url: 'https://github.com/siva1369/secretsanta-generator.git'
            }
        }

        stage('Code Compile') {
            steps {
               sh "mvn clean compile"
            }
        }
        
        stage('Unit Tests') {
            steps {
               sh "mvn test"
            }
        }
        stage('Sonar Analysis') {
            steps {
               withSonarQubeEnv('sonar-scanner'){
                   sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.url=http://3.110.32.211:9000/ -Dsonar.login=squ_ad9d8b825808fda6c0565b4eb1453ced1d3ad003 -Dsonar.projectName=secret-santa  \
                   -Dsonar.java.binaries=. \
                   -Dsonar.projectKey=secret-santa '''
               }
            }
        }
        	stage('OWASP Scan') {
            steps {
               dependencyCheck additionalArguments: ' --scan ./ ', odcInstallation: 'DP'
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('build') {
            steps {
               sh "mvn clean install"
            }
        }
        stage('docker build & push') {
            steps {
               script{
                   withDockerRegistry(credentialsId: 'be114797-4c49-4072-bea1-8250ce6cab24', toolName: 'docker') {
                       sh "docker build -t santa ."
                       sh "docker tag santa siva1369/santa:latest"
                       sh "docker push siva1369/santa:latest"
                     }
               }
            }
        }
         stage('docker deploy to container') {
            steps {
               script{
                   withDockerRegistry(credentialsId: 'be114797-4c49-4072-bea1-8250ce6cab24', toolName: 'docker') {
                       sh "docker run --name santa -d -p 9090:8080 siva1369/santa:latest "
                       
                     }
               }
            }
        }
    }
}    