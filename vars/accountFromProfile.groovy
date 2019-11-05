def call(awsAccount) {
    def accountMapping = libraryResource "account_mapping_profile.json"
    return accountMapping.get(awsAccount)
//    return accountMapping[awsAccount]
}