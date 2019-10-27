def slackMessage(def script,def committerEmail) {
        def icons = [":unicorn_face:", ":beer:", ":bee:", ":man_dancing:",
            ":ghost:", ":dancer:", ":scream_cat:"]

        def userId = "";

        if(currentBuild.rawBuild.getCause(Cause.UserIdCause)) {
            userId = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
        } else {
            userId = "System-User"
        }

        def randomIndex = (new Random()).nextInt(icons.size())

        def msg = "*Job:* ${currentBuild.projectName}"
        if(script.currentBuild.resultIsBetterOrEqualTo("SUCCESS")) {
              msg += " ${icons[randomIndex]}"
        }
        def gitUrl = "https://github.com/bridgecrewio/platform";
        msg += "\n"
        msg += "*Build:* <${env.BUILD_URL}|${currentBuild.displayName}> \n"
        msg += "*Status:* ${currentBuild.currentResult} \n"
        msg += "*User:* @${committerEmail}\n"
        msg += "*Running By:* ${userId}\n"
        msg += "*Branch:* <${gitUrl}/tree/${env.GITHUB_BRANCH_NAME}|(${env.GITHUB_BRANCH_NAME})>\n"
        msg += "*Changes:* \n"

        if (!script.currentBuild.changeSets.isEmpty()) {
            script.currentBuild.changeSets.first().getLogs().each {
                msg += "- <${gitUrl}/commit/${it.getCommitId()}|`${it.getCommitId().substring(0, 8)}`> *${it.getComment().substring(0, it.getComment().length()-1)}*\n"
            }
        } else {
            msg += "no changes for this run\n"
        }

        if (msg.length() > 1024) msg.take(msg.length() - 1024)

        return msg;
}


return this