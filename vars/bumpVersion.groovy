def call(){
    println "bumping version"
    def packageJson = readJSON file: "package.json"
    println "current version ${packageJson['version']}"
    def version = packageJson['version'].split('\\.')
    version[2] = version[2].toInteger() + 1
    packageJson['version'] = version.join('.')
    println "next version ${packageJson['version']}"
    writeJSON file: "package.json", json: packageJson, pretty: 4
    return packageJson['version']
}