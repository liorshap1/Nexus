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

public abstract class BaseUser {
    @NonNull
    private String uid = "";
    @NonNull
    private String firstName = "";
    @NonNull
    private String secondName = "";
    @NonNull
    private String email = "";
    private String profilePicture;

    public BaseUser() {
        // Required for serialization and frameworks like Hilt/Firebase
    }

    public BaseUser(@NonNull String uid, @NonNull String firstName, @NonNull String secondName, @NonNull String email, String profilePicture) {
        this.uid = uid;
        this.firstName = firstName;
        this.secondName = secondName;
        this.email = email;
        this.profilePicture = profilePicture;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NonNull String firstName) {
        this.firstName = firstName;
    }

    @NonNull
    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(@NonNull String secondName) {
        this.secondName = secondName;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getFullName() {
        return this.firstName + " " + this.secondName;
    }

    @NonNull
    @Override
    public String toString() {
        return "BaseUser{" + "uid='" + uid + '\'' + ", firstName='" + firstName + '\'' + ", secondName='" + secondName + '\'' + ", email='" + email + '\''
                + ", profilePicture='" + profilePicture + '\'' + '}';
    }
}
