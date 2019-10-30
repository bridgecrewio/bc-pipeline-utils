def call(){
    def packageJson = readJSON file: "package.json"
    def version = packageJson['version'].split('\\.')
    version[2] = version[2].toInteger() + 1
    packageJson['version'] = version.join('.')
    writeJSON file: "package.json", json: packageJson, pretty: 4
}