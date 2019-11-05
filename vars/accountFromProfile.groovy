def call(awsAccount) {
    sh "ls -la"
    def accountMapping = readJSON file: "account_mapping_profile.json"
    return accountMapping[awsAccount]
}