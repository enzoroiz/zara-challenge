package com.zara.challenge.ui.common

import retrofit2.HttpException
import java.io.IOException

private const val GENERIC_ERROR = "Something went wrong"

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> when (code()) {
        404 -> "Unable to fetch"
        429 -> "Too many requests. Please try again later."
        in 500..599 -> "Something went wrong on our side. Please try again later."
        else -> GENERIC_ERROR
    }
    is IOException -> "Check your connection"
    else -> GENERIC_ERROR
}
