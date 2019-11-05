def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    return accountMapping[awsAccount]
}