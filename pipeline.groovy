pipeline {
    agent any

    environment {
        MYSQL_USERNAME = 'username'
        MYSQL_PASSWORD = 'password'
        MYSQL_ADDRESS  = 'mysql:3306'
        MYSQL_DBNAME   = 'docker_e_kubernetes'
    }

    stages {
        stage('Cleanup') {
            steps {
                sh '''
                    echo "***CONTAINERS EXISTENTES***"
                    docker ps -a
                    docker rm -f mysql
                    docker rm -f web
                '''
            }
        }

        stage('Checkout') {
            steps {
                sh '''
                    git clone https://github.com/nikolasluis/DEVOPS_Cloud2025.git
                    echo "***ARQUIVOS IMPORTADOS***"
                    ls -l
                    ls -l db
                    ls -l web
                '''
            }
        }

        stage('Construção') {
            steps {
                sh '''
                    echo "***CONSTRUINDO DOCKER MYSQL***"
                    docker build -t mysql-img -f db/Dockerfile.mysql db/

                    echo "***CONSTRUINDO DOCKER WEB***"
                    docker build -t web-img -f web/Dockerfile.web web/
                '''
            }
        }

        stage('Entrega') {
            steps {
                sh '''
                    docker network create dockerNetwork || true

                    echo "***RODANDO SQL DOCKER***"
                    docker run -d --name mysql \
                        --network dockerNetwork \
                        -e MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_DATABASE=$MYSQL_DBNAME \
                        -v $WORKSPACE/codigo.sql:/docker-entrypoint-initdb.d/init.sql \
                        mysql-img

                    sleep 20

                 echo "***RODANDO WEB DOCKER***"
                    docker run -d --name web \
                        --network dockerNetwork \
                        -p 8200:8200 \
                        -e MYSQL_USERNAME=$MYSQL_USERNAME \
                        -e MYSQL_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_ADDRESS=$MYSQL_ADDRESS \
                        -e MYSQL_DBNAME=$MYSQL_DBNAME \
                        web-img

                     echo "***CONTAINERS SENDO EXECUTADOS***"
                    docker ps
                '''
            }
        }
    }
}
