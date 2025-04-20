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
package com.example.nexus.core;

import androidx.annotation.NonNull;

public class User extends BaseUser {
    /**
     * Constructs a new BaseUser.
     *
     * @param uid
     *            The user's unique ID.
     * @param firstName
     *            The user's first name.
     * @param secondName
     *            The user's second name.
     * @param email
     *            The user's email address.
     * @param profilePicture
     *            The URL or path of the user's profile picture.
     */
    public User(@NonNull String uid, @NonNull String firstName, @NonNull String secondName, @NonNull String email, String profilePicture) {
        super(uid, firstName, secondName, email, profilePicture);
    }
}
