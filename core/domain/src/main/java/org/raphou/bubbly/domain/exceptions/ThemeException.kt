package org.raphou.bubbly.domain.exceptions

sealed class ThemeException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ThemeNotFound(message: String) : ThemeException(message)
    class ThemeCreationFailed(message: String, cause: Throwable? = null) : ThemeException(message, cause)
    class ThemeRetrievalFailed(message: String, cause: Throwable? = null) : ThemeException(message, cause)
    class DefaultThemesInsertionFailed(message: String, cause: Throwable? = null) : ThemeException(message, cause)
}