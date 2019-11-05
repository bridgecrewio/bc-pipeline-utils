import groovy.json.JsonSlurper;

def doSomething(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    def jsonSlurper = new JsonSlurper()
    def object = jsonSlurper.parseText(accountMapping)
    return object[awsAccount]
}

class Kuki {
    def script
    def some
    def something

    Kuki(def script) {
        this.script = script
        this.some = accountFromProfile("dev")
        this.something = doSomething("dev")
    }

    def print() {
        println(this.something)
        println(this.some)
    }
}