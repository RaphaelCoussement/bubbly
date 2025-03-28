package org.raphou.bubbly.domain.exceptions

sealed class WordException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class WordNotFound(message: String) : WordException(message)
    class WordInitializationFailed(message: String, cause: Throwable? = null) : WordException(message, cause)
    class WordParsingFailed(message: String, cause: Throwable? = null) : WordException(message, cause)
    class WordInsertionFailed(message: String, cause: Throwable? = null) : WordException(message, cause)
    class WordRetrievalFailed(message: String, cause: Throwable? = null) : WordException(message, cause)
    class WordLimitExceeded(message: String, cause: Throwable? = null) : WordException(message, cause)
}