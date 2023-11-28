package com.appboosty.premiumhelper.util

sealed class PHResult<out T> {

    companion object {

        fun <T> of(block: () -> T): PHResult<T> = try {
            Success(block())
        } catch (exception: Exception) {
            Failure(exception)
        }

        suspend fun <T> suspendOf(block: suspend () -> T): PHResult<T> = try {
            Success(block())
        } catch (exception: Exception) {
            Failure(exception)
        }

    }

    data class Success<T>(val value: T) : PHResult<T>()
    data class Failure(val error: Exception?): PHResult<Nothing>()
}

val <T> PHResult<T>.isSuccess: Boolean
    get() {
        return this is PHResult.Success
    }

val <T> PHResult<T>.successValue: T?
    get() {
        return when (this) {
            is PHResult.Success -> this.value
            else -> null
        }
    }

val <T> PHResult<T>.error: Exception?
    get() {
        return when (this) {
            is PHResult.Failure -> this.error
            else -> null
        }
    }

fun <T> PHResult<T>.then(block: () -> T) = when(this) {
    is PHResult.Success -> PHResult.of { block() }
    else -> this
}

fun <T, R> PHResult<T>.bind(block: (T) -> PHResult<R>): PHResult<R> = when(this) {
    is PHResult.Success -> block(value)
    is PHResult.Failure -> this
}

fun <T> PHResult<T>.onSuccess(action: (T) -> Unit): PHResult<T> = when (this) {
    is PHResult.Success -> apply { action(value) }
    is PHResult.Failure -> this
}

fun <T> PHResult<T>.onError(action: (PHResult.Failure) -> Unit): PHResult<T> = when (this) {
    is PHResult.Success -> this
    is PHResult.Failure -> apply { action(this) }
}
