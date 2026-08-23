package dev.helm.ota

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OtaVersionTest {

    @ParameterizedTest(name = "server {0} is newer than installed {1}")
    @CsvSource(
        "0.2.0, 0.1.0",
        "1.0.0, 0.9.9",
        "0.1.1, 0.1.0",
        "0.10.0, 0.9.0",   // numeric compare, not lexicographic
        "1.1, 1.0.9",      // missing component pads to 0, compared left to right
    )
    fun `newer server version triggers update`(server: String, installed: String) {
        assertTrue(isNewer(server, installed))
    }

    @ParameterizedTest(name = "server {0} is NOT newer than installed {1}")
    @CsvSource(
        "0.1.0, 0.1.0",
        "0.1.0, 0.2.0",
        "0.9.0, 0.10.0",
        "1.0, 1.0.0",      // equal once padded
        "0.1.0, 1.0.0",    // downgrade must never be offered
    )
    fun `same or older server version does not trigger update`(server: String, installed: String) {
        assertFalse(isNewer(server, installed))
    }

    @Test
    fun `leading v is stripped from the release tag`() {
        assertTrue(isNewer("v0.2.0", "0.1.0"))
        assertFalse(isNewer("v0.1.0", "0.1.0"))
    }

    @Test
    fun `pre-release and build metadata suffixes are ignored`() {
        assertFalse(isNewer("0.1.0-rc1", "0.1.0"))
        assertFalse(isNewer("0.1.0+42", "0.1.0"))
        assertTrue(isNewer("0.2.0-rc1", "0.1.0"))
    }

    @Test
    fun `unparseable versions never trigger an update`() {
        // A garbage tag_name must not push an APK onto the head unit.
        assertFalse(isNewer("", "0.1.0"))
        assertFalse(isNewer("latest", "0.1.0"))
        assertFalse(isNewer("nightly", "0.1.0"))
    }

    @Test
    fun `unparseable installed version accepts any real release`() {
        // versionName came back empty from PackageManager — treated as 0.
        assertTrue(isNewer("0.1.0", ""))
    }
}
