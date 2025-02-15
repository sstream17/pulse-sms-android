package com.stream_suite.link.shared.receiver.notification_action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.stream_suite.link.shared.service.NotificationDismissedReceiver

class NotificationDismissedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getLongExtra(NotificationDismissedReceiver.EXTRA_CONVERSATION_ID, 0L)

        val serviceIntent = Intent(context, NotificationDismissedReceiver::class.java)
        serviceIntent.putExtra(NotificationDismissedReceiver.EXTRA_CONVERSATION_ID, conversationId)

        Thread { NotificationDismissedReceiver.handle(serviceIntent, context) }.start()
    }
}
