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
import com.example.nexus.R;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class SuggestedAdapter extends RecyclerView.Adapter<SuggestedViewHolder> {
    private final AppLogger logger;
    private List<User> usersList;
    private Context context;
    private FirebaseStorage firebaseStorage;

    public SuggestedAdapter(Context context, FirebaseStorage firebaseStorage, AppLogger logger) {
        usersList = new ArrayList<>();
        this.context = context;
        this.firebaseStorage = firebaseStorage;
        this.logger = logger;
    }

    @NonNull
    @Override
    public SuggestedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new SuggestedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestedViewHolder holder, int position) {
        if (holder.getUsername() == null) {
            throw new IllegalStateException("nameTextView is null! Did you inflate the right layout and use the correct ID?");
        }
        if (holder.getEmail() == null) {
            throw new IllegalStateException("emailTextView is null! Check R.id.tvUserEmail in user_card.xml");
        }

        String email = usersList.get(position).getEmail();
        String username = usersList.get(position).getFullName();
        fetchUserProfilePicture(usersList.get(position).getUid(), uri -> Glide.with(context).load(uri).circleCrop().into(holder.getProfilePicture()));

        holder.bind(username, email);
    }

    private void fetchUserProfilePicture(String uid, Callback callback) {
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

    @SuppressLint("NotifyDataSetChanged")
    public void setUsers(List<User> newUsers) {
        usersList.clear();
        if (newUsers != null) {
            logger.v("Maybe here is the problem?");
            logger.v(String.valueOf(newUsers));
            usersList.addAll(newUsers);
        }

        notifyDataSetChanged();
    }

    public List<User> getUsers() {
        return usersList;
    };

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    interface Callback {
        void onComplete(Uri uri);
    }
}
