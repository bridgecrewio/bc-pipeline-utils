import groovy.json.JsonSlurper;

@NonCPS
def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    return object[awsAccount]
}