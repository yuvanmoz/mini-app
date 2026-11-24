package com.mini.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mini.app.data.local.entities.*
import com.mini.app.data.repository.PostBackupContainer
import com.mini.app.util.XUrlParser
import org.junit.Assert.*
import org.junit.Test

class DataRetentionTest {

    private val gson = Gson()

    @Test
    fun testLegacyBackupRestorationRetainsAllOldFields() {
        // Legacy JSON format from previous versions (does NOT contain isFavorite or note)
        val legacyJson = """
            {
              "authors": [
                {
                  "handle": "jack",
                  "displayName": "Jack Dorsey",
                  "avatarRemoteUrl": "https://pbs.twimg.com/avatar.jpg",
                  "localAvatarPath": "/data/user/0/com.mini.app/files/avatars/jack.jpg"
                }
              ],
              "posts": [
                {
                  "tweetId": "123456789",
                  "authorHandle": "jack",
                  "text": "just setting up my twttr",
                  "tweetUrl": "https://x.com/jack/status/123456789",
                  "originalDateMs": 1142974200000,
                  "savedAtMs": 1723500000000,
                  "collectionId": "uncategorized"
                }
              ],
              "mediaItems": [
                {
                  "id": 1,
                  "postTweetId": "123456789",
                  "remoteUrl": "https://pbs.twimg.com/media.jpg",
                  "localPath": "/data/user/0/com.mini.app/files/media/123456789/0.jpg",
                  "mediaType": "PHOTO",
                  "width": 1080,
                  "height": 720
                }
              ],
              "tags": [
                {
                  "name": "tech",
                  "createdAtMs": 1723500000000
                }
              ],
              "postTagCrossRefs": [
                {
                  "tweetId": "123456789",
                  "tagName": "tech"
                }
              ]
            }
        """.trimIndent()

        val type = object : TypeToken<PostBackupContainer>() {}.type
        val container: PostBackupContainer = gson.fromJson(legacyJson, type)

        assertNotNull(container)
        assertEquals(1, container.authors.size)
        assertEquals(1, container.posts.size)
        assertEquals(1, container.mediaItems.size)
        assertEquals(1, container.tags.size)
        assertEquals(1, container.postTagCrossRefs.size)

        val post = container.posts[0]
        assertEquals("123456789", post.tweetId)
        assertEquals("jack", post.authorHandle)
        assertEquals("just setting up my twttr", post.text)
        assertEquals("https://x.com/jack/status/123456789", post.tweetUrl)
        assertEquals(1142974200000L, post.originalDateMs)
        assertEquals("uncategorized", post.collectionId)

        // Verifying default values for upgraded features are safely initialized without data corruption
        assertFalse("Old posts should default isFavorite to false", post.isFavorite)
        assertNull("Old posts should default note to null", post.note)
    }

    @Test
    fun testUpgradedBackupPreservesNewFeatures() {
        val container = PostBackupContainer(
            posts = listOf(
                PostEntity(
                    tweetId = "987654321",
                    authorHandle = "design",
                    text = "Minimalism is about intentionality.",
                    tweetUrl = "https://x.com/design/status/987654321",
                    originalDateMs = 1700000000000L,
                    savedAtMs = 1720000000000L,
                    collectionId = "uncategorized",
                    isFavorite = true,
                    note = "Great design principle to remember"
                )
            )
        )

        val json = gson.toJson(container)
        val type = object : TypeToken<PostBackupContainer>() {}.type
        val restored: PostBackupContainer = gson.fromJson(json, type)

        assertEquals(1, restored.posts.size)
        val post = restored.posts[0]
        assertEquals("987654321", post.tweetId)
        assertTrue(post.isFavorite)
        assertEquals("Great design principle to remember", post.note)
    }

    @Test
    fun testXUrlParserExtractsUrlsCorrectly() {
        val parsed = XUrlParser.parse("https://x.com/jack/status/20")
        assertNotNull(parsed)
        assertEquals("jack", parsed?.username)
        assertEquals("20", parsed?.statusId)

        val parsedTwitter = XUrlParser.parse("https://twitter.com/elonmusk/status/1645000000000000000")
        assertNotNull(parsedTwitter)
        assertEquals("elonmusk", parsedTwitter?.username)
        assertEquals("1645000000000000000", parsedTwitter?.statusId)
    }
}
