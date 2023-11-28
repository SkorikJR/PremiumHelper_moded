package com.appboosty.ads

data class PhAdError(val code: Int, val message: String, val domain: String) {
    companion object {
        const val UNDEFINED_DOMAIN = "undefined"
    }
}