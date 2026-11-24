package com.mini.app.data.local

import androidx.room.*
import com.mini.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class PostWithDetails(
    @Embedded val post: PostEntity,
    @Relation(parentColumn = "authorHandle", entityColumn = "handle")
    val author: AuthorEntity?,
    @Relation(parentColumn = "tweetId", entityColumn = "postTweetId")
    val mediaItems: List<MediaItemEntity>,
    @Relation(
        parentColumn = "tweetId",
        entityColumn = "name",
        associateBy = Junction(
            value = PostTagCrossRef::class,
            parentColumn = "tweetId",
            entityColumn = "tagName"
        )
    )
    val tags: List<TagEntity>
)

data class TagWithCount(
    val name: String,
    val postCount: Int,
    val previewImageUrl: String? = null
)

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Query("SELECT * FROM posts ORDER BY savedAtMs DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY savedAtMs DESC")
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>>

    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM posts p 
        LEFT JOIN post_tag_cross_ref ref ON p.tweetId = ref.tweetId 
        WHERE p.text LIKE :query OR p.authorHandle LIKE :query OR ref.tagName LIKE :query OR (p.note IS NOT NULL AND p.note LIKE :query)
        ORDER BY p.savedAtMs DESC
    """)
    fun searchPosts(query: String): Flow<List<PostWithDetails>>

    @Transaction
    @Query("SELECT * FROM posts WHERE isFavorite = 1 ORDER BY savedAtMs DESC")
    fun getFavoritePostsWithDetails(): Flow<List<PostWithDetails>>

    @Query("SELECT COUNT(*) FROM posts WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE tweetId = :tweetId")
    suspend fun updateFavorite(tweetId: String, isFavorite: Boolean)

    @Query("UPDATE posts SET note = :note WHERE tweetId = :tweetId")
    suspend fun updateNote(tweetId: String, note: String?)

    @Transaction
    @Query("""
        SELECT p.* FROM posts p 
        INNER JOIN post_tag_cross_ref ref ON p.tweetId = ref.tweetId 
        WHERE ref.tagName = :tagName 
        ORDER BY p.savedAtMs DESC
    """)
    fun getPostsForTag(tagName: String): Flow<List<PostWithDetails>>

    @Query("SELECT * FROM authors WHERE handle = :handle")
    suspend fun getAuthor(handle: String): AuthorEntity?

    @Query("SELECT * FROM media_items WHERE postTweetId = :tweetId")
    fun getMediaForPost(tweetId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM posts WHERE tweetId = :tweetId)")
    suspend fun existsById(tweetId: String): Boolean

    @Transaction
    @Query("SELECT * FROM posts WHERE tweetId = :tweetId LIMIT 1")
    suspend fun getPostWithDetailsById(tweetId: String): PostWithDetails?

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("DELETE FROM media_items WHERE postTweetId = :tweetId")
    suspend fun deleteMediaForPost(tweetId: String)

    @Query("DELETE FROM post_tag_cross_ref WHERE tweetId = :tweetId")
    suspend fun deleteTagsForPost(tweetId: String)

    @Query("SELECT COUNT(*) FROM posts")
    fun getPostCount(): Flow<Int>

    @Update
    suspend fun updateAuthor(author: AuthorEntity)

    @Update
    suspend fun updateMediaItem(item: MediaItemEntity)

    // Tag Operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteTag(name: String)

    @Query("DELETE FROM post_tag_cross_ref WHERE tagName = :name")
    suspend fun deleteTagCrossRefs(name: String)

    @Query("""
        SELECT t.name AS name, 
               COUNT(DISTINCT ref.tweetId) AS postCount,
               (SELECT m.localPath FROM media_items m 
                INNER JOIN post_tag_cross_ref r2 ON m.postTweetId = r2.tweetId 
                WHERE r2.tagName = t.name 
                ORDER BY m.id DESC LIMIT 1) AS previewImageUrl
        FROM tags t 
        LEFT JOIN post_tag_cross_ref ref ON t.name = ref.tagName 
        GROUP BY t.name 
        ORDER BY t.name ASC
    """)
    fun getAllTagsWithCount(): Flow<List<TagWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostTagCrossRef(crossRef: PostTagCrossRef)

    @Query("DELETE FROM post_tag_cross_ref WHERE tweetId = :tweetId AND tagName = :tagName")
    suspend fun deletePostTagCrossRef(tweetId: String, tagName: String)

    @Query("UPDATE OR IGNORE post_tag_cross_ref SET tagName = :newName WHERE tagName = :oldName")
    suspend fun updateTagCrossRefs(oldName: String, newName: String)

    @Transaction
    suspend fun renameTag(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return
        insertTag(TagEntity(name = newName))
        updateTagCrossRefs(oldName, newName)
        deleteTag(oldName)
        deleteTagCrossRefs(oldName)
    }

    @Transaction
    suspend fun setPostTags(tweetId: String, tagNames: List<String>) {
        deleteTagsForPost(tweetId)
        tagNames.forEach { name ->
            insertTag(TagEntity(name = name))
            insertPostTagCrossRef(PostTagCrossRef(tweetId = tweetId, tagName = name))
        }
    }

    // Backup & Restore helper queries
    @Query("SELECT * FROM authors")
    suspend fun getAllAuthorsSync(): List<AuthorEntity>

    @Query("SELECT * FROM posts")
    suspend fun getAllPostsSync(): List<PostEntity>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItemsSync(): List<MediaItemEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM post_tag_cross_ref")
    suspend fun getAllPostTagCrossRefsSync(): List<PostTagCrossRef>

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCountSync(): Int
}
