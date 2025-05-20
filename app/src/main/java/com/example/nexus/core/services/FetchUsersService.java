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
package com.example.nexus.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

@AndroidEntryPoint
public class FetchUsersService extends Service {
    private final IBinder serviceBinder = new LocalBinder();
    private final Gson jsonParser = new Gson();
    private final List<User> cachedUsers = new ArrayList<>();
    private final BehaviorSubject<List<User>> usersStream = BehaviorSubject.createDefault(Collections.emptyList());

    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    FirebaseFirestore firebaseFirestore;

    @Override
    public void onCreate() {
        super.onCreate();

        logger.success("Created successfully");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String localUserUid = localUserSingleton.getUid();
        if (!localUserUid.isEmpty()) {
            startFetchingUsers();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startFetchingUsers() {
        firebaseFirestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUserSingleton.getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            logger.e(error.getMessage(), error.getCause());
                        }
                        cachedUsers.clear();

                        if (value != null && value.exists()) {
                            List<String> currentFriendsList = (List<String>) value.get(Constants.UserFields.FRIENDS);
                            if (currentFriendsList != null && !currentFriendsList.isEmpty()) {
                                for (String friendUid : currentFriendsList) {
                                    fetchSingleUser(friendUid);
                                }
                            } else {
                                logger.i("Empty or null friends list");
                                usersStream.onNext(new ArrayList<>());
                            }
                        } else {
                            logger.w("User document doesn't exist or is null");
                            usersStream.onNext(new ArrayList<>());
                        }
                    }
                });
    }

    private void fetchSingleUser(String userUid) {
        String cached = SharedPreferencesUtils.getDataByKey(getApplicationContext(), "user_" + userUid);
        if (cached == null) {
            firebaseFirestore.collection(Constants.Firestore.USERS_COLLECTION).document(userUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot docSnap = task.getResult();
                    if (docSnap != null && docSnap.exists()) {
                        String first = docSnap.getString(Constants.UserFields.FIRST_NAME);
                        String last = docSnap.getString(Constants.UserFields.SECOND_NAME);
                        String email = docSnap.getString(Constants.UserFields.EMAIL);
                        String pic = docSnap.getString(Constants.UserFields.PROFILE_PICTURE);

                        if (first != null && last != null && email != null) {
                            User user = new User(userUid, first, last, email, pic);
                            cachedUsers.add(user);
                            SharedPreferencesUtils.insertData(getApplicationContext(), "user_" + userUid, jsonParser.toJson(user));
                            logger.success("Fetched and cached user: " + userUid);
                            usersStream.onNext(new ArrayList<>(cachedUsers));
                        } else {
                            logger.w("Incomplete data for UID: " + userUid);
                        }
                    } else {
                        logger.w("User document doesn't exist for UID: " + userUid);
                    }
                } else {
                    logger.e(Objects.requireNonNull(task.getException()).getMessage(), task.getException());
                }
            }).addOnFailureListener(error -> {
                logger.e(error.getMessage(), error.getCause());
            });
        } else {
            User user = jsonParser.fromJson(cached, User.class);
            cachedUsers.add(user);
            usersStream.onNext(new ArrayList<>(cachedUsers));
            logger.success("Retrieved user from SharedPreferences: " + userUid);
        }
    }

    @NonNull
    public BehaviorSubject<List<User>> observeCurrentUsers() {
        return usersStream;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
    public class LocalBinder extends Binder {
        @NonNull
        public FetchUsersService getService() {
            return FetchUsersService.this;
        }
    }
}