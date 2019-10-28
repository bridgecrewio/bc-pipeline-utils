def call() {
    def userId
    if (currentBuild.rawBuild.getCause(Cause.UserIdCause)) {
        wrap([$class: 'BuildUser']) { userId = env.BUILD_USER }
    } else userId = "System-User"
    addShortText(text: userId, border: "0")
}