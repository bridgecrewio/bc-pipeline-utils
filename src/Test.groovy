import groovy.json.JsonSlurper;

static def doSomething(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    return object[awsAccount]
}

class Test {
    def something

    Test() {
        this.something = doSomething("dev");
    }

    def print() {
        println(this.something)
    }
}