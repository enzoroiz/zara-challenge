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
    fun `maps known http codes`() {
        assertEquals("Unable to fetch", http(404).toUserMessage())
        assertEquals("Wait to try again", http(429).toUserMessage())
        assertEquals("Server unavailable, try again later", http(503).toUserMessage())
    }

    @Test
    fun `maps io and unknown errors`() {
        assertEquals("Check your connection", IOException().toUserMessage())
        assertEquals("Something went wrong", http(418).toUserMessage())
        assertEquals("Something went wrong", IllegalStateException("x").toUserMessage())
    }
}
