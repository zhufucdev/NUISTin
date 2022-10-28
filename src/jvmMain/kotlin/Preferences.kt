import kotlinx.serialization.Serializable

@Serializable
data class Preferences(var recentAccount: String, var notified: Boolean, var intervalIndex: Int) {
    companion object {
        val default get() = Preferences("", false, 0)
    }
}
