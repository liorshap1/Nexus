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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nexus.Constants;
import com.example.nexus.R;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.databinding.ActivityChatBinding;
import com.example.nexus.utils.GetTextUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChangeSettingActivity extends AppCompatActivity {
    private Map<String, String> dictionary = new HashMap<>();
    private String FIELD_TO_CHANGE;
    ActivityChatBinding binding;
    @Inject
    FirebaseFirestore firestore;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    AppLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeDictionary();

        Intent intent = getIntent();
        FIELD_TO_CHANGE = intent.getStringExtra(Constants.ChangeSettingActivity.CHANGE_SETTING);
        binding.textInput.setHelperText(dictionary.get(FIELD_TO_CHANGE));
        binding.textInput.setHint(dictionary.get(FIELD_TO_CHANGE));
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.sendButton.setOnClickListener(view -> {
            String value = GetTextUtils.getTextFromInput(binding.textInput);
            changeSetting(FIELD_TO_CHANGE, value, new Callback() {
                @Override
                public void onComplete() {
                    logger.success("Updated current user settings, see in firestore");
                }

                @Override
                public void onError(Exception e) {
                    logger.e(e.getMessage(), e.getCause());
                }
            });
        });

        binding.backButton.setOnClickListener(view -> finish());
    }

    private void changeSetting(String FIELD_TO_CHANGE, String VALUE, Callback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_TO_CHANGE, VALUE);

        firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUserSingleton.getUid()).update(data).addOnCompleteListener(task -> callback.onComplete())
                .addOnFailureListener(callback::onError);
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