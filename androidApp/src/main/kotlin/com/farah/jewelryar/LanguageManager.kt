package com.farah.jewelryar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LanguageManager {
    private val _isArabic = MutableStateFlow(false)
    val isArabic: StateFlow<Boolean> = _isArabic

    fun toggle() {
        _isArabic.value = !_isArabic.value
    }
}
