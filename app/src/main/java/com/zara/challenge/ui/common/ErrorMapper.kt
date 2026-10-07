package com.zara.challenge.ui.common

import com.zara.challenge.R
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toUserMessage(): UiText = UiText.Res(
    when (this) {
        is HttpException -> when (code()) {
            404 -> R.string.error_not_found
            429 -> R.string.error_too_many_requests
            in 500..599 -> R.string.error_server
            else -> R.string.error_generic
        }
        is IOException -> R.string.error_connection
        else -> R.string.error_generic
    },
)
