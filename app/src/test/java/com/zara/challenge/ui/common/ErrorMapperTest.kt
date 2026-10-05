package com.zara.challenge.ui.common

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErrorMapperTest {
    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `maps not found`() = assertEquals("Unable to fetch", http(404).toUserMessage())

    @Test
    fun `maps rate limiting`() =
        assertEquals("Too many requests. Please try again later.", http(429).toUserMessage())

    @Test
    fun `maps every server error`() {
        listOf(500, 502, 503, 599).forEach {
            assertEquals(
                "Something went wrong on our side. Please try again later.",
                http(it).toUserMessage(),
            )
        }
    }

    @Test
    fun `maps unlisted http codes to the generic message`() {
        listOf(400, 401, 418, 499).forEach {
            assertEquals("Something went wrong", http(it).toUserMessage())
        }
    }

    @Test
    fun `maps io failures to a connectivity message`() =
        assertEquals("Check your connection", IOException().toUserMessage())

    @Test
    fun `maps anything else to the generic message`() =
        assertEquals("Something went wrong", IllegalStateException("x").toUserMessage())
}
