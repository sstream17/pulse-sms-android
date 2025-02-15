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

package com.stream_suite.link.shared.data.model

import android.database.Cursor
import com.stream_suite.link.api.entity.DraftBody
import com.stream_suite.link.encryption.EncryptionUtils
import com.stream_suite.link.shared.data.model.ScheduledMessage.Companion.REPEAT_NEVER

/**
 * Table for holding drafts for a conversation.
 */
class Draft : DatabaseTable {

    var id: Long = 0
    var conversationId: Long = 0
    var data: String? = null
    var mimeType: String? = null
    var scheduledTimestamp: Long = 0
    var scheduledRepeat: Int = REPEAT_NEVER

    constructor()
    constructor(body: DraftBody) {
        this.id = body.deviceId
        this.conversationId = body.deviceConversationId
        this.data = body.data
        this.mimeType = body.mimeType
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = INDEXES

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_CONVERSATION_ID -> this.conversationId = cursor.getLong(i)
                COLUMN_DATA -> this.data = cursor.getString(i)
                COLUMN_MIME_TYPE -> this.mimeType = cursor.getString(i)
                COLUMN_SCHEDULED_TIME -> this.scheduledTimestamp = cursor.getLong(i)
                COLUMN_SCHEDULED_REPEAT -> this.scheduledRepeat = cursor.getInt(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.data = utils.encrypt(this.data)
        this.mimeType = utils.encrypt(this.mimeType)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.data = utils.decrypt(this.data)
            this.mimeType = utils.decrypt(this.mimeType)
        } catch (e: Exception) {
        }
    }

    companion object {

        const val TABLE = "draft"

        const val COLUMN_ID = "_id"
        const val COLUMN_CONVERSATION_ID = "conversation_id"
        const val COLUMN_DATA = "data"
        const val COLUMN_MIME_TYPE = "mime_type"
        const val COLUMN_SCHEDULED_TIME = "scheduled_timestamp"
        const val COLUMN_SCHEDULED_REPEAT = "scheduled_repeat"

        private const val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_CONVERSATION_ID + " integer not null, " +
                COLUMN_DATA + " text not null, " +
                COLUMN_MIME_TYPE + " text not null, " +
                COLUMN_SCHEDULED_TIME + " integer not null default 0, " +
                COLUMN_SCHEDULED_REPEAT + " integer not null default 0" +
                ");"

        private val INDEXES = arrayOf("create index if not exists conversation_id_draft_index on " + TABLE +
                " (" + COLUMN_CONVERSATION_ID + ");")
    }

}
