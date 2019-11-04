  
@Library('Utils').vars _

class BaseStack {
    def region;
    def aws_profile;
    def accountID;
    def bucket;
    def statePath;
    def script;
    def branch;
    def uniqueTag;
    def stateDir;
    def paramsFileName = "params.tfvars";
    def customerBaseImageName;
    def customerBaseImageURL;

    BaseStack(def script, String region, String aws_profile, String bucket, String statePath, String uniqueTag, String branch) {
        this.branch = branch
        this.region = region
        this.aws_profile = aws_profile
        this.accountID = aws_profile
        this.bucket = bucket
        this.statePath = statePath
        this.uniqueTag = uniqueTag
        this.script = script
        this.customerBaseImageName = "customer-stack-base-${this.aws_profile}"
        this.accountID = accountFromProfile(this.aws_profile)

        if (uniqueTag) {
            this.customerBaseImageName += "-" + this.uniqueTag;
            this.stateDir = statePath + '/' + uniqueTag;
        } else {
            this.stateDir = statePath
        }

        this.customerBaseImageURL = this.accountID + ".dkr.ecr." + this.region + ".amazonaws.com/" + this.customerBaseImageName;

        this.script.dir('src/stacks/baseStack') {
            this.script.sh """
                 if aws s3api head-bucket --profile ${aws_profile} --region ${region} --bucket ${bucket} 2>/dev/null;
                 then
                    echo 'Bucket exists'
                 else
                    echo 'Creating bucket with versioning'
                    aws s3api create-bucket --profile ${aws_profile} --bucket ${bucket} --region ${
                region
            } --acl private --create-bucket-configuration LocationConstraint=${region}
                    aws s3api put-bucket-versioning --profile ${aws_profile} --bucket ${bucket} --region ${region} --versioning-configuration Status=Enabled
                 fi
           """

            this.script.sh """
               echo 'region = "${region}"' > init.tfbackend
               echo 'profile = "${aws_profile}"' >> init.tfbackend
               echo 'bucket = "${bucket}"' >> init.tfbackend
               echo 'key = "${this.stateDir}/state.tfbackend"' >> init.tfbackend

               cat init.tfbackend
           """
           def state_check = this.script.sh(
                script: """
                   if aws s3api --profile ${aws_profile} head-object --bucket ${bucket} --key ${this.stateDir}/state.tfbackend 2>/dev/null;
                   then
                        echo "state exists"
                   else
                        echo "no state"
                   fi
                   """,
                returnStdout: true
           )
           if (state_check.contains("state exists")) {
               this.migrate()
           }

           this.script.sh "terraform init -reconfigure -backend-config=init.tfbackend"
        }
    }

    def readOutput() {
        this.script.dir('src/stacks/baseStack') {
            def outputResponse = this.script.sh(
                    script: "terraform output -json",
                    returnStdout: true
            )

            def outputResponseJson = this.script.readJSON text: outputResponse;

            return outputResponseJson;
        }
    }

    def migrate() {
        this.script.sh """
            if aws s3api head-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/migrations.json 2>/dev/null;
            then
                aws s3api get-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/migrations.json migrations.json
            else
                echo {} > migrations.json
            fi
        """
        def migrationObj = this.script.readJSON file: "migrations.json"

        // Get version from package.json
        def packageJson = this.script.readJSON file: "../../../package.json"
        def version = packageJson.version
        List versionParts = version.tokenize('.')
        def major = versionParts[0].toInteger()
        def minor = versionParts[1].toInteger()
        def patch = versionParts[2].toInteger()

        if ((!migrationObj.RegionalCFs || migrationObj.RegionalCFs < 1) && major >= 0 && minor >= 9 && patch >= 3) {
            this.script.sh """
                cp ../../../devTools/migration/migrate.sh .
                git checkout tags/v0.9.1
                terraform init -reconfigure -backend-config=init.tfbackend
                chmod +x migrate.sh
                ./migrate.sh ${this.bucket} ${this.stateDir}/${this.paramsFileName} "--target module.create_us_east-1.module.create_cloudformation_stack" ${this.aws_profile}
                git checkout ${this.branch}
            """
            migrationObj.RegionalCFs = 1
        }

        this.script.writeJSON file: "migrations.json", json: migrationObj
        this.script.sh "aws s3api put-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/migrations.json --body ./migrations.json"
    }

