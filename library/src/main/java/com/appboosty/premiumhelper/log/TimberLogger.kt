package com.appboosty.premiumhelper.log

import timber.log.Timber

class TimberLogger(thisRef: Any, tag: String? = null) : Timber.Tree() {

    private val tag = tag ?: thisRef.toTag()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Timber.tag(tag ?: this.tag).log(priority, t, message)
    }

    private fun Any.toTag(): String {
        val str = this::class.java.simpleName
                .run { if (endsWith("Impl")) substring(0, length - 4) else this }
        if (str.length <= 23) {
            return str
        }
        return str
                .replace("Fragment", "Frag")
                .replace("ViewModel", "VM")
                .replace("Controller", "Ctrl")
                .replace("Manager", "Mgr")
                .take(23)
    }
}