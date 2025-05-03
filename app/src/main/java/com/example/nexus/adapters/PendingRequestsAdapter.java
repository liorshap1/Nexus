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
package com.example.nexus.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nexus.Constants;
import com.example.nexus.R;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsViewHolder> {
    private final List<User> pendingUsersList;
    private final LocalUserSingleton localUser;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage firebaseStorage;
    private final AppLogger logger;
    private final Context context;

    public PendingRequestsAdapter(List<User> pendingUsersList, LocalUserSingleton localUser, FirebaseFirestore firestore, AppLogger appLogger,
            FirebaseStorage firebaseStorage, Context context) {
        this.pendingUsersList = pendingUsersList;
        this.localUser = localUser;
        this.firestore = firestore;
        this.logger = appLogger;
        this.firebaseStorage = firebaseStorage;
        this.context = context;
    }

    @NonNull
    @Override
    public PendingRequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_friend_card, parent, false);
        return new PendingRequestsViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull PendingRequestsViewHolder holder, int position) {
        User pendingUser = pendingUsersList.get(position);
        String pendingUserEmail = pendingUser.getEmail();
        String pendingUserFullName = pendingUser.getFullName();
        String pendingUserUid = pendingUser.getUid();

        fetchUserProfilePicture(pendingUserUid, uri -> {
            logger.v("I was fetched!");
            Glide.with(context).load(uri).circleCrop().into(holder.getProfileImage());
        });

        logger.d(pendingUserUid);
        holder.bind(pendingUserFullName, pendingUserEmail);

        holder.getAddButton().setOnClickListener(view -> {
            List<String> currentUserFriends = new ArrayList<>(localUser.getFriends());
            ArrayList<User> currentPendingUsersList = new ArrayList<>(pendingUsersList);
            currentUserFriends.add(pendingUserUid);
            currentPendingUsersList.remove(position);

            Map<String, Object> currentUserUpdates = new HashMap<>();
            currentUserUpdates.put(Constants.UserFields.FRIENDS, currentUserFriends);
            currentUserUpdates.put(Constants.UserFields.PENDING_REQUESTS, currentPendingUsersList);

            firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUser.getUid()).update(currentUserUpdates)
                    .addOnSuccessListener(unused -> logger.success("Updated current user friends list"))
                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));

            logger.v(pendingUserUid);
            firestore.collection(Constants.Firestore.USERS_COLLECTION).document(pendingUserUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot docSnap = task.getResult();
                    if (docSnap.exists()) {
                        List<String> otherUserFriends = (List<String>) docSnap.get(Constants.UserFields.FRIENDS);
                        if (otherUserFriends == null)
                            otherUserFriends = new ArrayList<>();

                        Map<String, Object> otherUpdates = new HashMap<>();

                        if (!otherUserFriends.contains(localUser.getUid())) {
                            otherUserFriends.add(localUser.getUid());
                            otherUpdates.put(Constants.UserFields.FRIENDS, otherUserFriends);
                        }

                        logger.w(otherUpdates.toString());
                        if (!otherUpdates.isEmpty()) {
                            firestore.collection(Constants.Firestore.USERS_COLLECTION).document(pendingUserUid).update(otherUpdates)
                                    .addOnSuccessListener(unused -> logger.success("Updated other used friends!"))
                                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
                        }

                    }
                }
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        });

        holder.getRemoveButton().setOnClickListener(view -> {
            pendingUsersList.remove(position);
            notifyItemRemoved(position);

            DocumentReference documentReference = firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUser.getUid());
            documentReference.update(Constants.UserFields.PENDING_REQUESTS, pendingUsersList)
                    .addOnSuccessListener(aVoid -> logger.success("Declined user successfully"))
                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        });
    }

    private void fetchUserProfilePicture(String uid, @NonNull Callback callback) {
        String path = "user_profile" + uid;
        String localUri = SharedPreferencesUtils.getDataByKey(context, path);

        if (localUri == null) {
            StorageReference storageRef = firebaseStorage.getReference().child(path);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                SharedPreferencesUtils.insertData(context, path, uri.toString());
                logger.success("Fetched user profile picture");

                callback.onComplete(uri);
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        } else {
            Uri imageUri = Uri.parse(localUri);
            callback.onComplete(imageUri);
        }
    }

    @Override
    public int getItemCount() {
        return pendingUsersList.size();
    }

    interface Callback {
        void onComplete(Uri uri);
    }
}
