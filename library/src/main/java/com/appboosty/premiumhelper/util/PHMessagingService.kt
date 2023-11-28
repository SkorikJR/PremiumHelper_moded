package com.appboosty.premiumhelper.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.appboosty.premiumhelper.Analytics
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.toto.TotoRegisterWorker

class PHMessagingService : FirebaseMessagingService() {

    private val log by timber()

    interface PushMessageListener {
        fun onPushNotification(message: RemoteMessage)
        fun onSilentPush(message: RemoteMessage)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        log.i("Message received: ${message.messageId}, ${message.notification}, ${message.messageType}, ${message.data}")

        val premiumHelper = PremiumHelper.getInstance()

        if (message.notification != null) {
            premiumHelper.configuration.appConfig.pushMessageListener?.onPushNotification(message)
        } else {
            premiumHelper.analytics.onSilentPush(getPushType(message))
            premiumHelper.configuration.appConfig.pushMessageListener?.onSilentPush(message)
        }

    }

    private fun getPushType(message: RemoteMessage): Analytics.SilentNotificationType {
        return when(message.data["push-type"]) {
            "NOTIFICATION_TYPE_CANCELLED" -> Analytics.SilentNotificationType.CANCELLED
            "NOTIFICATION_TYPE_RECOVERED" -> Analytics.SilentNotificationType.RECOVERED
            "NOTIFICATION_TYPE_HOLD" -> Analytics.SilentNotificationType.HOLD
            else -> Analytics.SilentNotificationType.UNKNOWN
        }
    }

    override fun onNewToken(token: String) {
        if (Preferences(applicationContext).hasActivePurchase()) {
            TotoRegisterWorker.schedule(applicationContext, token)
        }
    }

}