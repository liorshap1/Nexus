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

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

@AndroidEntryPoint
public class FetchUsersService extends LifecycleService implements IFetchUsersService {
    private static final long POLL_INTERVAL_MS = 500;

    private final Queue<String> userQueue = new ConcurrentLinkedQueue<>();
    private final List<User> cachedUsers = new CopyOnWriteArrayList<>();
    private final BehaviorSubject<List<User>> usersSubject = BehaviorSubject.createDefault(Collections.emptyList());

    private final IBinder binder = new LocalBinder();
    @Inject
    FirebaseFirestore firestore;
    private ScheduledExecutorService scheduler;
    @Inject
    AppLogger logger;

    @Override
    public void onCreate() {
        super.onCreate();
        logger.i("FetchUsersService created");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::processQueue, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        List<String> uids = intent.getStringArrayListExtra(Constants.USERS_KEY);
        if (uids != null && !uids.isEmpty()) {
            userQueue.addAll(uids);
            logger.i("Enqueued " + uids.size() + " user(s)");
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduler.shutdownNow();
        logger.i("FetchUsersService destroyed");
    }

    private void processQueue() {
        String uid;
        while ((uid = userQueue.poll()) != null) {
            fetchUserData(uid);
        }
    }

    private void fetchUserData(String uid) {
        firestore.collection(Constants.Firestore.USERS_COLLECTION).document(uid).get().addOnSuccessListener(doc -> {
            String first = doc.getString(Constants.UserFields.FIRST_NAME);
            String last = doc.getString(Constants.UserFields.SECOND_NAME);
            String email = doc.getString(Constants.UserFields.EMAIL);
            String pic = doc.getString(Constants.UserFields.PROFILE_PICTURE);

            if (first == null || last == null || email == null) {
                logger.w("Incomplete data for UID: " + uid);
                return;
            }

            User user = new User(uid, first, last, email, pic);
            cachedUsers.add(user);
            SharedPreferencesUtils.insertData(getApplicationContext(), "user_" + uid, new Gson().toJson(user));
            logger.d("Fetched and cached user: " + uid);

            // emit updated list
            usersSubject.onNext(Collections.unmodifiableList(new ArrayList<>(cachedUsers)));
        }).addOnFailureListener(e -> logger.e("Error fetching UID: " + uid, e));
    }

    /** Expose an Rx Observable that emits the list of users whenever it changes. */
    public Observable<List<User>> getUsersObservable() {
        return usersSubject.hide();
    }

    @Override
    public void enqueueUser(String uid) {
        if (uid != null && !uid.isEmpty())
            userQueue.add(uid);
    }

    @Override
    public List<User> getFetchedUsers() {
        return List.copyOf(cachedUsers);
    }

    @Override
    public User getUser(String uid) {
        for (User u : cachedUsers) {
            if (uid.equals(u.getUid()))
                return u;
        }
        return null;
    }

    public void addToCachedUsers(User user) {
        this.cachedUsers.add(user);
    }

    @Override
    public boolean isUserFetched(String uid) {
        return getUser(uid) != null;
    }

    @Override
    public void addFetchListener(FetchListener listener) {
    }

    @Override
    public void removeFetchListener(FetchListener listener) {
        listener = null;
    }

    @Override
    public void clear() {
        userQueue.clear();
        cachedUsers.clear();
        usersSubject.onNext(Collections.emptyList());
    }

    public class LocalBinder extends Binder {
        public FetchUsersService getService() {
            return FetchUsersService.this;
        }
    }
}
