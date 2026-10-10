package com.example.completion.core

import com.example.diagnostics.AppLogger

interface CompletionLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)

    class AndroidLogger(private val tag: String = "KotlinCompletion") : CompletionLogger {
        override fun debug(message: String) { AppLogger.d(tag, message) }
        override fun info(message: String) { AppLogger.i(tag, message) }
        override fun warn(message: String) { AppLogger.w(tag, message) }
        override fun error(message: String, throwable: Throwable?) { AppLogger.e(tag, message, throwable) }
    }

    class NoOpLogger : CompletionLogger {
        override fun debug(message: String) {}
        override fun info(message: String) {}
        override fun warn(message: String) {}
        override fun error(message: String, throwable: Throwable?) {}
    }
}
