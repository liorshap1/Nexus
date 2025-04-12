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
package com.example.nexus;

public class Constants {
    public static final int REQUEST_CODE = 1;

    public static class UserFields {
        public static final String FIRST_NAME = "first_name";
        public static final String SECOND_NAME = "second_name";
        public static final String EMAIL = "email";
        public static final String PROFILE_PICTURE = "photo";
        public static final String UID = "uid";
        public static final String FRIENDS = "friends";
        public static final String PENDING_REQUESTS = "pending_requests";
        public static final String PASSWORD = "password";
    }

    // Firebase Authentication Errors
    public static class AuthErrors {
        public static final String INVALID_EMAIL = "ERROR_INVALID_EMAIL";
        public static final String USER_NOT_FOUND = "ERROR_USER_NOT_FOUND";
        public static final String WRONG_PASSWORD = "ERROR_WRONG_PASSWORD";
        public static final String EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE";
        public static final String USER_DISABLED = "ERROR_USER_DISABLED";
        public static final String WEAK_PASSWORD = "ERROR_WEAK_PASSWORD";
    }

    // Firebase Realtime Database Errors
    public static class RealtimeDatabaseErrors {
        public static final String PERMISSION_DENIED = "ERROR_PERMISSION_DENIED";
        public static final String NETWORK_ERROR = "ERROR_NETWORK";
    }

    // Firebase Firestore Errors
    public static class FirestoreErrors {
        public static final String DOCUMENT_NOT_FOUND = "ERROR_DOCUMENT_NOT_FOUND";
        public static final String ALREADY_EXISTS = "ERROR_ALREADY_EXISTS";
        public static final String UNAVAILABLE = "ERROR_UNAVAILABLE";
        public static final String DEADLINE_EXCEEDED = "ERROR_DEADLINE_EXCEEDED";
        public static final String PERMISSION_DENIED = "ERROR_PERMISSION_DENIED";
    }

    public static class Firestore {
        public static final String USERS_COLLECTION = "_users";
    }

    public static class FetchUsersServiceConstants {
        public static final String USERS_KEY = "users";
    }
}
