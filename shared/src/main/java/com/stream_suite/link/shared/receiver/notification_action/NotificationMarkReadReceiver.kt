package com.stream_suite.link.shared.receiver.notification_action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stream_suite.link.api.implementation.Account
import com.stream_suite.link.api.implementation.ApiUtils
import com.stream_suite.link.shared.data.DataSource
import com.stream_suite.link.shared.util.NotificationUtils
import com.stream_suite.link.shared.util.UnreadBadger
import com.stream_suite.link.shared.util.closeSilent
import com.stream_suite.link.shared.widget.MessengerAppWidgetProvider

open class NotificationMarkReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        handle(intent, context)
    }

    companion object {

        const val EXTRA_CONVERSATION_ID = "conversation_id"

        fun handle(intent: Intent?, context: Context) {
            Thread {
                val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1) ?: return@Thread
                DataSource.readConversation(context, conversationId)
                val conversation = DataSource.getConversation(context, conversationId)

                // cancel the notification we just replied to or
                // if there are no more notifications, cancel the summary as well
                val unseenMessages = DataSource.getUnseenMessages(context)
                try {
                    if (unseenMessages.count <= 0) {
                        NotificationUtils.cancelAll(context)
                    } else {
                        NotificationManagerCompat.from(context).cancel(conversationId.toInt())
                    }
                } catch (e: SecurityException) {
                    // not posted by this user
                }

                unseenMessages.closeSilent()

                ApiUtils.dismissNotification(Account.accountId,
                        Account.deviceId,
                        conversationId)

                com.stream_suite.link.shared.receiver.ConversationListUpdatedReceiver.Companion.sendBroadcast(context, conversationId, if (conversation == null) "" else conversation.snippet, true)

                UnreadBadger(context).clearCount()
                MessengerAppWidgetProvider.refreshWidget(context)
                NotificationManagerCompat.from(context).cancel(conversationId.toInt())
            }.start()
        }
    }
}
