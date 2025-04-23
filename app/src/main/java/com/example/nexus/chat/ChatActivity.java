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
package com.example.nexus.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.example.nexus.databinding.ActivityChatBinding;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatActivity extends AppCompatActivity {
    @Inject
    FirebaseStorage firebaseStorage;
    @Inject
    AppLogger logger;
    private ActivityChatBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        User user = (User) intent.getSerializableExtra(Constants.ChatsActivity.USER);

        assert user != null;
        fetchUserProfilePicture(user.getUid(), uri -> {
            logger.success("Fetched user profile image");
            Glide.with(ChatActivity.this).load(uri).circleCrop().into(binding.profilePicture);
        });

        binding.userName.setText(user.getFullName());
        binding.backButton.setOnClickListener(view -> finish());
    }

    private void fetchUserProfilePicture(String uid, Callback callback) {
        String path = "user_profile" + uid;
        String localUri = SharedPreferencesUtils.getDataByKey(ChatActivity.this, path);

        if (localUri == null) {
            StorageReference storageRef = firebaseStorage.getReference().child(path);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                SharedPreferencesUtils.insertData(ChatActivity.this, path, uri.toString());
                logger.success("Fetched user profile picture");

                callback.onComplete(uri);
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        } else {
            Uri imageUri = Uri.parse(localUri);
            callback.onComplete(imageUri);
        }
    }

    interface Callback {
        void onComplete(Uri uri);
    }
}
