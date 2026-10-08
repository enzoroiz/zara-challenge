package com.zara.challenge.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Res(@StringRes val id: Int) : UiText

    fun asString(context: Context): String = when (this) {
        is Plain -> value
        is Res -> context.getString(id)
    }

    @Composable
    fun asString(): String = when (this) {
        is Plain -> value
        is Res -> stringResource(id)
    }
}
