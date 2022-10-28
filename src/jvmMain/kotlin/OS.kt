import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Paths

interface OS {
    fun isDarkModeEnabled(): Boolean
    val dataDir: File
}

class MacOS : OS {
    override fun isDarkModeEnabled() = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
    override val dataDir: File by lazy {
        Paths.get(SystemUtils.USER_HOME, "Library", "Application Support", "NUISTin").toFile()
    }
}

class Windows : OS {
    override fun isDarkModeEnabled() = WindowsRegistry.getWindowsRegistryEntry(
        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        "AppsUseLightTheme"
    ) == 0x0

    override val dataDir: File by lazy { Paths.get(System.getenv("APPDATA"), "NUISTin").toFile() }
}

class Linux : OS {
    override fun isDarkModeEnabled() =
        Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/gtk-theme").lowercase().contains("dark")

    override val dataDir: File by lazy { File(SystemUtils.getUserHome(), ".nuistin") }
}

class OtherOS : OS {
    override fun isDarkModeEnabled() = false
    override val dataDir: File by lazy { File(SystemUtils.getUserHome(), "nuistin") }
}

val currentOS: OS by lazy { when {
    SystemUtils.IS_OS_WINDOWS -> Windows()
    SystemUtils.IS_OS_MAC_OSX -> MacOS()
    SystemUtils.IS_OS_LINUX -> Linux()
    else -> OtherOS()
} }