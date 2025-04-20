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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.databinding.ActivityRegisterBinding;
import com.example.nexus.utils.GetTextUtils;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Objects;
import javax.inject.Inject;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    @Inject
    FirebaseFirestore firebaseFirestore;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;

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

            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword).addOnSuccessListener(authResult -> {
                String userId = Objects.requireNonNull(authResult.getUser()).getUid();
                User user = new User(privateName, familyName, userEmail, "0", userId);
                insertUserToCollection(user, new Callback() {
                    @Override
                    public void onInsertCompleted() {
                        logger.success("Create user successfully!");
                        SharedPreferencesUtils.insertData(RegisterActivity.this, Constants.UserFields.EMAIL, userEmail);
                        SharedPreferencesUtils.insertData(RegisterActivity.this, Constants.UserFields.PASSWORD, userPassword);
                    }

                    @Override
                    public void onInsertFailed(Exception e) {
                        logger.e(e.getMessage(), e.getCause());
                    }
                });
            }).addOnFailureListener(e -> {
                logger.e(e.getMessage(), e.getCause());
            });
        });

        binding.loginButton.setOnClickListener((view) -> {
            Intent loginActivityIntent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(loginActivityIntent);
        });
    }

    protected void insertUserToCollection(User user, Callback callback) {
        firebaseFirestore.collection(Constants.Firestore.USERS_COLLECTION).document(user.getUid()).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                localUserSingleton.initializeLocalUserSingleton(user.getFirstName(), user.getSecondName(), user.getEmail(), "00", user.getUid(), new ArrayList<>());
                callback.onInsertCompleted();
            }
        }).addOnFailureListener(callback::onInsertFailed);
    }

    interface Callback {
        void onInsertCompleted();

        void onInsertFailed(Exception e);
    }
}
