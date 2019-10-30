def call(awsAccount) {
    def accountMapping = readJSON file: "resources/account_mapping.json"
    return accountMapping[awsAccount]
}