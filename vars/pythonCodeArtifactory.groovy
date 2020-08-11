def call() {
    println "logging into AWS CodeArtifact"
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'code-artifact', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        sh "aws configure --profile artifact set aws_access_key_id ${AWS_ACCESS_KEY_ID}"
                        sh "aws configure --profile artifact set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}"
                        sh """
                            aws codeartifact login --tool pip --repository relations-graph --domain bridgecrew --domain-owner 890234264427 --profile artifact --region us-west-2
                            export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain bridgecrew --domain-owner 890234264427 --query authorizationToken --output text --profile artifact --region us-west-2`
                            pipenv install --pypi-mirror https://aws:$CODEARTIFACT_AUTH_TOKEN@bridgecrew-890234264427.d.codeartifact.us-west-2.amazonaws.com/pypi/relations-graph/simple/
                        """
                    }
    }
}
