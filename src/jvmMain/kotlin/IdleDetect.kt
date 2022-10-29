class IdleDetect(private val delay: Long) {
    private var last = 0L
    fun hasIdled(): Boolean {
        val present = System.currentTimeMillis()
        if (last <= 0) {
            last = present
            return false
        }
        if (present - last > ERROR + delay) {
            return true
        }
        last = present
        return false
    }

    fun reset() {
        last = 0L
    }

    companion object {
        const val ERROR = 500L
    }
}