package com.example.nexus.core;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class User extends BaseUser implements Parcelable {

    public User(@NonNull String uid, @NonNull String firstName, @NonNull String secondName, @NonNull String email, String profilePicture) {
        super(uid, firstName, secondName, email, profilePicture);
    }

    protected User(Parcel in) {
        super(
                in.readString(), // uid
                in.readString(), // firstName
                in.readString(), // secondName
                in.readString(), // email
                in.readString()  // profilePicture
        );
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
