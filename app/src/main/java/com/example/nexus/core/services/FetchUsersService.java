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
import androidx.annotation.Nullable;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

@AndroidEntryPoint
public class FetchUsersService extends Service implements IFetchUsersService {
  private final Queue<String> userIdQueue = new ConcurrentLinkedQueue<>();
  private final List<User> fetchedUsers = new CopyOnWriteArrayList<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final IBinder binder = new FetchUsersBinder();
  private boolean isServiceRunning = false;
  @Inject FirebaseFirestore firebaseFirestore;
  @Inject AppLogger logger;

  @Override
  public void onCreate() {
    super.onCreate();
    logger.i("Service started");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    List<String> userUidsList = intent.getStringArrayListExtra(Constants.USERS_KEY);
    if (userUidsList != null && !userUidsList.isEmpty()) {
      userUidsList.forEach(this::enqueueUser);
      logger.i("Received users to enqueue");
    }

    isServiceRunning = true;
    return START_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void enqueueUser(String uid) {
    if (uid != null && !uid.isEmpty()) {
      userIdQueue.add(uid);
    } else {
      logger.d("enqueueUser::empty_uid");
    }
  }

  @Override
  public void fetchUser(String uid) {
    firebaseFirestore
        .collection(Constants.Firestore.USERS_COLLECTION)
        .document(uid)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              String firstName = documentSnapshot.getString(Constants.UserFields.FIRST_NAME);
              String secondName = documentSnapshot.getString(Constants.UserFields.SECOND_NAME);
              String email = documentSnapshot.getString(Constants.UserFields.EMAIL);
              String profilePicture =
                  documentSnapshot.getString(Constants.UserFields.PROFILE_PICTURE);
              if (firstName == null || secondName == null || email == null) {
                logger.w("Missing user fields for UID: " + uid);
                return;
              }

              User user = new User(uid, firstName, secondName, email, profilePicture);
              Gson json = new Gson();

              String userToJson = json.toJson(user);
              SharedPreferencesUtils.insertData(getApplicationContext(), "user_" + uid, userToJson);
              fetchedUsers.add(user);
            })
        .addOnFailureListener(e -> logger.e("Failed to fetch user: " + e.getMessage(), e));
  }

  @Override
  public void fetchUsers() {
    while (!userIdQueue.isEmpty() && isServiceRunning) {
      String userUid = userIdQueue.poll();
      if (userUid != null && !userUid.isEmpty()) {
        fetchUser(userUid);
      }
    }
  }

  @Override
  public List<User> getFetchedUsers() {
    return fetchedUsers;
  }

  @Override
  public User getUser(String uid) {
    return null;
  }

  @Override
  public boolean isUserFetched(String uid) {
    Set<String> uidSet = new HashSet<>();
    for (User user : fetchedUsers) {
      uidSet.add(user.getUid());
    }

    return uidSet.contains(uid);
  }

  @Override
  public void addFetchListener(FetchListener listener) {
    executorService.execute(
        () -> {
          while (isServiceRunning && !userIdQueue.isEmpty()) {
            fetchUsers();
          }

          if (userIdQueue.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              logger.e(e.getMessage(), e.getCause());
            }
          }
        });
  }

  @Override
  public void clear() {
    userIdQueue.clear();
    ;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  public class FetchUsersBinder extends Binder {
    public FetchUsersService getService() {
      return FetchUsersService.this;
    }
  }
}
