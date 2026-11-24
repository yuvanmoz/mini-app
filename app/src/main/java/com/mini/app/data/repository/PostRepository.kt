package com.mini.app.data.repository

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mini.app.data.local.AppDatabase
import com.mini.app.data.local.PostDao
import com.mini.app.data.local.PostWithDetails
import com.mini.app.data.local.entities.AuthorEntity
import com.mini.app.data.local.entities.MediaItemEntity
import com.mini.app.data.local.entities.PostEntity
import com.mini.app.data.local.entities.PostTagCrossRef
import com.mini.app.data.local.entities.TagEntity
import com.mini.app.data.remote.FxTwitterApiService
import com.mini.app.data.remote.OEmbedApiService
import com.mini.app.domain.model.Author
import com.mini.app.domain.model.MediaItem
import com.mini.app.domain.model.Post
import com.mini.app.domain.model.Tag
import com.mini.app.util.XUrlParser
import com.mini.app.widget.PostWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class VideoNotSupportedException(message: String = "Video posts aren't supported yet") : Exception(message)

enum class ImportStep {
    RECEIVING, FETCHING, EXTRACTING, DOWNLOADING_MEDIA, SAVING, COMPLETE, ERROR
}

data class PostBackupContainer(
    val authors: List<AuthorEntity> = emptyList(),
    val posts: List<PostEntity> = emptyList(),
    val mediaItems: List<MediaItemEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val postTagCrossRefs: List<PostTagCrossRef> = emptyList()
)