    def paramsFile(String slackHookUrl, String zendeskApiKey, boolean turboMode, String googleAnalytics, String circleCIKey, String appNpmToken, String domain = null) {
        this.script.sh """
                           echo 'region = "${this.region}"' > ${this.paramsFileName}
                           echo 'aws_profile = "${this.aws_profile}"' >> ${this.paramsFileName}
                           echo 'state_bucket = "${this.bucket}"' >> ${this.paramsFileName}
                           echo 'slack_hook_url = "${slackHookUrl}"' >> ${this.paramsFileName}
                           echo 'unique_tag = "${this.uniqueTag}"' >> ${this.paramsFileName}
                           echo 'circle_job_branch = "${this.branchName}"' >> ${this.paramsFileName}
                           echo 'zendesk_api_key = "${zendeskApiKey}"' >> ${this.paramsFileName}
                           echo 'google_analytics_key = "${googleAnalytics}"' >> ${this.paramsFileName}
                           echo 'circle_ci_key = "${circleCIKey}"' >> ${this.paramsFileName}
                           echo 'app_npm_token = "${appNpmToken}"' >> ${this.paramsFileName}
                           echo 'run_circleci = "False"' >> ${this.paramsFileName}
                           echo 'support_email = "t${this.uniqueTag ? "+" + this.uniqueTag : ""}@bridgecrew.io"' >> ${this.paramsFileName}
                        """

        if (turboMode == true) {
            this.script.sh "echo 'turbo_mode = true' >> ${this.paramsFileName}"
        } else {
            this.script.sh "echo 'turbo_mode = false' >> ${this.paramsFileName}"

            if (domain == null) {
                throw new Exception("Domain does not exist");
            } else {
                this.script.sh """echo 'domain = "${domain}"' >> ${this.paramsFileName}"""
            }
        }

        this.script.sh "cat ${this.paramsFileName}"
    }

    def BackupToS3() {
        this.script.sh "aws s3api put-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/${this.paramsFileName} --body ${this.paramsFileName}"
        this.script.sh """
             json=\$(jq -n '{
                                 "branch_sha": "${this.script.env.GIT_COMMIT}",
                                 "branch_name": "${this.branch}",
                             }')

             echo \$json | jq > info.json
         """
        this.script.sh "aws s3api put-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/info.json --body info.json"
    }

    def RestoreFromS3() {
        this.script.sh "aws s3api get-object --profile ${this.aws_profile} --bucket ${this.bucket} --key ${this.stateDir}/${this.paramsFileName} ${this.paramsFileName}"
    }

    def plan(String slackHookUrl, String zendeskApiKey, boolean turboMode, String googleAnalytics,String circleCIKey, String appNpmToken, String domain = null) {
        this.script.dir('src/stacks/baseStack') {
            paramsFile(slackHookUrl, zendeskApiKey, turboMode, googleAnalytics, circleCIKey, appNpmToken, domain)
            this.script.sh """
             export isLocal=false
             terraform plan --var-file=${this.paramsFileName}
             """
        }
    }

    def planUpgrade() {
        this.script.dir('src/stacks/baseStack') {
            this.RestoreFromS3()

            this.script.sh """
            export isLocal=false
            terraform plan --var-file=${this.paramsFileName}
            """
        }
    }

    def apply(String slackHookUrl, String zendeskApiKey, boolean turboMode, String googleAnalytics, String circleCIKey, String appNpmToken, String domain = null) {
        this.script.dir('src/stacks/baseStack') {
            paramsFile(slackHookUrl, zendeskApiKey, turboMode, googleAnalytics, circleCIKey, appNpmToken, domain)
            this.BackupToS3()

            this.script.sh """
            export isLocal=false
            terraform apply --var-file=${this.paramsFileName} -auto-approve
            """
        }
    }

    def upgrade() {
        this.script.dir('src/stacks/baseStack') {
            this.RestoreFromS3()

            this.script.sh """
                         export isLocal=false
                         terraform apply --var-file=${this.paramsFileName} -auto-approve
                         """
        }
    }

    def destroy() {
        this.script.dir('src/stacks/baseStack') {
            this.RestoreFromS3()

            this.script.sh """
                         export isLocal=false
                         terraform destroy --var-file=${this.paramsFileName} -auto-approve
                         """

            this.script.sh "aws s3 rm --profile ${this.aws_profile} s3://${this.bucket}/${this.stateDir} --recursive"

            def repoExist = this.script.sh(
                    script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region} --repository-names ${this.customerBaseImageName}",
                    returnStatus: true
            )

            if (repoExist == 0) {
                this.script.sh "aws ecr delete-repository --profile ${this.aws_profile} --region ${this.region} --repository-name ${this.customerBaseImageName} --force"
            }
        }
    }

    def createCustomerBaseImage() {
        this.script.sh "rsync -av src jenkins/src/customerDockerFiles/base --exclude node_modules"



        this.script.dir('jenkins/src/customerDockerFiles/base') {
            this.script.sh "docker build -t ${this.customerBaseImageName} ."

            def repoExist = this.script.sh(
                    script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region} --repository-names ${this.customerBaseImageName}",
                    returnStatus: true
            )

            if (repoExist != 0) {
                this.script.sh "aws ecr create-repository --profile ${this.aws_profile} --region ${this.region} --repository-name ${this.customerBaseImageName}"
            }

            this.script.sh "aws ecr get-login --profile ${this.aws_profile} --region ${this.region} --no-include-email | sh";
            this.script.sh "docker tag ${this.customerBaseImageName} ${this.customerBaseImageURL}"
            this.script.sh "docker push ${this.customerBaseImageURL}";
        }
    }

    def describeCustomers() {
        def repos = this.script.sh(
                script: "aws ecr describe-repositories --profile ${this.aws_profile} --region ${this.region}",
                returnStdout: true
        )

        def reposJson = this.script.readJSON text: repos;
        def customersRepos = reposJson.repositories.findAll { it.repositoryName.startsWith(this.customerBaseImageName + "-") }
        return customersRepos.collect { it.repositoryName.substring(this.customerBaseImageName.length() + 1) }
    }
}
