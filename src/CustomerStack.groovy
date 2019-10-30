import groovy.json.JsonOutput;

class CustomerStack {
    def region;
    def aws_profile;
    def accountID;
    def bucket;
    def statePath;
    def script;
    def stateDir;
    def customerName;
    def baseStack;
    def customerImageName;
    def customerImageURL;
    def customerDetails = [:]
    def baseAccountID;

    CustomerStack(def script, String bucket, String statePath, String customerName, def baseStack) {
        this.region = baseStack.region
        this.aws_profile = baseStack.aws_profile
        this.baseAccountID = baseStack.accountID
        this.bucket = bucket
        this.statePath = statePath
        this.script = script
        this.customerName = customerName
        this.baseStack = baseStack
        this.stateDir = statePath + "/" + customerName
        this.customerImageName  = "${this.baseStack.customerBaseImageName}-${this.customerName}";
        this.customerImageURL = baseStack.accountID + ".dkr.ecr." + this.region + ".amazonaws.com/" + this.customerImageName;

    }

    def signup(String aws_profile_customer, String email, String firstName, String lastName, String phone, def reuseCT = true) {
        def output = this.baseStack.readOutput();
        def customerAccountID = this.script.accountFromProfile(this.aws_profile)
        this.customerDetails["accountId"] = customerAccountID;
        this.customerDetails["customer_name"] = this.customerName;
        this.customerDetails["email"] = email;
        this.customerDetails["firstName"] = firstName;
        this.customerDetails["lastName"] = lastName;
        this.customerDetails["phone"] = phone;


        def contentCustomerData = """
            {
                "customer_name": "${this.customerName}",
                "owner_email": "${email}",
                "owner_first_name": "${firstName}",
                "owner_last_name": "${lastName}",
                "owner_phone": "${phone}"
            }
        """

        this.script.echo(contentCustomerData);
        def customerJson = this.script.readJSON text: contentCustomerData.trim()
        this.script.echo(output.api_base_url.value);

        def cloudFormationUrl = this.script.sh(
                script: """
            curl -X POST \
                        ${output.api_base_url.value}/api/v1/signup \
                        -H 'Content-Type: application/json' \
                        -d '${customerJson.toString()}'
        """,
                returnStdout: true)

        cloudFormationUrl = this.script.sh(
                script: """
                    python -c "import sys, urllib.parse; print(urllib.parse.unquote('${cloudFormationUrl}'))"
                """,
                returnStdout: true)

        this.script.echo(cloudFormationUrl)

        def url = this.script.sh(
                script: """
                    echo "${cloudFormationUrl}" | awk -F'[=&]' '{print \$2}'
                 """,
                returnStdout: true).trim()

        def paramExternalID = this.script.sh(
                script: """
                    echo "${cloudFormationUrl}" | awk -F'[=&]' '{print \$4}'
                 """,
                returnStdout: true).trim()

        def stackName = this.script.sh(
                script: """
                    echo "${cloudFormationUrl}" | awk -F'[=&]' '{print \$6}'
                 """,
                returnStdout: true).trim()

        def paramResourceNamePrefix = this.script.sh(
                script: """
                    echo "${cloudFormationUrl}" | awk -F'[=&]' '{print \$8}'
                 """,
                returnStdout: true).trim()

        def paramCustomerName = this.script.sh(
                script: """
                    echo "${cloudFormationUrl}" | awk -F'[=&]' '{print \$10}'
                 """,
                returnStdout: true).trim()


        this.script.echo(url)
        this.script.echo(paramExternalID)
        this.script.echo(stackName)
        this.script.echo(paramResourceNamePrefix)
        this.script.echo(paramCustomerName)

        def stackID = stackName;

        def describeStackCode = this.script.sh(script: "aws cloudformation describe-stacks --region ${this.region} --profile ${aws_profile_customer} --stack-name ${stackName} 2>/dev/null", returnStatus: true)

        this.script.echo(describeStackCode.toString())

        if (describeStackCode != 0) {
        def cloudtrailParams = ""
            if (reuseCT) {
                def cloudtrailStackName = "bc-cloudtrail-bridgecrew";

                this.script.sh "aws cloudformation deploy --region ${this.region} --profile ${aws_profile_customer} --stack-name ${cloudtrailStackName} --template-file resources/cf_cloudtrail_demo.json --no-fail-on-empty-changeset"

                def outputsCloudtrail = this.waitForCloudFormation(aws_profile_customer, cloudtrailStackName);
                def outputFormat = [:]
                outputsCloudtrail.each {
                    outputFormat[it.OutputKey] = it.OutputValue
                }
                this.script.sh "echo ${outputFormat}"
                cloudtrailParams = "ParameterKey=CreateTrail,ParameterValue=No ParameterKey=ExistingTrailBucketName,ParameterValue=${outputFormat.BucketArn} ParameterKey=ExistingTrailTopicArn,ParameterValue=${outputFormat.TopicARN}"
            }
            def createStackRes = this.script.sh(
                    script: """
                    aws cloudformation create-stack --region ${this.region} --profile ${
                        aws_profile_customer
                    } --stack-name ${stackName}  \
                                    --template-url ${url} \
                                    --capabilities CAPABILITY_NAMED_IAM \
                                    --parameters ParameterKey=ResourceNamePrefix,ParameterValue=${paramResourceNamePrefix} ParameterKey=ExternalID,ParameterValue=${paramExternalID} ParameterKey=CustomerName,ParameterValue=${paramCustomerName} ${cloudtrailParams} 
                """,
                    returnStdout: true
            ).trim();

            if (createStackRes) {
                stackID = this.script.readJSON(text: createStackRes).StackId;
            } else {
                throw new Exception("Response is empty")
            }
        }

        this.script.echo(stackID)

        def outputs = this.waitForCloudFormation(aws_profile_customer, stackID);

        this.customerDetails.customer_account_info = [:]

        outputs.each {
            this.customerDetails.customer_account_info[it.OutputKey] = it.OutputValue
        }

        def customerDetailsJson = JsonOutput.toJson(this.customerDetails)
        customerDetailsJson = JsonOutput.prettyPrint(customerDetailsJson)

        this.script.echo customerDetailsJson;

        return this.customerDetails;
    }

