import groovy.json.JsonSlurper;

def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    return object[awsAccount]
}