@Singleton
class PostRepository @Inject constructor(
    private val db: AppDatabase,
    private val fxApi: FxTwitterApiService,
    private val oEmbedApi: OEmbedApiService,
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val postDao: PostDao = db.postDao()
    private val gson = Gson()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cleanupPublicAvatars()
            restoreFromBackupIfEmpty()
        }
    }

    private fun ensureNoMedia(dir: File) {
        try {
            val nomedia = File(dir, ".nomedia")
            if (!nomedia.exists()) nomedia.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanupPublicAvatars() {
        try {
            val publicAvatarsDir = File(getPublicBackupDir(), "avatars")
            if (publicAvatarsDir.exists()) {
                publicAvatarsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPublicBackupDir(): File {
        val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Mini")
        if (!pubDir.exists()) pubDir.mkdirs()
        ensureNoMedia(pubDir)
        return pubDir
    }

    private fun getExtBackupDir(): File {
        val extDir = context.getExternalFilesDir(null) ?: getPublicBackupDir()
        if (!extDir.exists()) extDir.mkdirs()
        ensureNoMedia(extDir)
        return extDir
    }

    private suspend fun saveBackupData() = withContext(Dispatchers.IO) {
        try {
            val container = PostBackupContainer(
                authors = postDao.getAllAuthorsSync(),
                posts = postDao.getAllPostsSync(),
                mediaItems = postDao.getAllMediaItemsSync(),
                tags = postDao.getAllTagsSync(),
                postTagCrossRefs = postDao.getAllPostTagCrossRefsSync()
            )
            val json = gson.toJson(container)

            val file1 = File(getPublicBackupDir(), "mini_backup.json")
            file1.writeText(json)

            val file2 = File(getExtBackupDir(), "mini_backup.json")
            file2.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreFromBackupIfEmpty() = withContext(Dispatchers.IO) {
        try {
            if (postDao.getPostCountSync() > 0) return@withContext

            var backupFile = File(getPublicBackupDir(), "mini_backup.json")
            if (!backupFile.exists() || backupFile.length() == 0L) {
                backupFile = File(getExtBackupDir(), "mini_backup.json")
            }

            if (backupFile.exists() && backupFile.length() > 0) {
                val json = backupFile.readText()
                val type = object : TypeToken<PostBackupContainer>() {}.type
                val container: PostBackupContainer? = gson.fromJson(json, type)

                if (container != null) {
                    container.authors.forEach { postDao.insertAuthor(it) }
                    container.posts.forEach { postDao.insertPost(it) }
                    container.mediaItems.forEach { item ->
                        // If local path was lost on reinstall, check if image exists in external backup dir
                        val restoredPath = item.localPath?.let { path ->
                            val f = File(path)
                            if (f.exists()) path else {
                                val fileName = f.name
                                val backupImage = File(getPublicBackupDir(), "media/$fileName")
                                if (backupImage.exists()) backupImage.absolutePath else path
                            }
                        } ?: item.localPath
                        postDao.insertMediaItems(listOf(item.copy(localPath = restoredPath)))
                    }
                    container.tags.forEach { postDao.insertTag(it) }
                    container.postTagCrossRefs.forEach { postDao.insertPostTagCrossRef(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPostsWithDetails().map { list ->
            list.map { it.toDomainPost() }
        }
    }

    fun getPostsForTag(tagName: String): Flow<List<Post>> {
        return postDao.getPostsForTag(tagName).map { list ->
            list.map { it.toDomainPost() }
        }
    }

    fun searchPosts(query: String): Flow<List<Post>> {
        return postDao.searchPosts("%$query%").map { list ->
            list.map { it.toDomainPost() }
        }
    }

    suspend fun existsById(tweetId: String): Boolean {
        return postDao.existsById(tweetId)
    }

    suspend fun getPostById(tweetId: String): Post? = withContext(Dispatchers.IO) {
        postDao.getPostWithDetailsById(tweetId)?.toDomainPost()
    }

    suspend fun deletePost(tweetId: String) {
        withContext(Dispatchers.IO) {
            val exists = postDao.existsById(tweetId)
            if (exists) {
                postDao.deletePost(PostEntity(tweetId, "", "", "", 0))
                postDao.deleteMediaForPost(tweetId)
                postDao.deleteTagsForPost(tweetId)
                val mediaDir = File(context.filesDir, "media/$tweetId")
                if (mediaDir.exists()) {
                    mediaDir.deleteRecursively()
                }
                saveBackupData()
                try { PostWidgetProvider.updateAllWidgets(context) } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun getPostCount(): Flow<Int> = postDao.getPostCount()

    fun getFavoritePosts(): Flow<List<Post>> {
        return postDao.getFavoritePostsWithDetails().map { list ->
            list.map { it.toDomainPost() }
        }
    }

    fun getFavoriteCount(): Flow<Int> = postDao.getFavoriteCount()

    suspend fun toggleFavorite(tweetId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        postDao.updateFavorite(tweetId, isFavorite)
        saveBackupData()
    }

    suspend fun updateNote(tweetId: String, note: String?) = withContext(Dispatchers.IO) {
        postDao.updateNote(tweetId, note)
        saveBackupData()
    }

    // Tag Operations
    fun getAllTags(): Flow<List<Tag>> {
        return postDao.getAllTagsWithCount().map { list ->
            list.map { Tag(name = it.name, postCount = it.postCount, previewImageUrl = it.previewImageUrl) }
        }
    }

    suspend fun createTag(name: String) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            postDao.insertTag(TagEntity(name = trimmed))
            saveBackupData()
        }
    }

    suspend fun renameTag(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty() && oldName != trimmed) {
            postDao.renameTag(oldName, trimmed)
            saveBackupData()
        }
    }

    suspend fun deleteTag(name: String) = withContext(Dispatchers.IO) {
        postDao.deleteTag(name)
        postDao.deleteTagCrossRefs(name)
        saveBackupData()
    }

    suspend fun setPostTags(tweetId: String, tagNames: List<String>) = withContext(Dispatchers.IO) {
        postDao.setPostTags(tweetId, tagNames)
        saveBackupData()
        try { PostWidgetProvider.updateAllWidgets(context) } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun restorePost(post: Post) = withContext(Dispatchers.IO) {
        val authorEntity = AuthorEntity(
            handle = post.author.handle,
            displayName = post.author.displayName,
            avatarRemoteUrl = post.author.avatarRemoteUrl,
            localAvatarPath = post.author.avatarUrl
        )
        val postEntity = PostEntity(
            tweetId = post.tweetId,
            authorHandle = post.author.handle,
            text = post.text,
            tweetUrl = post.tweetUrl,
            originalDateMs = post.originalDateMs,
            savedAtMs = post.savedAtMs,
            collectionId = post.collectionId,
            isFavorite = post.isFavorite,
            note = post.note
        )
        val mediaEntities = post.mediaItems.map {
            MediaItemEntity(
                id = 0,
                postTweetId = post.tweetId,
                remoteUrl = it.remoteUrl,
                localPath = it.localPath,
                mediaType = it.mediaType,
                width = it.width,
                height = it.height
            )
        }
        postDao.insertAuthor(authorEntity)
        postDao.insertPost(postEntity)
        postDao.insertMediaItems(mediaEntities)
        post.tags.forEach { tag ->
            postDao.insertTag(TagEntity(name = tag.name))
            postDao.insertPostTagCrossRef(PostTagCrossRef(tweetId = post.tweetId, tagName = tag.name))
        }
        saveBackupData()
        try { PostWidgetProvider.updateAllWidgets(context) } catch (e: Exception) { e.printStackTrace() }
    }

    data class StorageInfo(
        val savedMediaBytes: Long,
        val cacheBytes: Long
    ) {
        val totalBytes: Long get() = savedMediaBytes + cacheBytes
    }

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        fun calculateDirSize(dir: File): Long {
            var size = 0L
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
            return size
        }

        val savedMedia = calculateDirSize(context.filesDir)
        val cache = calculateDirSize(context.cacheDir)

        StorageInfo(
            savedMediaBytes = savedMedia,
            cacheBytes = cache
        )
    }

    suspend fun getStorageUsageBytes(): Long = withContext(Dispatchers.IO) {
        getStorageInfo().totalBytes
    }

    suspend fun clearImageCache(): Long = withContext(Dispatchers.IO) {
        fun calculateDirSize(dir: File): Long {
            var size = 0L
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
            return size
        }

        val cacheDir = context.cacheDir
        val bytesBefore = calculateDirSize(cacheDir)

        try {
            // Clear Coil in-memory and disk caches
            val imageLoader = coil.Coil.imageLoader(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            cacheDir.listFiles()?.forEach {
                it.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val bytesAfter = calculateDirSize(cacheDir)
        maxOf(0L, bytesBefore - bytesAfter)
    }

    suspend fun exportToMarkdown(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val posts = postDao.getAllPostsWithDetails().first().map { it.toDomainPost() }
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Mini_Saved_Posts_$timestamp.md"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, fileName)

            val sb = StringBuilder()
            sb.append("# Mini Saved Posts\n")
            sb.append("> Exported on ").append(java.text.SimpleDateFormat("MMMM d, yyyy · h:mm a", java.util.Locale.getDefault()).format(java.util.Date())).append("\n\n")
            sb.append("Total Posts: ").append(posts.size).append("\n\n---\n\n")

            posts.forEachIndexed { index, post ->
                sb.append("## ").append(index + 1).append(". Post by @").append(post.author.handle)
                if (post.isFavorite) sb.append(" ⭐")
                sb.append("\n\n")
                sb.append("**Author:** ").append(post.author.displayName).append(" (@").append(post.author.handle).append(")\n\n")
                if (post.originalDateMs > 0) {
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(post.originalDateMs))
                    sb.append("**Original Date:** ").append(dateStr).append("\n\n")
                }
                sb.append("**Source URL:** [View on X](").append(post.tweetUrl).append(")\n\n")

                if (post.tags.isNotEmpty()) {
                    sb.append("**Collections/Tags:** ")
                    sb.append(post.tags.joinToString(", ") { "#${it.name}" })
                    sb.append("\n\n")
                }

                if (!post.note.isNullOrBlank()) {
                    sb.append("> **Personal Note:** ").append(post.note).append("\n\n")
                }

                sb.append("### Content\n\n")
                sb.append(post.text).append("\n\n")

                if (post.mediaItems.isNotEmpty()) {
                    sb.append("### Media (").append(post.mediaItems.size).append(" images)\n\n")
                    post.mediaItems.forEachIndexed { mIdx, media ->
                        val path = media.localPath ?: media.remoteUrl
                        sb.append("![Image ").append(mIdx + 1).append("](").append(path).append(")\n\n")
                    }
                }
                sb.append("---\n\n")
            }

            targetFile.writeText(sb.toString())
            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun exportBackupJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val container = PostBackupContainer(
                authors = postDao.getAllAuthorsSync(),
                posts = postDao.getAllPostsSync(),
                mediaItems = postDao.getAllMediaItemsSync(),
                tags = postDao.getAllTagsSync(),
                postTagCrossRefs = postDao.getAllPostTagCrossRefsSync()
            )
            val json = gson.toJson(container)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "mini_backup_$timestamp.json"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(json)
            Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<PostBackupContainer>() {}.type
            val container: PostBackupContainer? = gson.fromJson(jsonString, type)
            if (container == null) {
                return@withContext Result.failure(Exception("Invalid backup JSON file"))
            }

            container.authors.forEach { postDao.insertAuthor(it) }
            container.posts.forEach { postDao.insertPost(it) }
            container.mediaItems.forEach { item ->
                val restoredPath = item.localPath?.let { path ->
                    val f = File(path)
                    if (f.exists()) path else {
                        val fileName = f.name
                        val backupImage = File(getPublicBackupDir(), "media/$fileName")
                        if (backupImage.exists()) backupImage.absolutePath else path
                    }
                } ?: item.localPath
                postDao.insertMediaItems(listOf(item.copy(localPath = restoredPath)))
            }
            container.tags.forEach { postDao.insertTag(it) }
            container.postTagCrossRefs.forEach { postDao.insertPostTagCrossRef(it) }

            saveBackupData()
            try { PostWidgetProvider.updateAllWidgets(context) } catch (e: Exception) { e.printStackTrace() }
            Result.success(container.posts.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun fetchAndSavePost(url: String, onProgress: (ImportStep) -> Unit): Result<Post> = withContext(Dispatchers.IO) {
        try {
            onProgress(ImportStep.RECEIVING)
            val parsedUrl = XUrlParser.parse(url) ?: return@withContext Result.failure(Exception("Not a valid X post URL"))

            onProgress(ImportStep.FETCHING)
            val response = fxApi.getTweet(parsedUrl.username, parsedUrl.statusId)

            onProgress(ImportStep.EXTRACTING)
            val fxTweet = response.body()?.tweet ?: return@withContext Result.failure(Exception("Couldn't retrieve this post. It may be private, deleted, or unavailable."))

            // Check if tweet contains video/gif media
            val hasVideo = fxTweet.media?.videos?.isNotEmpty() == true ||
                    fxTweet.media?.all?.any { it.type.equals("video", ignoreCase = true) || it.type.equals("gif", ignoreCase = true) } == true

            if (hasVideo) {
                return@withContext Result.failure(VideoNotSupportedException())
            }

            val authorEntity = AuthorEntity(
                handle = fxTweet.author.screenName,
                displayName = fxTweet.author.name,
                avatarRemoteUrl = fxTweet.author.avatarUrl,
                localAvatarPath = null
            )

            val postEntity = PostEntity(
                tweetId = fxTweet.id,
                authorHandle = fxTweet.author.screenName,
                text = fxTweet.text,
                tweetUrl = fxTweet.url,
                originalDateMs = fxTweet.createdTimestamp?.let { it * 1000 } ?: 0L,
                savedAtMs = System.currentTimeMillis()
            )

            val mediaItems = mutableListOf<MediaItemEntity>()
            fxTweet.media?.photos?.forEach { photo ->
                mediaItems.add(
                    MediaItemEntity(
                        postTweetId = fxTweet.id,
                        remoteUrl = photo.url,
                        mediaType = "PHOTO",
                        width = photo.width,
                        height = photo.height
                    )
                )
            }

            onProgress(ImportStep.DOWNLOADING_MEDIA)
            val localAvatarPath = authorEntity.avatarRemoteUrl?.let { downloadFile(it, "avatars/${authorEntity.handle}.jpg", isPrivateOnly = true) }
            val finalAuthor = authorEntity.copy(localAvatarPath = localAvatarPath)

            val finalMediaItems = mediaItems.mapIndexed { index, mediaItemEntity ->
                val localPath = downloadFile(mediaItemEntity.remoteUrl, "media/${postEntity.tweetId}/$index.jpg", isPrivateOnly = false)
                mediaItemEntity.copy(localPath = localPath)
            }

            onProgress(ImportStep.SAVING)
            postDao.insertAuthor(finalAuthor)
            postDao.insertPost(postEntity)
            postDao.insertMediaItems(finalMediaItems)

            saveBackupData()
            try { PostWidgetProvider.updateAllWidgets(context) } catch (e: Exception) { e.printStackTrace() }

            onProgress(ImportStep.COMPLETE)

            val postWithDetails = PostWithDetails(postEntity, finalAuthor, finalMediaItems, emptyList())
            Result.success(postWithDetails.toDomainPost())
        } catch (e: VideoNotSupportedException) {
            onProgress(ImportStep.ERROR)
            Result.failure(e)
        } catch (e: Exception) {
            onProgress(ImportStep.ERROR)
            Result.failure(Exception("Something went wrong while retrieving the post. Please try again."))
        }
    }

    private suspend fun downloadFile(url: String, relativePath: String, isPrivateOnly: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body
                if (body != null) {
                    val bytes = body.bytes()
                    val file = File(context.filesDir, relativePath)
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)

                    // Only save non-private media (e.g. tweet post photos) to public backup dir for reinstall persistence.
                    // Private assets (like author avatars) remain strictly inside app's private filesDir and never appear in galleries.
                    if (!isPrivateOnly) {
                        try {
                            val pubMediaFile = File(getPublicBackupDir(), relativePath)
                            pubMediaFile.parentFile?.mkdirs()
                            pubMediaFile.writeBytes(bytes)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    return@withContext file.absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}

fun PostWithDetails.toDomainPost(): Post {
    return Post(
        tweetId = post.tweetId,
        tweetUrl = post.tweetUrl,
        text = post.text,
        author = author?.let {
            Author(
                handle = it.handle,
                displayName = it.displayName,
                avatarUrl = it.localAvatarPath ?: it.avatarRemoteUrl,
                avatarRemoteUrl = it.avatarRemoteUrl
            )
        } ?: Author(post.authorHandle, post.authorHandle, null, null),
        mediaItems = mediaItems.map {
            MediaItem(
                id = it.id,
                remoteUrl = it.remoteUrl,
                localPath = it.localPath,
                mediaType = it.mediaType,
                width = it.width,
                height = it.height
            )
        },
        originalDateMs = post.originalDateMs,
        savedAtMs = post.savedAtMs,
        collectionId = post.collectionId,
        tags = tags.map { Tag(name = it.name) },
        isFavorite = post.isFavorite,
        note = post.note
    )
}
