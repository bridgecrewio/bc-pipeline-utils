import groovy.json.JsonSlurper;

def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    println object[awsAccount]
    return object[awsAccount]
}