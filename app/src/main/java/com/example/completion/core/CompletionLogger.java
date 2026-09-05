package com.example.completion.core;

/**
 * Pluggable logger interface for the completion engine.
 */
public interface CompletionLogger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message, Throwable throwable);

    /**
     * Default Android Logcat / standard error logger.
     */
    class AndroidLogger implements CompletionLogger {
        private final String tag;

        public AndroidLogger() {
            this("KotlinCompletion");
        }

        public AndroidLogger(String tag) {
            this.tag = tag;
        }

        @Override
        public void debug(String message) {
            android.util.Log.d(tag, message);
        }

        @Override
        public void info(String message) {
            android.util.Log.i(tag, message);
        }

        @Override
        public void warn(String message) {
            android.util.Log.w(tag, message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            android.util.Log.e(tag, message, throwable);
        }
    }

    /**
     * No-op silent logger.
     */
    class NoOpLogger implements CompletionLogger {
        @Override
        public void debug(String message) {}
        @Override
        public void info(String message) {}
        @Override
        public void warn(String message) {}
        @Override
        public void error(String message, Throwable throwable) {}
    }
}
