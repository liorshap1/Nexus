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
    public static final int NOTIFICATION_ID = 2;
    public static final int FOREGROUND_NOTIFICATION_ID = 1;
    public static final String CHAT_CHANNEL_ID = "chat_channels";
    public static final String CURRENT_CHAT = "current_chat_id";

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

    public static class Firestore {
        public static final String USERS_COLLECTION = "_users";
    }

    public static class FIREBASE_DATABASE {
        public static final String MESSAGES = "messages";
        public static final String TIMESTAMP = "timestamp";
        public static final String CHATS = "chats";
    }

    public static class MessageFields {
        public static final String DELIVER_UID = "deliver_uid";
        public static final String MESSAGE = "message";
        public static final String DELIVER_NAME = "deliver_name";
        public static final String TIMESTAMP = "timestamp";
        public static final String REACTIONS = "reactions";
        public static final String MESSAGE_KEY = "message_key";
    }

    public static class ChatsActivity {
        public static final String USER = "user";
    }

    public static class ChangeSettingActivity {
        public static final String CHANGE_SETTING = "change_setting";

    }
}
