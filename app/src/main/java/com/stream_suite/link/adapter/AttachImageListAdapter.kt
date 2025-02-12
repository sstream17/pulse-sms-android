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

package com.stream_suite.link.adapter

import android.content.ContentUris
import android.database.Cursor
import android.graphics.Color
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.stream_suite.link.R
import com.stream_suite.link.adapter.view_holder.ImageViewHolder
import com.stream_suite.link.shared.data.MimeType
import com.stream_suite.link.shared.util.listener.ImageSelectedListener

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
class AttachImageListAdapter(private val images: Cursor, private val callback: ImageSelectedListener?, private val colorForMediaTile: Int)
    : RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attach_image, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == 0) {
            holder.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.image.setImageResource(R.drawable.ic_photo_gallery)
            holder.image.setBackgroundColor(colorForMediaTile)

            holder.image.setOnClickListener {
                callback?.onGalleryPicker()
            }

            if (holder.playButton.visibility != View.GONE) {
                holder.playButton.visibility = View.GONE
            }

            if (holder.selectedCheckmarkLayout.visibility != View.GONE) {
                holder.selectedCheckmarkLayout.visibility = View.GONE
            }
        } else {
            try {
                images.moveToPosition(position - 1)
                val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        images.getLong(images.getColumnIndex(MediaStore.Images.Media._ID))
                )

                holder.image.setOnClickListener {
                    if (holder.uri != null) {
                        callback?.onImageSelected(holder.uri!!, holder.mimeType ?: MimeType.IMAGE_JPG)
                    }

                    if (holder.selectedCheckmarkLayout.visibility != View.VISIBLE) {
                        holder.selectedCheckmarkLayout.visibility = View.VISIBLE
                    } else {
                        holder.selectedCheckmarkLayout.visibility = View.GONE
                    }
                }

                holder.mimeType = images.getString(images.getColumnIndex(MediaStore.Images.Media.MIME_TYPE))
                holder.uri = uri
                holder.image.setBackgroundColor(Color.TRANSPARENT)

                Glide.with(holder.image.context).load(uri)
                        .apply(RequestOptions().centerCrop())
                        .into(holder.image)

                if (holder.mimeType != null && holder.mimeType!!.contains("video") && holder.playButton.visibility == View.GONE) {
                    holder.playButton.visibility = View.VISIBLE
                } else if (holder.playButton.visibility != View.GONE) {
                    holder.playButton.visibility = View.GONE
                }

                if (holder.selectedCheckmarkLayout.visibility != View.VISIBLE && callback!!.isCurrentlySelected(holder.uri!!)) {
                    holder.selectedCheckmarkLayout.visibility = View.VISIBLE
                } else if (holder.selectedCheckmarkLayout.visibility != View.GONE) {
                    holder.selectedCheckmarkLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun getItemCount() = if (images.isClosed) 0 else images.count + 1
}