    def waitForCloudFormation(String aws_profile_customer,String stackID) {
        def stackStatus = "";
        def aws_res;

        while (stackStatus != "CREATE_COMPLETE" && stackStatus != "UPDATE_COMPLETE") {
            aws_res = this.script.sh(
                    script: "aws cloudformation describe-stacks --region ${this.region} --profile ${aws_profile_customer} --stack-name ${stackID} || true",
                    returnStdout: true).trim();
            if (aws_res == "") {
                aws_res = "{}"
            } else {
                stackStatus = this.script.readJSON(text: aws_res).Stacks[0].StackStatus;
                this.script.echo stackStatus;
                if(stackStatus == "ROLLBACK_COMPLETE") {
                    throw new Exception("Deploy CloudFormation Failed " + stackID)
                }
                this.script.sleep(time: 10, unit: "SECONDS");
            }
        }

        return this.script.readJSON(text: aws_res).Stacks[0].Outputs;
    }

    def signupDestroy(String aws_profile_customer) {
        def stackName = this.customerName + "-bridgecrew"
        this.script.sh "aws cloudformation delete-stack  --region ${this.region} --profile ${aws_profile_customer} --stack-name ${stackName}"

        def aws_res = "";

        while (!aws_res.contains("does not exist")) {
            aws_res = this.script.sh(
                    script: "aws cloudformation --region ${this.region} --profile ${aws_profile_customer} describe-stacks --stack-name ${stackName} 2>&1 || true",
                    returnStdout: true).trim();
        }
    }

