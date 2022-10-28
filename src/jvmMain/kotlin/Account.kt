data class Account(val id: String, val password: String, val carrier: Carrier, val remember: Boolean, val autostart: Boolean)

enum class Carrier(val channel: Int) {
    MOBILE(2), TELECOM(3), UNICOM(4)
}
