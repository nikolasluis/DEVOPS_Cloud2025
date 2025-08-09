pipeline {
    agent any

    environment {
        MYSQL_USERNAME = 'root'
        MYSQL_PASSWORD = '123456'
        MYSQL_ADDRESS  = 'mysql:3306'
        MYSQL_DBNAME   = 'docker_e_kubernetes'
    }

    stages {
        stage('Cleanup') {
            steps {
                sh '''
                    echo "Listando containers existentes antes da limpeza:"
                    docker ps -a
                    docker rm -f mysql || true
                    docker rm -f web || true
                    docker system prune -af || true
                '''
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/nikolasluis/DEVOPS_Cloud2025.git'
                sh '''
                    echo "Conteúdo do workspace após checkout:"
                    ls -l
                    ls -l db
                    ls -l web
                '''
            }
        }

        stage('Construção') {
            steps {
                sh '''
                    echo "Conteúdo da pasta db para o build do MySQL:"
                    ls -l db

                    echo "Construindo imagem mysql-img..."
                    docker build -t mysql-img -f db/Dockerfile.mysql db/

                    echo "Conteúdo da pasta web para o build do web:"
                    ls -l web

                    echo "Construindo imagem web-img..."
                    docker build -t web-img -f web/Dockerfile.web web/
                '''
            }
        }

        stage('Entrega') {
            steps {
                sh '''
                    docker network create mynetwork || true

                    echo "Rodando container mysql..."
                    docker run -d --name mysql \
                        --network mynetwork \
                        -e MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_DATABASE=$MYSQL_DBNAME \
                        -v $WORKSPACE/codigo.sql:/docker-entrypoint-initdb.d/init.sql \
                        mysql-img

                    sleep 15

                    echo "Rodando container web..."
                    docker run -d --name web \
                        --network mynetwork \
                        -p 8200:8200 \
                        -e MYSQL_USERNAME=$MYSQL_USERNAME \
                        -e MYSQL_PASSWORD=$MYSQL_PASSWORD \
                        -e MYSQL_ADDRESS=$MYSQL_ADDRESS \
                        -e MYSQL_DBNAME=$MYSQL_DBNAME \
                        web-img

                    echo "Containers em execução:"
                    docker ps
                '''
            }
        }
    }
}
