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
package com.example.nexus.home;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nexus.Constants;
import com.example.nexus.adapters.PendingRequestsAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.databinding.ActivityFriendsBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FriendsActivity extends AppCompatActivity {
    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    FirebaseFirestore firestore;
    @Inject
    FirebaseStorage firebaseStorage;
    private List<User> pendingUsersList = new ArrayList<>();
    private ActivityFriendsBinding binding;
    private FetchUsersService fetchUsersService;
    private PendingRequestsAdapter pendingRequestsAdapter;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FetchUsersService.LocalBinder binder = (FetchUsersService.LocalBinder) iBinder;
            fetchUsersService = binder.getService();

            pendingRequestsAdapter = new PendingRequestsAdapter(pendingUsersList, localUserSingleton, firestore, logger, firebaseStorage, FriendsActivity.this);
            binding.pendingRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(FriendsActivity.this));
            binding.pendingRequestsRecyclerView.setAdapter(pendingRequestsAdapter);

            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            fetchUsersService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, FetchUsersService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        DocumentReference docRef = firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUserSingleton.getUid());
        docRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                logger.e(error.getMessage(), error.getCause());
                return;
            }

            if (value != null && value.exists()) {
                ArrayList<String> currentPendingRequestsList = (ArrayList<String>) value.get(Constants.UserFields.PENDING_REQUESTS);
                if (currentPendingRequestsList != null) {
                    pendingUsersList.clear();

                    if (currentPendingRequestsList.isEmpty()) {
                        pendingRequestsAdapter.notifyDataSetChanged();
                        return;
                    }

                    final int total = currentPendingRequestsList.size();
                    final int[] completed = {0};

                    for (String uid : currentPendingRequestsList) {
                        fetchUser(uid, () -> {
                            completed[0]++;
                            if (completed[0] == total) {
                                logger.i("All users fetched. Notifying adapter.");
                                pendingRequestsAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });

        binding.backButton.setOnClickListener(view -> finish());
    }

    private void fetchUser(String uid, Runnable onComplete) {
        firestore.collection(Constants.Firestore.USERS_COLLECTION).document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot docSnap = task.getResult();
                if (docSnap.exists()) {
                    String first = docSnap.getString(Constants.UserFields.FIRST_NAME);
                    String last = docSnap.getString(Constants.UserFields.SECOND_NAME);
                    String email = docSnap.getString(Constants.UserFields.EMAIL);
                    String pic = docSnap.getString(Constants.UserFields.PROFILE_PICTURE);

                    if (first == null || last == null || email == null) {
                        logger.w("Incomplete data for UID: " + uid);
                        onComplete.run();
                        return;
                    }

                    User user = new User(uid, first, last, email, pic);
                    pendingUsersList.add(user);
                }
            }
            onComplete.run();
        }).addOnFailureListener(e -> {
            logger.e(e.getMessage(), e.getCause());
            onComplete.run();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
