def call(stage) {
    println "bumping version"
    def packageJson = readJSON file: "package.json"
    println "current version ${packageJson['version']}"
    println "going to bump ${stage}"
    def index = 2
    switch (stage) {
        case "major": index = 0; break;
        case "minor": index = 1; break;
        case "patch": index = 2; break;
    }
    def version = packageJson['version'].split('\\.')
    version[index] = version[index].toInteger() + 1
    for (def i = 2; i > index; i--) version[i] = 0
    packageJson['version'] = version.join('.')
    println "next version ${packageJson['version']}"
    writeJSON file: "package.json", json: packageJson, pretty: 4
    return packageJson['version']
}
