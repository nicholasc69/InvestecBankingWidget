package com.example.data.repository

actual fun logDebug(tag: String, message: String) {
    println("[$tag] $message")
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    println("ERROR: [$tag] $message ${throwable?.message ?: ""}")
}
