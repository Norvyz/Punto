package com.punto.app.core.error

import com.punto.app.core.common.AppResult

/**
 * Centralized error model. Extend with specific error types as features are implemented.
 */
sealed interface AppError {
    val message: String?
    val cause: Throwable?

    data class Database(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError

    data class Storage(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError

    data class Location(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError

    data class Permission(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError

    data class Unknown(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError
}

suspend inline fun <T> runAppCatching(
    crossinline mapError: (Throwable) -> AppError = { AppError.Unknown(it.message, it) },
    crossinline block: suspend () -> T,
): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Error(mapError(t))
}
