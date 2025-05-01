/*
 * Copyright 2025 Lior Shaposhnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.nexus.applogger;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

import javax.inject.Inject;

public class AppLoggerImpl implements AppLogger {
    private static final EnumSet<LogLevel> ENABLED_LEVELS = EnumSet.of(LogLevel.INFO, LogLevel.SUCCESS, LogLevel.ERROR, LogLevel.WARNING, LogLevel.DEBUG,
            LogLevel.VERBOSE // Remove
    // in
    // release
    // build
    // if
    // needed
    );

    @Inject
    public AppLoggerImpl() {
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCallerClassName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 4; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!className.contains("AppLogger")) {
                return className.substring(className.lastIndexOf('.') + 1);
            }
        }
        return "UnknownClass";
    }

    @Override
    public void log(LogLevel level, String message) {
        log(level, message, null);
    }

    @Override
    public void log(LogLevel level, String message, Throwable throwable) {
        if (!ENABLED_LEVELS.contains(level))
            return;

        String tag = getCallerClassName();
        String logMsg = "[" + getTimestamp() + "] " + message;

        switch (level) {
            case VERBOSE :
                Log.v(tag, logMsg);
                break;
            case DEBUG :
                Log.d(tag, logMsg);
                break;
            case INFO :
                Log.i(tag, "ℹ️ " + logMsg);
                break;
            case SUCCESS :
                Log.i(tag, "✅ " + logMsg);
                break;
            case WARNING :
                Log.w(tag, "⚠️ " + logMsg);
                break;
            case ERROR :
                Log.e(tag, "❌ " + logMsg, throwable);
                break;
        }
    }

    @Override
    public void v(String message) {
        log(LogLevel.VERBOSE, message);
    }

    @Override
    public void d(String message) {
        log(LogLevel.DEBUG, message);
    }

    @Override
    public void i(String message) {
        log(LogLevel.INFO, message);
    }

    @Override
    public void w(String message) {
        log(LogLevel.WARNING, message);
    }

    @Override
    public void e(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    @Override
    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }
}
