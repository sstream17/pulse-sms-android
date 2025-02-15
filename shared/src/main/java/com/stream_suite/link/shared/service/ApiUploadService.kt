/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stream_suite.link.shared.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

import com.google.firebase.auth.FirebaseAuth

import java.io.IOException
import java.util.ArrayList

import retrofit2.Response
import com.stream_suite.link.shared.R
import com.stream_suite.link.api.implementation.ApiUtils
import com.stream_suite.link.api.implementation.Account
import com.stream_suite.link.shared.data.ColorSet
import com.stream_suite.link.shared.data.DataSource
import com.stream_suite.link.shared.data.MimeType
import com.stream_suite.link.shared.data.model.*
import com.stream_suite.link.shared.util.*

open class ApiUploadService : Service() {

    private var encryptionUtils: com.stream_suite.link.encryption.EncryptionUtils? = null
    private var completedMediaUploads = 0
    private var finished = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        uploadData()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun uploadData() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .build()
        
        startForeground(MESSAGE_UPLOAD_ID, notification)

        encryptionUtils = Account.encryptor
        if (encryptionUtils == null) {
            val intent = Intent(this, com.stream_suite.link.api.implementation.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            return
        }
        
        Thread {
            val startTime = TimeUtils.now

            uploadMessages()
            uploadConversations()
            uploadContacts(this, encryptionUtils!!)
            uploadBlacklists()
            uploadScheduledMessages()
            uploadDrafts()
            uploadTemplates()
            uploadFolders()
            uploadAutoReplies()

            Log.v(TAG, "time to upload: " + (TimeUtils.now - startTime) + " ms")

            uploadMedia()
        }.start()
    }

    private fun uploadMessages() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getMessages(this)

