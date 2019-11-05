def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    println accountMapping
    return accountMapping[awsAccount]
}