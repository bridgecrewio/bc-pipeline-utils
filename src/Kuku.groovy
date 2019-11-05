import groovy.json.JsonSlurper;

class Kuki {
    def script
    def some
    def something

    Kuki(def script) {
        this.script = script
        this.something = doSomething("dev")
    }

    @NonCPS
    def doSomething(awsAccount) {
        println new File(".").absolutePath
        def jsonSlurper = new JsonSlurper()
        def accountMapping = jsonSlurper.parse(new File("./account_mapping_profile.json").text)
        return accountMapping[awsAccount]
    }

    def print() {
        println(this.something)
        println(this.some)
    }
}