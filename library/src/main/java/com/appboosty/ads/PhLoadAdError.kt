package com.appboosty.ads

data class PhLoadAdError(val code: Int, val message: String, val domain: String, val cause: String? = null)