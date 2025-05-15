package com.example.data.datasource.local.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.domain.model.MediaImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.Result
import java.time.Instant

/**
 * 로컬 미디어 파일에 접근하는 LocalMediaDataSource의 구현체입니다.
 * ContentResolver를 사용하여 MediaStore에서 이미지를 가져옵니다.
 */
class LocalMediaDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : LocalMediaDataSource {

    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>> = withContext(dispatcherProvider.io) {
        runCatching {
            val images = mutableListOf<MediaImage>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED
            )

            // Android Q (API 29) 이상에서는 MediaStore.Images.Media.RELATIVE_PATH 등을 사용할 수 있지만,
            // 여기서는 DATE_ADDED로 정렬하고 페이징합니다.
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val offset = (page - 1) * pageSize

            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(
                queryUri,
                projection,
                null, // selection
                null, // selectionArgs
                "$sortOrder LIMIT $pageSize OFFSET $offset"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn))

                    images.add(
                        MediaImage(
                            id = id,
                            contentPath = contentUri,
                            name = name,
                            size = size,
                            mimeType = mimeType,
                            dateAdded = Instant.ofEpochSecond(dateAdded)
                        )
                    )
                }
            } ?: throw IOException("ContentResolver query returned null cursor.")
            images
        }
    }
} 