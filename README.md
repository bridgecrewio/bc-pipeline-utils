# bc-pipeline-utils

This repository holds the global Jenkins utils library that will be ready to import in any pipeline created to help you develop pipelines faster and avoid duplicate code blocks.

The structure of this repository is following the required structure as described in the documentation of [Jenkins pipeline shared libraries]([https://jenkins.io/doc/book/pipeline/shared-libraries/](https://jenkins.io/doc/book/pipeline/shared-libraries/))

The file structure of this repository contain 3 main directories:
 * src
 * vars
 * resources
#
### src
Holds groovy classes to be imported in the Jenkinsfile by the name of the file, example:
> @Library("Utils")_
> import GitUtils
>
> def git = new GitUtils(this, env.GITHUB_WRITE)
> ...
> script {
> git.withCredentials("push")
>}	
>...

the code above will import the file GitUtils from the src directory and create new instance of this class ready to use it's methods

### vars
Holds one method groovy files that implements the method `call`.
This will create a method on the global scope of the pipeline ready to use with just using the `@Library` notation at the top of the Jenkinsfile, example:
>@Library("Utils)_
>...
>def awsAccountId = accountFromProfile(env.AWS_BASE_PROFILE)

The code above will use a file under the directory vars called accountFromProfile.groovy, that implements the `call` method the convert an AWS profile name (such as `stage`) to it's AWS account id number (such as `312345614275`)

### resources
Holds any other resource files needed for the methods and classes in this repository to work proper, example:
In the previous example we used the global function accountFromProfile that convert AWS account profile into it's co-related AWS account id number,
In order to do so in the accountFromProfile.groovy file under the vars directory we load a json file that maps our AWS accounts and their profile names.
the json file called `account_mapping_profile.json` is in the resources directory and should imported like this:
> def call(awsAccount) {  
    def accountMapping = readJSON file: "resources/account_mapping_profile.json"  
  return accountMapping[awsAccount]  
}