    def signupRemote(String account_id, String email, String firstName, String lastName, String phone, def customer_account_info) {
        this.customerDetails["accountId"] = account_id;
        this.customerDetails["customer_name"] = this.customerName;
        this.customerDetails["email"] = email;
        this.customerDetails["firstName"] = firstName;
        this.customerDetails["lastName"] = lastName;
        this.customerDetails["phone"] = phone;
        this.customerDetails["customer_account_info"] = customer_account_info;

        def customerDetailsJson = JsonOutput.toJson(this.customerDetails)
        customerDetailsJson = JsonOutput.prettyPrint(customerDetailsJson)

        this.script.echo customerDetailsJson;
    }

    def build() {
        def tenant = this.script.sh(
                script: "aws dynamodb get-item --profile ${this.aws_profile} --region ${this.region} --table-name tenants${this.baseStack.uniqueTag} --key '{\"customer_name\": {\"S\": \"${this.customerName}\"}}'",
                returnStdout: true
        )

        def tenantJson = this.script.readJSON text: tenant;
        tenantJson = tenantJson.Item;

        def tenantEnvDetails = this.script.sh(
                script: "aws dynamodb scan --profile ${this.aws_profile} --region ${this.region} --table-name tenants_env_details${this.baseStack.uniqueTag} --filter-expression 'customer_name = :customer' --expression-attribute-values '{\":customer\" :{\"S\":\"${this.customerName}\"}}'",
                returnStdout: true
        )

        def tenantEnvDetailsJson = this.script.readJSON text: tenantEnvDetails;
        tenantEnvDetailsJson = tenantEnvDetailsJson.Items[0];


        def customerDetails = [:];
        customerDetails.accountId = tenantEnvDetailsJson.aws_account_id["S"]
        customerDetails.customer_name = this.customerName
        customerDetails.email = tenantJson.owner_email["S"];
        customerDetails.firstName = tenantJson.owner_first_name["S"];
        customerDetails.lastName = tenantJson.owner_last_name["S"];
        customerDetails.phone = tenantJson.owner_phone["S"];
        customerDetails.customer_account_info = [:];

        def sqsQueueUrl = tenantEnvDetailsJson.sqs_queue_url["S"]
        def sqsQueueUrlSplit = sqsQueueUrl.split('/')
        def sqsQueueRegion = sqsQueueUrlSplit[2].split('\\.')[1]
        def sqsQueueAccount = sqsQueueUrlSplit[3]
        def sqsQueueName = sqsQueueUrlSplit[4]

        customerDetails.customer_account_info.SQSQueueARN = "arn:aws:sqs:${sqsQueueRegion}:${sqsQueueAccount}:${sqsQueueName}"
        customerDetails.customer_account_info.STSRoleARN = tenantEnvDetailsJson.role_arn["S"]
        customerDetails.customer_account_info.RoleARN = tenantEnvDetailsJson.cross_account_role_arn["S"]

        this.script.echo customerDetails.toString();

        this.build(customerDetails)
    }

