class GitUtils {
    def script;
    def credentials

    GitUtils(def script) {
        this.script = script;
    }

    GitUtils(def script, def credentials) {
        this.script = script
        this.credentials = credentials
    }

    def gitConfig() {
        this.script.sh("git config user.email 'ci-build@bridgecrew.io'")
        this.script.sh("git config user.name 'ci-build'")
        this.script.sh("mkdir ~/.ssh")
        this.script.sh("ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts")
    }

    def withCredentials(String command) {
        this.script.withCredentials([this.script.sshUserPrivateKey(credentialsId: this.credentials, keyFileVariable: 'ssh_key', passphraseVariable: '', usernameVariable: 'bridgecrew')]) {
            this.script.sh("cp ${this.script.ssh_key} ~/.ssh/id_rsa_write")
            this.script.sh("chmod 400 ~/.ssh/id_rsa_write")
            this.script.sh("GIT_SSH_COMMAND='ssh -i ~/.ssh/id_rsa_write' git ${command}")
        }
    }

    def checkout(String url, String branch) {
        this.script.checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'github', url: "${url}"]]])
    }
}
