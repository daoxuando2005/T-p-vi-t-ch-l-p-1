package com.hocchu.mobile.data

import java.net.URI

/** Public assets use the configured backend; localhost in legacy web URLs is not the phone. */
internal fun publicMediaAddress(raw: String, server: String, allowHttp: Boolean): URI {
    var source = raw.trim()
    val original = URI(source)
    if (original.host in listOf("localhost", "127.0.0.1") && original.port == 5173 &&
        listOf("/alphabet/", "/audio/", "/example_images/", "/api/public/media/").any {original.path.startsWith(it)}) {
        source = original.rawPath
    }
    for (folder in listOf("alphabet", "audio", "example_images")) {
        if (source.startsWith("$folder/")) source = "/$source"
        if (source.startsWith("/$folder/")) source = "/api/public/media$source"
    }
    return URI(server).resolve(source).also {
        require(it.scheme in (if (allowHttp) listOf("http", "https") else listOf("https")) && it.userInfo == null && it.host != null)
    }
}
