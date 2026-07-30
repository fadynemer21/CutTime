package com.fadynemer.cutime.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.fadynemer.cutime.model.GalleryUploadRequest

object SelectedGalleryImageResolver {
    fun resolve(
        context: Context,
        uri: Uri,
        caption: String = ""
    ): Result<GalleryUploadRequest> {
        return runCatching {
            val contentType =
                context.contentResolver.getType(uri)
                    ?: throw IllegalArgumentException(
                        "The selected file type could not be identified."
                    )
            val size = querySize(context, uri)

            GalleryUploadRequest(
                localUri = uri.toString(),
                contentType = contentType.lowercase(),
                sizeBytes = size,
                caption =
                    GalleryImageValidator.sanitizeCaption(caption)
            )
        }
    }

    private fun querySize(
        context: Context,
        uri: Uri
    ): Long {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex =
                cursor.getColumnIndex(OpenableColumns.SIZE)
            if (
                sizeIndex >= 0 &&
                cursor.moveToFirst() &&
                !cursor.isNull(sizeIndex)
            ) {
                return cursor.getLong(sizeIndex)
            }
        }

        context.contentResolver
            .openAssetFileDescriptor(uri, "r")
            ?.use { descriptor ->
                if (descriptor.length >= 0L) {
                    return descriptor.length
                }
            }

        throw IllegalArgumentException(
            "The selected image size could not be read."
        )
    }
}
