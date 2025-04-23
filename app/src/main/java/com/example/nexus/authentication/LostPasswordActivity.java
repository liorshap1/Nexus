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

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nexus.applogger.AppLogger;
import com.example.nexus.databinding.ActivityLostPasswordBinding;
import com.example.nexus.utils.GetTextUtils;
import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Inject;

public class LostPasswordActivity extends AppCompatActivity {
    private ActivityLostPasswordBinding binding;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    AppLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLostPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.submitButton.setOnClickListener(view -> {
            String userEmail = GetTextUtils.getTextFromInput(binding.emailInput);
            firebaseAuth.sendPasswordResetEmail(userEmail).addOnSuccessListener(unused -> logger.i("Sent password reset link"))
                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        });

        binding.backButton.setOnClickListener(view -> {
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
