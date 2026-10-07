package com.zara.challenge.ui.common

import com.zara.challenge.ui.common.UiText
import com.zara.challenge.R
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErrorMapperTest {
    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `maps not found`() = assertEquals(UiText.Res(R.string.error_not_found), http(404).toUserMessage())

    @Test
    fun `maps rate limiting`() =
        assertEquals(UiText.Res(R.string.error_too_many_requests), http(429).toUserMessage())

    @Test
    fun `maps every server error`() {
        listOf(500, 502, 503, 599).forEach {
            assertEquals(
                UiText.Res(R.string.error_server),
                http(it).toUserMessage(),
            )
        }
    }

    @Test
    fun `maps unlisted http codes to the generic message`() {
        listOf(400, 401, 418, 499).forEach {
            assertEquals(UiText.Res(R.string.error_generic), http(it).toUserMessage())
        }
    }

    @Test
    fun `maps io failures to a connectivity message`() =
        assertEquals(UiText.Res(R.string.error_connection), IOException().toUserMessage())

    @Test
    fun `maps anything else to the generic message`() =
        assertEquals(UiText.Res(R.string.error_generic), IllegalStateException("x").toUserMessage())
}
