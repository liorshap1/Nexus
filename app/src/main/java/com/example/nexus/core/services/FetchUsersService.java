package com.example.nexus.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

@AndroidEntryPoint
public class FetchUsersService extends Service {
    private final IBinder binder = new LocalBinder();
    private final Gson gson = new Gson();
    private final List<User> fetchedUsersList = new ArrayList<>();
    private final BehaviorSubject<List<User>> usersSubject = BehaviorSubject.createDefault(Collections.emptyList());

    @Inject
    AppLogger logger;

    @Inject
    LocalUserSingleton localUserSingleton;

    @Inject
    FirebaseFirestore firestore;

    @Override
    public void onCreate() {
        super.onCreate();
        logger.success("FetchUsersService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Validate UID before proceeding
        String uid = localUserSingleton.getUid();
        if (uid == null || uid.isEmpty()) {
            logger.i("UID is null or empty! Cannot fetch friends.");
            stopSelf();
            return START_NOT_STICKY;
        }

        logger.success("FetchUsersService started for UID: " + uid);
        fetchConnectedUserFriends(uid);
        return START_STICKY;
    }

    private void fetchConnectedUserFriends(String uid) {
        // Ensure the USERS_COLLECTION constant has no trailing slash
        String collection = Constants.Firestore.USERS_COLLECTION;
        if (collection.endsWith("/")) {
            collection = collection.substring(0, collection.length() - 1);
        }

        firestore.collection(collection)
                .document(uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        logger.e(error.getMessage(), error);
                        return;
                    }

                    fetchedUsersList.clear();

                    if (value != null && value.exists()) {
                        List<String> currentFriendsList = (List<String>) value.get(Constants.UserFields.FRIENDS);
                        if (currentFriendsList != null && !currentFriendsList.isEmpty()) {
                            logger.i("Friends: " + currentFriendsList);
                            for (String friendUid : currentFriendsList) {
                                fetchSingleUser(friendUid);
                            }
                        } else {
                            logger.i("Empty or null friends list");
                            usersSubject.onNext(new ArrayList<>());
                        }
                    } else {
                        logger.w("User document doesn't exist or is null");
                        usersSubject.onNext(new ArrayList<>());
                    }
                });
    }

    private void fetchSingleUser(String uid) {
        if (uid == null || uid.isEmpty()) {
            logger.v("Friend UID is null or empty, skipping fetch.");
            return;
        }

        String cached = SharedPreferencesUtils.getDataByKey(getApplicationContext(), "user_" + uid);
        if (cached == null) {
            firestore.collection(Constants.Firestore.USERS_COLLECTION)
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot docSnap = task.getResult();
                            if (docSnap != null && docSnap.exists()) {
                                String first = docSnap.getString(Constants.UserFields.FIRST_NAME);
                                String last = docSnap.getString(Constants.UserFields.SECOND_NAME);
                                String email = docSnap.getString(Constants.UserFields.EMAIL);
                                String pic = docSnap.getString(Constants.UserFields.PROFILE_PICTURE);

                                if (first != null && last != null && email != null) {
                                    User user = new User(uid, first, last, email, pic);
                                    fetchedUsersList.add(user);
                                    SharedPreferencesUtils.insertData(getApplicationContext(), "user_" + uid, gson.toJson(user));
                                    logger.success("Fetched and cached user: " + uid);
                                    usersSubject.onNext(new ArrayList<>(fetchedUsersList));
                                } else {
                                    logger.w("Incomplete data for UID: " + uid);
                                }
                            } else {
                                logger.w("User document doesn't exist for UID: " + uid);
                            }
                        } else {
                            logger.e(task.getException().getMessage(), task.getException());
                        }
                    });
        } else {
            User user = gson.fromJson(cached, User.class);
            fetchedUsersList.add(user);
            usersSubject.onNext(new ArrayList<>(fetchedUsersList));
            logger.success("Retrieved user from SharedPreferences: " + uid);
        }
    }

    public BehaviorSubject<List<User>> observeCurrentUsers() {
        return usersSubject;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public FetchUsersService getService() {
            return FetchUsersService.this;
        }
    }
}
