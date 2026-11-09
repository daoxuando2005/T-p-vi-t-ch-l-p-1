package com.hocchu.mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in only: README's public demo account, no registration or changes to server data. */
class AuthLiveBackendTest {
    @Test fun publicDemoStudentSignsInAndMeVerifiesOverTheDeviceNetwork() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveBackend") == "true")
        val api = AuthApi()
        val server = normalizeServer(BuildConfig.DEFAULT_API_BASE_URL, true)
        val signedIn = api.signIn(server, "student1", "123456")
        val me = api.me(signedIn)
        assertEquals(signedIn.account.id, me.account.id)
        assertEquals(signedIn.token, me.token)
        assertTrue("ROLE_STUDENT" in me.account.roles)
    }
}
