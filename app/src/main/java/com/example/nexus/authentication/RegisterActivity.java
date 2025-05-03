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
package com.example.nexus.authentication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.databinding.ActivityRegisterBinding;
import com.example.nexus.home.MainHomeActivity;
import com.example.nexus.utils.GenerateAvatarUtils;
import com.example.nexus.utils.GetTextUtils;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends AppCompatActivity {
    @Inject
    FirebaseFirestore firebaseFirestore;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    FirebaseStorage firebaseStorage;
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.registerButton.setOnClickListener(view -> {
            String userEmail = GetTextUtils.getTextFromInput(binding.emailInput);
            String userPassword = GetTextUtils.getTextFromInput(binding.passwordInput);
            String privateName = GetTextUtils.getTextFromInput(binding.privateNameInput);
            String familyName = GetTextUtils.getTextFromInput(binding.familyNameInput);

            logger.v(userEmail);
            logger.v(userPassword);
            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword).addOnSuccessListener(authResult -> {
                String userId = Objects.requireNonNull(authResult.getUser()).getUid();
                User user = new User(userId, privateName, familyName, userEmail, "0");
                insertUserToCollection(user, new Callback() {
                    @Override
                    public void onInsertCompleted() {
                        logger.success("Create user successfully!");
                        SharedPreferencesUtils.insertData(RegisterActivity.this, Constants.UserFields.EMAIL, userEmail);
                        SharedPreferencesUtils.insertData(RegisterActivity.this, Constants.UserFields.PASSWORD, userPassword);

                        Intent loginActivityIntent = new Intent(RegisterActivity.this, MainHomeActivity.class);
                        startActivity(loginActivityIntent);
                    }

                    @Override
                    public void onInsertFailed(@NonNull Exception e) {
                        logger.e(e.getMessage(), e.getCause());
                    }
                });
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        });

        binding.loginButton.setOnClickListener((view) -> {
            Intent loginActivityIntent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(loginActivityIntent);
        });
    }

    private void createUserProfilePicture(String fullName, String userUid) {
        Bitmap avatar = GenerateAvatarUtils.generateInitialsAvatar(fullName, 250, this);
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference userPhotoRef = storageRef.child("user_profile" + userUid);

        Glide.with(getApplicationContext()).asBitmap().load(avatar) // can be URL, drawable, etc.
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Uri uri = bitmapToUri(getApplicationContext(), resource);
                        userPhotoRef.putFile(uri).addOnSuccessListener(taskSnapshot -> logger.success("Uploaded default Image Profile"))
                                .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    @Nullable
    public Uri bitmapToUri(@NonNull Context context, @NonNull Bitmap bitmap) {
        try {
            File file = new File(context.getCacheDir(), "image_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            logger.e(e.getMessage(), e.getCause());
            return null;
        }
    }

    private void insertUserToCollection(@NonNull User user, @NonNull Callback callback) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put(Constants.UserFields.FIRST_NAME, user.getFirstName());
        userMap.put(Constants.UserFields.SECOND_NAME, user.getSecondName());
        userMap.put(Constants.UserFields.EMAIL, user.getEmail());
        userMap.put(Constants.UserFields.UID, user.getUid());
        userMap.put(Constants.UserFields.PROFILE_PICTURE, "user_" + user.getUid());
        userMap.put(Constants.UserFields.FRIENDS, new ArrayList<>());
        userMap.put(Constants.UserFields.PENDING_REQUESTS, new ArrayList<>());
        createUserProfilePicture(user.getFullName(), user.getUid());

        firebaseFirestore.collection(Constants.Firestore.USERS_COLLECTION).document(user.getUid()).set(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                localUserSingleton.initializeLocalUserSingleton(userMap);
                callback.onInsertCompleted();
            }
        }).addOnFailureListener(callback::onInsertFailed);
    }

    interface Callback {
        void onInsertCompleted();

        void onInsertFailed(Exception e);
    }
}
