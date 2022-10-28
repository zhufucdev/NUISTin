import kotlinx.serialization.Serializable

@Serializable
data class Preferences(var recentAccount: String) {
    companion object {
        val default get() = Preferences("")
    }
}
