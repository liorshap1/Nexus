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
package com.example.nexus.home.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.authentication.LoginActivity;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.databinding.FragmentMenuBinding;
import com.example.nexus.home.ChangeSettingActivity;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MenuFragment extends Fragment {
    @Inject
    AppLogger logger;
    @Inject
    FirebaseStorage firebaseStorage;
    @Inject
    LocalUserSingleton localUser;
    @Inject
    FirebaseFirestore firebaseFirestore;
    @Inject
    FirebaseAuth firebaseAuth;
    private FragmentMenuBinding binding;
    @NonNull
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (uri != null) {
            Glide.with(requireContext()).load(uri).circleCrop().into(binding.profileImage);

            String path = "user_profile" + localUser.getUid();
            SharedPreferencesUtils.insertData(requireContext(), path, uri.toString());
            logger.success("Inserted user profile picture in shared preferences");

            StorageReference storageRef = firebaseStorage.getReference().child(path);

            // Delete old profile picture, then upload the new one
            storageRef.delete().addOnSuccessListener(unused -> logger.success("Deleted current profile picture"))
                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause())).addOnCompleteListener(task -> {
                        // Regardless of delete success or failure, upload the new file
                        storageRef.putFile(uri).addOnSuccessListener(unused -> logger.success("Uploaded new profile picture"))
                                .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
                    });
        }
    });

    public MenuFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);

        binding.userName.setText(localUser.getFullName());
        binding.userEmail.setText(localUser.getEmail());

        fetchUserProfilePicture(localUser.getUid(), uri -> {
            Glide.with(requireContext()).load(uri).circleCrop().into(binding.profileImage);
        });

        binding.profileImage.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });

        binding.signOutButton.setOnClickListener(view -> {
            firebaseAuth.signOut();
            logger.success("Signed out");

            startActivity(new Intent(getContext(), LoginActivity.class));
        });

        binding.changeEmailButton.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), ChangeSettingActivity.class);
            intent.putExtra(Constants.ChangeSettingActivity.CHANGE_SETTING, Constants.UserFields.EMAIL);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    private void fetchUserProfilePicture(String uid, @NonNull Callback callback) {
        String path = "user_profile" + uid;
        String localUri = SharedPreferencesUtils.getDataByKey(requireContext(), path);

        if (localUri == null) {
            StorageReference storageRef = firebaseStorage.getReference().child(path);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                SharedPreferencesUtils.insertData(requireContext(), path, uri.toString());
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