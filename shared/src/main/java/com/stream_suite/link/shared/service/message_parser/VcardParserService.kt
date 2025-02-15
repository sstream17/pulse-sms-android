package com.stream_suite.link.shared.service.message_parser

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stream_suite.link.shared.R
import com.stream_suite.link.shared.data.ColorSet
import com.stream_suite.link.shared.data.DataSource
import com.stream_suite.link.shared.data.model.Message
import com.stream_suite.link.shared.receiver.MessageListUpdatedReceiver
import com.stream_suite.link.shared.util.AndroidVersionUtil
import com.stream_suite.link.shared.util.NotificationUtils
import com.stream_suite.link.shared.util.vcard.VcardParser
import com.stream_suite.link.shared.util.vcard.VcardParserFactory

class VcardParserService : IntentService("VcardParserService") {

    override fun onHandleIntent(intent: Intent?) {
        if (AndroidVersionUtil.isAndroidO) {
            val notification = NotificationCompat.Builder(this,
                    NotificationUtils.SILENT_BACKGROUND_CHANNEL_ID)
                    .setContentTitle(getString(R.string.media_parse_text))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build()
            startForeground(VCARD_PARSE_FOREGROUND_ID, notification)
        }

        if (intent == null) {
            stopForeground(true)
            return
        }

        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val message = DataSource.getMessage(this, messageId)
        if (message == null) {
            stopForeground(true)
            return
        }

        val parsers = createParsers(this, message)
        if (parsers.isEmpty()) {
            stopForeground(true)
            return
        }

        parsers.forEach {
            val parsedMessage = it.parse(message)
            if (parsedMessage != null) {
                DataSource.insertMessage(this, parsedMessage, message.conversationId, true)
                MessageListUpdatedReceiver.sendBroadcast(this, message.conversationId, parsedMessage.data, parsedMessage.type)
            }
        }

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    companion object {
        fun start(context: Context, message: Message) {
            val parser = Intent(context, VcardParserService::class.java)
            parser.putExtra(EXTRA_MESSAGE_ID, message.id)

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(parser)
            } else {
                context.startService(parser)
            }
        }

        private val VCARD_PARSE_FOREGROUND_ID = 1337

        val EXTRA_MESSAGE_ID = "message_id"

        fun createParsers(context: Context, message: Message): List<VcardParser> {
            return VcardParserFactory().getInstances(context, message)
        }
    }
}
