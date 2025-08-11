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
                    echo "Listando os containers existentes: "
                    docker ps -a
                    docker rm -f mysql
                    docker rm -f web
                    rm -rf DEVOPS_Cloud2025
                    docker network rm dockerNetwork
                    docker volume prune -f
                '''
            }
        }

        stage('Checkout') {
            steps {
                sh '''
                    git clone https://github.com/nikolasluis/DEVOPS_Cloud2025.git
                    echo "Listando todos os arquivos importados"
                    ls -l DEVOPS_Cloud2025
                    ls -l DEVOPS_Cloud2025/web
                    ls -l DEVOPS_Cloud2025/db
                '''
            }
        }

        stage('Construção') {
            steps {
                sh '''
                    echo "Construindo docker MySQL"
                    docker build -t mysql-img -f DEVOPS_Cloud2025/db/Dockerfile.mysql DEVOPS_Cloud2025/db/

                    echo "Construindo docker Python Web"
                    docker build -t web-img -f DEVOPS_Cloud2025/web/Dockerfile.web DEVOPS_Cloud2025/web/
                '''
            }
        }

        stage('Entrega') {
            steps {
                sh '''
                    docker network create dockerNetwork || true

                    echo "Inicia docker MySQL"
                    docker run -d --name mysql \
                        --network dockerNetwork \
                        -e MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_DATABASE=$MYSQL_DBNAME \
                        -v $WORKSPACE/DEVOPS_Cloud2025/db/codigo.sql:/docker-entrypoint-initdb.d/init.sql \
                        mysql-img

                    echo "Aguardando inicialização docker MySQL"
                    sleep 5

                    echo "Inicia docker Web"
                    docker run -d --name web \
                        --network dockerNetwork \
                        -p 8200:8200 \
                        -e MYSQL_USERNAME=$MYSQL_USERNAME \
                        -e MYSQL_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_ADDRESS=$MYSQL_ADDRESS \
                        -e MYSQL_DBNAME=$MYSQL_DBNAME \
                        web-img

                    echo "Listando dockers executadas*"
                    docker ps -a
                '''
            }
        }
    }
}
