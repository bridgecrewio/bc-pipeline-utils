def accountMapping = readJSON file: "account_mapping_profile.json"

def call(awsAccount) {
    return accountMapping[awsAccount]
}