class Kuki {
    def something
    def accountFromProfile = [
            "dev"  : "090772183824",
            "test" : "148726905943",
            "stage": "372188014275",
            "acme3": "714018233037",
            "acme" : "809694787632",
            "prod" : "890234264427",
            "root" : "986292867370",
            "demo" : "418408045322"
    ]

    Kuki() {
        this.something = accountFromProfile["dev"]
    }

    def print() {
        println(this.something)
    }
}