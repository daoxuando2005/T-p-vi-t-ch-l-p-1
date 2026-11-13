package com.hocchu.mobile

import com.hocchu.mobile.auth.*
import org.junit.Assert.*
import org.junit.Test

class AuthModelsTest {
    @Test fun registrationRejectsPrivilegeEscalationAndBcryptByteTruncation() {
        val input = RegistrationInput(" Bé học ", " student ", " student@example.com ", " secret ", "STUDENT")
        assertTrue(input.errors().isEmpty())
        assertTrue(input.copy(role = "ADMIN").errors().containsKey("role"))
        assertTrue(input.copy(role = "TEACHER").errors().containsKey("role"))
        assertTrue(input.copy(password = "ế".repeat(25)).errors().containsKey("password"))
        assertTrue(input.copy(username = "có dấu").errors().containsKey("username"))
        assertTrue(input.copy(email = "missing-at").errors().containsKey("email"))
        assertFalse(input.toString().contains("secret"))
    }
    @Test fun endpointNormalizationPreservesBackendIdentity() {
        assertEquals("http://192.168.1.2:8080/", normalizeServer(" http://192.168.1.2:8080/api/ ", true))
        assertEquals("https://example.com/", normalizeServer("HTTPS://EXAMPLE.COM:443/api", false))
        assertEquals("http://localhost/", normalizeServer("http://localhost:80", true))
    }
    @Test fun releaseRejectsHttpAndCredentialsNeverBelongInTheUrl() {
        val invalid = listOf("", "localhost:8080", "http://example.com", "https://user:pass@example.com",
            "https://example.com/?token=secret", "https://example.com/#secret", "https://example.com/api/auth/signin",
            "file:///etc/passwd", "https://example.com:0", "https://example.com:99999")
        invalid.forEach { value ->
            try { normalizeServer(value, false); fail("Accepted invalid endpoint") }
            catch (_: AuthFailure.Configuration) { }
        }
    }
    @Test fun progressIsIsolatedByServerAndUserRatherThanDisplayName() {
        val a = accountPreferences("https://school.example", 1)
        assertEquals(a, accountPreferences("https://SCHOOL.example:443/api/", 1))
        assertNotEquals(a, accountPreferences("https://school.example", 2))
        assertNotEquals(a, accountPreferences("https://other.example", 1))
        assertNotEquals(a, "learning")
        assertFalse(a.contains("school.example"))
    }
}