    def build(def customerDetails) {
        this.script.sh "aws ecr get-login --profile ${this.aws_profile} --region ${this.region} --no-include-email | sh";
        this.script.echo this.customerImageName;

        def baseStackOutput = this.baseStack.readOutput();

        this.script.echo baseStackOutput.toString();

        def accessKeyID = this.script.sh(
                script: "aws configure --profile ${aws_profile} get aws_access_key_id",
                returnStdout: true
        ).trim()

        def accessSecretKey = this.script.sh(
                script: "aws configure --profile ${aws_profile} get aws_secret_access_key",
                returnStdout: true
        ).trim()


        this.script.dir('jenkins/src/customerDockerFiles/run') {
            this.script.sh """
                docker build -t ${this.customerImageName} \
                --build-arg base_image=${this.baseStack.customerBaseImageURL} \
                --build-arg region=${this.region} \
                --build-arg profile=${this.baseStack.aws_profile} \
                --build-arg access_key_id=${accessKeyID} \
                --build-arg access_secret_key=${accessSecretKey} \\
                --build-arg bucket=${this.baseStack.bucket} \
                --build-arg key=${this.stateDir} \
                --build-arg customer_name=${this.customerName} \
                --build-arg client_sqs_arn=${(customerDetails.customer_account_info != null && customerDetails.customer_account_info.SQSQueueARN != null) ? customerDetails.customer_account_info.SQSQueueARN : ""} \
                --build-arg customer_assume_role_arn=${(customerDetails.customer_account_info != null && customerDetails.customer_account_info.STSRoleARN != null) ? customerDetails.customer_account_info.STSRoleARN : ""} \
                --build-arg user_pool_id=${baseStackOutput.user_pool.value} \
                --build-arg owner_first_name=${(customerDetails.firstName != null) ? customerDetails.firstName : ""} \
                --build-arg owner_family_name=${(customerDetails.lastName != null) ? customerDetails.lastName : ""} \
                --build-arg owner_phone=${(customerDetails.phone != null) ? customerDetails.phone : ""} \
                --build-arg owner_email=${(customerDetails.email != null) ? customerDetails.email : ""} \
                --build-arg customer_aws_account_id=${(customerDetails.accountId != null) ? customerDetails.accountId : ""} \
                --build-arg customer_assume_cross_account_role_arn=${(customerDetails.customer_account_info != null && customerDetails.customer_account_info.RoleARN != null) ? customerDetails.customer_account_info.RoleARN : ""} \
                --build-arg base_stack_unique_tag=${this.baseStack.uniqueTag} \
                .
            """

            def repoExist = this.script.sh(
                    script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region} --repository-names ${this.customerImageName}",
                    returnStatus: true
            )

            if (repoExist != 0) {
                this.script.sh "aws ecr create-repository --profile ${this.aws_profile} --region ${this.region} --repository-name ${this.customerImageName}"
            }

            this.script.sh "docker tag ${this.customerImageName} ${this.customerImageURL}"
            this.script.sh "docker push ${this.customerImageURL}";
        }
    }

    def plan() {
        this.runTfCommand("plan");
    }

    def apply() {
        this.runTfCommand("apply");
    }

    def upgrade() {
        this.runTfCommand("upgrade");
    }

    def destroy() {
        def repoExist = this.script.sh(
                script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region} --repository-names ${this.customerImageName}",
                returnStatus: true
        )

        if (repoExist == 0) {
            this.runTfCommand("destroy");

            this.script.sh "aws s3 rm --profile ${this.aws_profile} s3://${this.bucket}/${this.stateDir} --recursive"

            this.script.sh "aws ecr delete-repository --profile ${this.aws_profile} --region ${this.region} --repository-name ${this.customerImageName} --force"
        }
    }

    def configureLacework(def laceworkSecret,def laceworkKey, def customerAccountID) {
        def laceworkIntegration = """
            {
                "customer_name": "${this.customerName}",
                "name": "lacework",
                "enable": true,
                "params": {
                    "aws_account_id": "${customerAccountID}",
                    "lacework_account_id": "VIRTUE2",
                    "lacework_secret_access_key": "${laceworkSecret}",
                    "lacework_access_key_id": "${laceworkKey}",
                    "schedule_rate": 5
                }
            }
        """

        def laceworkIntegrationJson = this.script.readJSON text: laceworkIntegration.trim()

        this.script.sh """
            aws lambda invoke --profile ${this.aws_profile} --region ${this.region} --function-name integrations-handler${this.baseStack.uniqueTag} \\
                --payload '{"path":"/api/v1/integrations","httpMethod":"POST","body":"${laceworkIntegrationJson.toString().replace("\"","\\\"")}","headers":{"content-type":"application/json"},"requestContext":{}}' outfile
        """
    }

    def runTfCommand(String command) {
        def repoExist = this.script.sh(
                script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region} --repository-names ${this.customerImageName}",
                returnStatus: true
        )

        if (repoExist != 0) {
            throw new Exception(this.customerImageName + " image does not exist")
        }

        this.script.dir('run') {
            this.script.sh "aws ecr get-login --profile ${this.aws_profile} --region ${this.region} --no-include-email | sh";
            this.script.sh "docker run ${this.customerImageURL} ${command}"
        }
    }
}