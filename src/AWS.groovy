class AWS {
    def script
    String credentials

    AWS(def script, String credentials) {
        this.script = script
        this.credentials = credentials
    }

    def configure(){
        this.script.withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: this.credentials, accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            script {
                this.script.sh "aws configure --profile ${this.credentials} set aws_access_key_id ${this.script.AWS_ACCESS_KEY_ID}"
                this.script.sh "aws configure --profile ${this.credentials} set aws_secret_access_key ${this.script.AWS_SECRET_ACCESS_KEY}"
                this.script.sh "aws configure --profile ${this.credentials} set region us-west-2"
            }
        }
    }



    def nukeAccount(awsAccount) {
        println "Running aws-nuke for profile: ${awsAccount}"
        def accountId = this.script.accountFromProfile(awsAccount)
        this.script.sh """
            wget https://github.com/rebuy-de/aws-nuke/releases/download/v2.10.0/aws-nuke-v2.10.0-linux-amd64.tar.gz -O ./aws-nuke.tar.gz
            tar -xzf aws-nuke.tar.gz
            mv aws-nuke-v2.10.0-linux-amd64 aws-nuke
            ls -la
            sudo chmod +x ./aws-nuke
        """
        this.script.sh """                        
            cat << EOF > nuke-config.yaml
                regions:
                - "global"
                - "us-west-2"
                - "us-west-1"
                - "us-east-2"
                - "us-east-1"
                - "eu-central-1"
                - "eu-west-1"
                - "eu-west-2"
                - "eu-west-3"
                - "eu-north-1"
                
                account-blacklist:
                - 890234264427 # production
                - 986292867370 # root
                
                resource-types:
                  excludes:
                  - IAMUser
                  - IAMGroup
                  - IAMGroupPolicy
                  - IAMGroupPolicyAttachment
                  - IAMInstanceProfile
                  - IAMInstanceProfileRole
                  - IAMLoginProfile
                  - IAMOpenIDConnectProvider
                  - IAMSAMLProvider
                  - IAMServerCertificate
                  - IAMServiceSpecificCredential
                  - IAMUserAccessKey
                  - IAMUserGroupAttachment
                  - IAMUserPolicy
                  - IAMUserPolicyAttachment
                  - SESIdentity
                  - S3Object
                
                accounts:
                   $accountId:
                     filters:
                       IAMRole:
                       - "Acme"
                       - "Acme2"
                       - "Acme3"
                       - "onelogin_admin"
                       - "bridgecrew_onelogin"
                       - type: glob
                         value: "*portal*"
                       IAMPolicy:
                       - type: glob
                         value: "arn:aws:iam::*:policy/onelogin_list_roles"
                       IAMRolePolicy:
                       - type: glob
                         value: "*portal*"
                       IAMRolePolicyAttachment:
                       - "Acme -> Billing"
                       - "Acme -> AdministratorAccess"
                       - "Acme2 -> Billing"
                       - "Acme2 -> AdministratorAccess"
                       - "Acme3 -> Billing"
                       - "Acme3 -> AdministratorAccess"
                       - "onelogin_admin -> Billing"
                       - "onelogin_admin -> AdministratorAccess"
                       - "bridgecrew_onelogin -> onelogin_list_roles"
                       SSMParameter:
                       - "/base_stack/ses_support_arn"
          """
        this.script.withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: this.credentials, accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            script {
                this.script.sh "aws configure --profile ${awsAccount} set aws_access_key_id ${this.script.AWS_ACCESS_KEY_ID}"
                this.script.sh "aws configure --profile ${awsAccount} set aws_secret_access_key ${this.script.AWS_SECRET_ACCESS_KEY}"
                this.script.sh "aws configure --profile ${awsAccount} set region us-west-2"
                this.script.sh "ls -la"
                this.script.sh "cat nuke-config.yaml"
                this.script.sh "./aws-nuke -c nuke-config.yaml --profile ${awsAccount} --no-dry-run --force"
            }
        }
    }
}