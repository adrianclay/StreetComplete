package de.westnordost.streetcomplete.data.osmnotes

import de.westnordost.osmapi.user.UserApi
import de.westnordost.osmapi.user.UserInfo
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.on
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvatarsDownloaderTest {
    private val mockEngine = MockEngine { request -> when (request.url.encodedPath) {
        "/NotFound" -> respondError(HttpStatusCode.NotFound)
        "/ConnectionError" -> throw IOException("Cannot connect")
        else -> respondOk("Image Content")
    } }
    private val fileSystem = FakeFileSystem()
    private val tempFolder = "/avatars/".toPath()
    private val userApi: UserApi = mock()
    private val downloader = AvatarsDownloader(HttpClient(mockEngine), userApi, fileSystem, tempFolder)
    private val userInfo = UserInfo(100, "Map Enthusiast 530")

    @BeforeTest fun setUp() {
        userInfo.profileImageUrl = "http://example.com/BigImage.png"
        on(userApi.get(userInfo.id)).thenReturn(userInfo)
    }

    @Test
    fun `download generates the tempFolder`() = runBlocking {
        assertFalse(fileSystem.exists(tempFolder))

        downloader.download(listOf())

        assertTrue(fileSystem.exists(tempFolder))
    }

    @Test
    fun `download makes GET request to profileImageUrl`() = runBlocking {
        downloader.download(listOf(userInfo.id))

        assertEquals(1, mockEngine.requestHistory.size)
        assertEquals(userInfo.profileImageUrl, mockEngine.requestHistory[0].url.toString())
        assertEquals(HttpMethod.Get, mockEngine.requestHistory[0].method)
    }

    @Test
    fun `download copies HTTP response from profileImageUrl into tempFolder`() = runBlocking {
        downloader.download(listOf(userInfo.id))

        assertEquals("Image Content", fileSystem.read(tempFolder.resolve("100")) { readUtf8() })
    }

    @Test
    fun `download does not throw exception on HTTP NotFound`() = runBlocking {
        userInfo.profileImageUrl = "http://example.com/NotFound"

        downloader.download(listOf(userInfo.id))

        assertEquals(404, mockEngine.responseHistory[0].statusCode.value)
    }

    @Test
    fun `download does not throw exception on networking error`() = runBlocking {
        userInfo.profileImageUrl = "http://example.com/ConnectionError"

        downloader.download(listOf(userInfo.id))

        assertEquals(0, mockEngine.responseHistory.size)
    }

    @Test
    fun `download does not make HTTP request if profileImageUrl is NULL`() = runBlocking {
        userInfo.profileImageUrl = null

        downloader.download(listOf(userInfo.id))

        assertEquals(0, mockEngine.requestHistory.size)
    }

    @Test
    fun `cachedProfileImagePath returns null when image not downloaded`() = runBlocking {
        assertNull(downloader.cachedProfileImagePath(100))
    }

    @Test
    fun `cachedProfileImagePath returns path when image downloaded`() = runBlocking {
        downloader.download(listOf(userInfo.id))

        assertEquals("/avatars/${userInfo.id}", downloader.cachedProfileImagePath(userInfo.id).toString())
    }
}
