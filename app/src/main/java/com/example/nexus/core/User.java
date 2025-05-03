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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class User extends BaseUser implements Parcelable {

    public static final Creator<User> CREATOR = new Creator<User>() {
        @NonNull
        @Override
        public User createFromParcel(@NonNull Parcel in) {
            return new User(in);
        }

        @NonNull
        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public User(@NonNull String uid, @NonNull String firstName, @NonNull String secondName, @NonNull String email, String profilePicture) {
        super(uid, firstName, secondName, email, profilePicture);
    }

    protected User(@NonNull Parcel in) {
        super(in.readString(), // uid
                in.readString(), // firstName
                in.readString(), // secondName
                in.readString(), // email
                in.readString() // profilePicture
        );
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getUid());
        dest.writeString(getFirstName());
        dest.writeString(getSecondName());
        dest.writeString(getEmail());
        dest.writeString(getProfilePicture());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
