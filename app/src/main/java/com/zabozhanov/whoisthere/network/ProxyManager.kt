package com.zabozhanov.whoisthere.network

import java.net.Proxy

object ProxyManager {

    public fun getProxyType(): Proxy.Type {
        return Proxy.Type.HTTP
    }

    public fun getProxyString(): String {
        return ""
    }

    public fun getProxyPort(): Int {
        return 8080
    }
}