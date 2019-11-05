def call(awsAccount) {
    def accountMapping = readJSON file: "account_mapping_profile.json"
    return accountMapping[awsAccount]
}