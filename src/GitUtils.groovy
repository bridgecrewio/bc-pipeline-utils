class GitUtils {
    def script;

    GitUtils(def script) {
        this.script = script;
    }

    def gitConfig() {
        sh("git config user.email 'ci-build@bridgecrew.io'")
        sh("git config user.name 'ci-build'")
        sh("mkdir ~/.ssh")
        sh("ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts")
    }

//    def gitCommandWithCredentials(command, credentials) {
//        this.script.withCredentials([this.script.sshUserPrivateKey(credentialsId: credentials, keyFileVariable: 'ssh_key', passphraseVariable: '', usernameVariable: 'bridgecrew')]) {
//            this.script.sh("cp ${this.script.ssh_key} ~/.ssh/id_rsa_write")
//            this.script.sh("chmod 400 ~/.ssh/id_rsa_write")
//            this.script.sh("GIT_SSH_COMMAND='ssh -i ~/.ssh/id_rsa_write' git ${command}")
//        }
//    }

    def mycheckout(String url, String branch) {
        checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'github', url: "${url}"]]])
    }

    def hello() {
        this.script.sh ("echo hello")
    }
}
