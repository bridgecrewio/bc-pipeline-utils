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
        GitUtils.configureGithubHost(this.script)
        this.script.sh("git config user.email 'ci-build@bridgecrew.io'")
        this.script.sh("git config user.name 'ci-build'")
    }

    def withCredentials(String command) {
        this.script.withCredentials([this.script.sshUserPrivateKey(credentialsId: this.credentials, keyFileVariable: 'ssh_key', passphraseVariable: '', usernameVariable: 'bridgecrew')]) {
            this.script.sh("cp ${this.script.ssh_key} ~/.ssh/id_rsa_${this.credentials}")
            this.script.sh("chmod 400 ~/.ssh/id_rsa_${this.credentials}")
            this.script.sh("GIT_SSH_COMMAND='ssh -i ~/.ssh/id_rsa_${this.credentials}' git ${command}")
        }
    }

    def checkout(String url, String branch, String credentialsId = 'github', boolean shallowClone = true) {
        this.script.checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], doGenerateSubmoduleConfigurations: false, extensions:  [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: ${shallowClone}], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "${credentialsId}", url: "${url}"]]])
    }

    static def configureGithubHost(def script) {
        script.sh("""
            mkdir -p ~/.ssh
            ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
            ssh-keyscan -t rsa spring.paloaltonetworks.com >> ~/.ssh/known_hosts
            """)
    }

    static def addPrivateKey(def script, String credentialsId) {
        GitUtils.configureGithubHost(script)
        script.withCredentials([script.sshUserPrivateKey(credentialsId: credentialsId, keyFileVariable: 'ssh_key')]) {
            def keyPath = String.format('~/.ssh/id_rsa_%s', credentialsId)
            script.sh("""
                cp ${script.ssh_key} ${keyPath}
                chmod 400 ${keyPath}
                echo IdentityFile ${keyPath} >> ~/.ssh/config
            """)
        }
    }
}
