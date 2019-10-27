class GitUtils {
    def script;

    GitUtils(def script) {
        this.script = script;
    }



    def gitConfig() {
        this.script.sh("git config user.email 'ci-build@bridgecrew.io'")
        this.script.sh("git config user.name 'ci-build'")
        this.script.sh("mkdir ~/.ssh")
        this.script.sh("ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts")
    }

    def gitCommandWithCredentials(command, credentials) {
        this.script.withCredentials([this.script.sshUserPrivateKey(credentialsId: credentials, keyFileVariable: 'ssh_key', passphraseVariable: '', usernameVariable: 'bridgecrew')]) {
            this.script.sh("cp ${this.script.ssh_key} ~/.ssh/id_rsa_write")
            this.script.sh("chmod 400 ~/.ssh/id_rsa_write")
            this.script.sh("GIT_SSH_COMMAND='ssh -i ~/.ssh/id_rsa_write' git ${command}")
        }
    }

    def checkout(url, branch) {
        this.script.checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'github', url: "${url}"]]])
    }

}
def hello(){
    echo "hello Or Evron"
}

def newInstance(def script) {
    return new GitUtils(script);
}
