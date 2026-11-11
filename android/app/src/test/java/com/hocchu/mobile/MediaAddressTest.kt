package com.hocchu.mobile

import com.hocchu.mobile.data.publicMediaAddress
import org.junit.Assert.*
import org.junit.Test

class MediaAddressTest {
    @Test fun legacyWebAssetsUseConfiguredServer() {
        for (source in listOf("/audio/a.mp3", "audio/a.mp3", "http://localhost:5173/audio/a.mp3", "http://127.0.0.1:5173/api/public/media/audio/a.mp3")) {
            assertEquals("http://192.0.2.10:8080/api/public/media/audio/a.mp3", publicMediaAddress(source,"http://192.0.2.10:8080/",true).toString())
        }
    }
    @Test fun externalHttpsIsKeptWithoutChangingHost() {
        assertEquals("https://media.example.org/a.png", publicMediaAddress("https://media.example.org/a.png","https://app.example.org/",false).toString())
    }
    @Test fun unsafeProtocolsCredentialsAndReleaseHttpAreRejected() {
        for (source in listOf("file:///a.png", "http://media.example.org/a.png", "https://user:password@media.example.org/a.png")) {
            assertThrows(IllegalArgumentException::class.java) {publicMediaAddress(source,"https://app.example.org/",false)}
        }
    }
}
