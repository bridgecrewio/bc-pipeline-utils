def call(awsAccount) {
    def accountMapping = readJSON file: "account_mapping.json"
    return accountMapping[awsAccount]
}