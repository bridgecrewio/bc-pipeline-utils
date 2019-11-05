import groovy.json.JsonSlurper;

static def doSomething(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    return object[awsAccount]
}

class Kuku {
    def something
    def script
    def some

    Kuku(def script) {
        this.script = script
        this.something = doSomething("dev")
        this.some = accountFromProfile("dev")
    }

    def print() {
        this.script.println(this.something)
        this.script.println(this.some)
    }
}