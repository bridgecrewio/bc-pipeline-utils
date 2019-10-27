import GitUtils

def util

def call() {
    if (util) return util
    else {
        util = new GitUtils()
    }
    return util
}