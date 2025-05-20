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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.databinding.ActivityChangeSettingBinding;
import com.example.nexus.utils.GetTextUtils;
import com.example.nexus.utils.SnackbarType;
import com.example.nexus.utils.SnackbarUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChangeSettingActivity extends AppCompatActivity {
    private final Map<String, String> dictionary = new HashMap<>();
    @Inject
    FirebaseFirestore firestore;
    ActivityChangeSettingBinding binding;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Nullable
    private String FIELD_TO_CHANGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangeSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeDictionary();

        Intent intent = getIntent();
        FIELD_TO_CHANGE = intent.getStringExtra(Constants.ChangeSettingActivity.CHANGE_SETTING);
        binding.FIELDINPUT.setHelperText(dictionary.get(FIELD_TO_CHANGE));
        binding.FIELDINPUT.setHint(dictionary.get(FIELD_TO_CHANGE));
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.submitButton.setOnClickListener(view -> {
            String value = GetTextUtils.getTextFromInput(binding.FIELDINPUT);
            changeSetting(FIELD_TO_CHANGE, value, new Callback() {
                @Override
                public void onComplete() {
                    logger.success("Updated current user settings, see in firestore");
                    binding.FIELDINPUT.clearFocus();

                    SnackbarUtils.build(ChangeSettingActivity.this).setSnackbarType(SnackbarType.SUCCESS)
                            .setMessage("Updated current user settings, see in firestore").show();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    logger.e(e.getMessage(), e.getCause());
                }
            });
        });

        binding.backButton.setOnClickListener(view -> finish());
    }

    private void changeSetting(String fieldToChange, String value, @NonNull Callback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new IllegalStateException("No authenticated user found."));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put(fieldToChange, value);

        if (fieldToChange.equals(Constants.UserFields.PASSWORD)) {
            currentUser.updatePassword(value).addOnCompleteListener(task -> callback.onComplete()).addOnFailureListener(callback::onError);

        } else if (fieldToChange.equals(Constants.UserFields.EMAIL)) {
            currentUser.sendEmailVerification().addOnCompleteListener(task -> updateUserField(data, callback)).addOnFailureListener(callback::onError);

        } else {
            updateUserField(data, callback);
        }
    }

    private void updateUserField(Map<String, Object> data, @NonNull Callback callback) {
        firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUserSingleton.getUid()).update(data)
                .addOnCompleteListener(task -> callback.onComplete()).addOnFailureListener(callback::onError);
    }

    private void initializeDictionary() {
        dictionary.put(Constants.UserFields.EMAIL, "Enter new email: ");
        dictionary.put(Constants.UserFields.PASSWORD, "Enter new password: ");
        dictionary.put(Constants.UserFields.FIRST_NAME, "Enter your full name");
    }

    interface Callback {
        void onComplete();
        void onError(Exception e);
    }
}