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

import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FetchUsersService extends Service implements IFetchUsersService {

    private final Queue<String> userIdQueue = new ConcurrentLinkedQueue<>();
    private final List<User> fetchedUsers = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final IBinder binder = new FetchUsersBinder();
    @Inject
    AppLogger logger;
    @Inject
    FirebaseFirestore firestore;
    private volatile boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        isServiceRunning = true;
        startFetchingLoop();
        logger.i("FetchUsersService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<String> userIds = intent.getStringArrayListExtra("user_ids");
        if (userIds != null) {
            userIds.forEach(this::enqueueUser);
            logger.i("Received " + userIds.size() + " users to enqueue");
        } else {
            logger.w("No user IDs received");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        logger.success("FetchUsersService bound");
        return binder;
    }

    @Override
    public void enqueueUser(String uid) {
        if (uid != null && !uid.isEmpty()) {
            userIdQueue.add(uid);
        } else {
            logger.w("Attempted to enqueue null or empty UID");
        }
    }

    @Override
    public List<User> getFetchedUsers() {
        return fetchedUsers;
    }

    private void startFetchingLoop() {
        executorService.execute(
                () -> {
                    while (isServiceRunning) {
                        String userUid = userIdQueue.poll();

                        if (userUid != null) {
                            fetchSingleUser(userUid);
                        } else {
                            try {
                                Thread.sleep(500); // Wait a bit before polling again
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                logger.e("Fetching thread interrupted", e);
                            }
                        }
                    }
                });
    }

    private void fetchSingleUser(String uid) {
        firestore
                .collection(Constants.Firestore.USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(
                        docSnap -> {
                            if (docSnap.exists()) {
                                User user = docSnap.toObject(User.class);
                                if (user != null) {
                                    fetchedUsers.add(user);
                                    logger.i("Fetched user: " + uid);
                                } else {
                                    logger.w("User document exists but could not convert: " + uid);
                                }
                            } else {
                                logger.w("User document does not exist: " + uid);
                            }
                        })
                .addOnFailureListener(e -> logger.e("Failed to fetch user " + uid, e));
    }

    public void stopService() {
        isServiceRunning = false;
        executorService.shutdownNow();
        stopSelf();
        logger.i("FetchUsersService stopped");
    }

    @Override
    public void onDestroy() {
        stopService(); // Ensures executor stops and flags are cleared
        super.onDestroy();
    }

    public class FetchUsersBinder extends Binder {
        public FetchUsersService getService() {
            return FetchUsersService.this;
        }
    }
}
