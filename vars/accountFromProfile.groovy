def call(awsAccount) {
    def accountMapping = readJSON file: "resources/account_mapping_profile.json"
    return accountMapping[awsAccount]
}