        if (cursor.moveToFirst()) {
            val messages = ArrayList<com.stream_suite.link.api.entity.MessageBody>()
            var firebaseNumber = 0

            do {
                val m = Message()
                m.fillFromCursor(cursor)

                // instead of sending the URI, we'll upload these images to firebase and retrieve
                // them on another device based on account id and message id.
                if (m.mimeType != MimeType.TEXT_PLAIN) {
                    m.data = "firebase " + firebaseNumber
                    firebaseNumber++
                }

                m.encrypt(encryptionUtils!!)
                val message = com.stream_suite.link.api.entity.MessageBody(m.id, m.conversationId, m.type, m.data,
                        m.timestamp, m.mimeType, m.read, m.seen, m.from, m.color, "-1", m.simPhoneNumber)
                messages.add(message)
            } while (cursor.moveToNext())

            var successPages = 0
            var expectedPages = 0
            val pages = PaginationUtils.getPages(messages, MESSAGE_UPLOAD_PAGE_SIZE)

            for (page in pages) {
                val request = com.stream_suite.link.api.entity.AddMessagesRequest(Account.accountId, page.toTypedArray())
                try {
                    val response = ApiUtils.api.message().add(request).execute()
                    expectedPages++
                    if (ApiUtils.isCallSuccessful(response)) {
                        successPages++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                Log.v(TAG, "uploaded " + page.size + " messages for page " + expectedPages)
            }

            if (successPages != expectedPages) {
                Log.v(TAG, "failed to upload messages in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "messages upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadConversations() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getAllConversations(this)

        if (cursor.moveToFirst()) {
            val conversations = arrayOfNulls<com.stream_suite.link.api.entity.ConversationBody>(cursor.count)

            do {
                val c = Conversation()
                c.fillFromCursor(cursor)
                c.encrypt(encryptionUtils!!)
                val conversation = com.stream_suite.link.api.entity.ConversationBody(c.id, c.colors.color,
                        c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent, c.ledColor, c.pinned,
                        c.read, c.timestamp, c.title, c.phoneNumbers, c.snippet, c.ringtoneUri, null,
                        c.idMatcher, c.mute, c.archive, c.private, c.folderId)/*c.imageUri*/

                conversations[cursor.position] = conversation
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddConversationRequest(Account.accountId, conversations)
            var result: Response<*>?

            var errorText: String? = null
            try {
                result = ApiUtils.api.conversation().add(request).execute()
            } catch (e: IOException) {
                try {
                    result = ApiUtils.api.conversation().add(request).execute()
                } catch (x: Exception) {
                    errorText = e.message
                    e.printStackTrace()
                    result = null
                }

            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload conversations in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, result.toString())
                Log.v(TAG, "conversations upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadBlacklists() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getBlacklists(this)

        if (cursor.moveToFirst()) {
            val blacklists = arrayOfNulls<com.stream_suite.link.api.entity.BlacklistBody>(cursor.count)

            do {
                val b = Blacklist()
                b.fillFromCursor(cursor)
                b.encrypt(encryptionUtils!!)
                val blacklist = com.stream_suite.link.api.entity.BlacklistBody(b.id, b.phoneNumber, b.phrase)

                blacklists[cursor.position] = blacklist
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddBlacklistRequest(Account.accountId, blacklists)
            val result = try {
                ApiUtils.api.blacklist().add(request).execute()
            } catch (e: IOException) {
                null
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload blacklists in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "blacklists upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadScheduledMessages() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getScheduledMessages(this)

        if (cursor.moveToFirst()) {
            val messages = arrayOfNulls<com.stream_suite.link.api.entity.ScheduledMessageBody>(cursor.count)

            do {
                val m = ScheduledMessage()
                m.fillFromCursor(cursor)
                m.encrypt(encryptionUtils!!)
                val message = com.stream_suite.link.api.entity.ScheduledMessageBody(m.id, m.to, m.data,
                        m.mimeType, m.timestamp, m.title, m.repeat)

                messages[cursor.position] = message
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddScheduledMessageRequest(Account.accountId, messages)
            val result = try {
                ApiUtils.api.scheduled().add(request).execute()
            } catch (e: IOException) {
                null
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload scheduled messages in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "scheduled messages upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadDrafts() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getDrafts(this)

        if (cursor.moveToFirst()) {
            val drafts = arrayOfNulls<com.stream_suite.link.api.entity.DraftBody>(cursor.count)

            do {
                val d = Draft()
                d.fillFromCursor(cursor)
                d.encrypt(encryptionUtils!!)
                val draft = com.stream_suite.link.api.entity.DraftBody(d.id, d.conversationId, d.data, d.mimeType)

                drafts[cursor.position] = draft
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddDraftRequest(Account.accountId, drafts)
            val result = try {
                ApiUtils.api.draft().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload drafts in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "drafts upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadTemplates() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getTemplates(this)

        if (cursor.moveToFirst()) {
            val templates = arrayOfNulls<com.stream_suite.link.api.entity.TemplateBody>(cursor.count)

            do {
                val t = Template()
                t.fillFromCursor(cursor)
                t.encrypt(encryptionUtils!!)
                val template = com.stream_suite.link.api.entity.TemplateBody(t.id, t.text)

                templates[cursor.position] = template
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddTemplateRequest(Account.accountId, templates)
            val result = try {
                ApiUtils.api.template().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload templates in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "template upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadFolders() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getFolders(this)

        if (cursor.moveToFirst()) {
            val folders = arrayOfNulls<com.stream_suite.link.api.entity.FolderBody>(cursor.count)

            do {
                val f = Folder()
                f.fillFromCursor(cursor)
                f.encrypt(encryptionUtils!!)
                val folder = com.stream_suite.link.api.entity.FolderBody(f.id, f.name, f.colors.color, f.colors.colorDark, f.colors.colorLight, f.colors.colorAccent)

                folders[cursor.position] = folder
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddFolderRequest(Account.accountId, folders)
            val result = try {
                ApiUtils.api.folder().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload folders in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "folder upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadAutoReplies() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getAutoReplies(this)

        if (cursor.moveToFirst()) {
            val replies = arrayOfNulls<com.stream_suite.link.api.entity.AutoReplyBody>(cursor.count)

            do {
                val r = AutoReply()
                r.fillFromCursor(cursor)
                r.encrypt(encryptionUtils!!)
                val reply = com.stream_suite.link.api.entity.AutoReplyBody(r.id, r.type, r.pattern, r.response)

                replies[cursor.position] = reply
            } while (cursor.moveToNext())

            val request = com.stream_suite.link.api.entity.AddAutoReplyRequest(Account.accountId, replies)
            val result = try {
                ApiUtils.api.autoReply().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload auto replies in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "auto reply upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    /**
     * Media will be uploaded after the messages finish uploading
     */
    private fun uploadMedia() {
        val builder = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading_media))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
        val manager = NotificationManagerCompat.from(this)
        startForeground(MESSAGE_UPLOAD_ID, builder.build())

        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously()
                .addOnSuccessListener { processMediaUpload(manager, builder) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "failed to sign in to firebase", e)
                    finishMediaUpload(manager)
                }
    }

    private fun processMediaUpload(manager: NotificationManagerCompat,
                                   builder: NotificationCompat.Builder) {
        ApiUtils.saveFirebaseFolderRef(Account.accountId)

        Thread {
            try {
                Thread.sleep((1000 * 60 * 2).toLong())
            } catch (e: InterruptedException) {
            }

            finishMediaUpload(manager)
        }.start()

        val media = DataSource.getAllMediaMessages(this, NUM_MEDIA_TO_UPLOAD)
        if (media.moveToFirst()) {
            val mediaCount = if (media.count < NUM_MEDIA_TO_UPLOAD) media.count else NUM_MEDIA_TO_UPLOAD
            do {
                val message = Message()
                message.fillFromCursor(media)

                Log.v(TAG, "started uploading " + message.id)

                val bytes = com.stream_suite.link.api.implementation.BinaryUtils.getMediaBytes(this, message.data, message.mimeType, true)

                ApiUtils.uploadBytesToFirebase(Account.accountId, bytes, message.id, encryptionUtils, com.stream_suite.link.api.implementation.firebase.FirebaseUploadCallback {
                    completedMediaUploads++

                    builder.setProgress(mediaCount, completedMediaUploads, false)
                    builder.setContentTitle(getString(R.string.encrypting_and_uploading_count,
                            completedMediaUploads + 1, media.count))

                    if (completedMediaUploads >= mediaCount) {
                        finishMediaUpload(manager)
                    } else if (!finished) {
                        startForeground(MESSAGE_UPLOAD_ID, builder.build())
                    }
                }, 0)
            } while (media.moveToNext())

            if (mediaCount == 0) {
                finishMediaUpload(manager)
            }
        } else {
            finishMediaUpload(manager)
        }

        media.closeSilent()
    }

    private fun finishMediaUpload(manager: NotificationManagerCompat) {
        stopForeground(true)
        stopSelf()
        finished = true
    }

    companion object {

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, ApiUploadService::class.java))
            } else {
                context.startService(Intent(context, ApiUploadService::class.java))
            }
        }

        private const val TAG = "ApiUploadService"
        private const val MESSAGE_UPLOAD_ID = 7235
        const val NUM_MEDIA_TO_UPLOAD = 20

        const val MESSAGE_UPLOAD_PAGE_SIZE = 300

        public fun uploadContacts(context: Context, encryptionUtils: com.stream_suite.link.encryption.EncryptionUtils) {
            val cursor = DataSource.getContacts(context)

            if (cursor.moveToFirst()) {
                val contacts = ArrayList<com.stream_suite.link.api.entity.ContactBody>()

                do {
                    val c = Contact()
                    c.fillFromCursor(cursor)
                    c.encrypt(encryptionUtils)

                    val contact = if (c.type != null) {
                        com.stream_suite.link.api.entity.ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.type!!, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
                    } else {
                        com.stream_suite.link.api.entity.ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
                    }

                    contacts.add(contact)
                } while (cursor.moveToNext())

                uploadContacts(contacts)
            }

            cursor.closeSilent()
        }

        fun uploadContacts(contacts: List<com.stream_suite.link.api.entity.ContactBody>) {
            val startTime = TimeUtils.now

            var successPages = 0
            var expectedPages = 0
            val pages = PaginationUtils.getPages(contacts, MESSAGE_UPLOAD_PAGE_SIZE)

            for (page in pages) {
                val request = com.stream_suite.link.api.entity.AddContactRequest(Account.accountId, page.toTypedArray())
                try {
                    val response = ApiUtils.api.contact().add(request).execute()
                    expectedPages++

                    if (ApiUtils.isCallSuccessful(response)) {
                        successPages++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                Log.v(TAG, "uploaded " + page.size + " contacts for page " + expectedPages)
            }

            if (successPages != expectedPages) {
                Log.v(TAG, "failed to upload contacts in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "contacts upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }
    }
}
