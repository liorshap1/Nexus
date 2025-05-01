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
import com.example.nexus.core.OpenChat;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class OpenChatsAdapter extends RecyclerView.Adapter<OpenChatsHolder> {
    private final AppLogger logger;
    private final FirebaseStorage firebaseStorage;
    private final List<OpenChat> openChats;
    private final Context context;

    public OpenChatsAdapter(AppLogger logger, FirebaseStorage firebaseStorage, Context context) {
        this.openChats = new ArrayList<>();
        this.logger = logger;
        this.firebaseStorage = firebaseStorage;
        this.context = context;
    }

    @NonNull
    @Override
    public OpenChatsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new OpenChatsHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpenChatsHolder holder, int position) {
        User user = openChats.get(position).getDeliverUser();
        String lastMessage = openChats.get(position).getLastMessage();
        fetchUserProfilePicture(user.getUid(), uri -> Glide.with(context).load(uri).circleCrop().into(holder.getProfilePicture()));

        holder.bind(user, lastMessage);
    }

    private void fetchUserProfilePicture(String uid, SuggestedAdapter.Callback callback) {
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
    public void addSingleChat(OpenChat openChat) {
        this.openChats.add(openChat);
        notifyDataSetChanged();
    }
    public void clear() {
        this.openChats.clear();
    }

    @Override
    public int getItemCount() {
        return openChats.size();
    }
}
