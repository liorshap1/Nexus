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
package com.example.nexus;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.nexus.applogger.AppLogger;
import com.example.nexus.authentication.LoginActivity;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.databinding.ActivityMainBinding;
import com.example.nexus.home.MainHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    FirebaseFirestore firebaseFirestore;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    AppLogger logger;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    logger.i("All permissions granted");
                } else {
                    logger.w("Permissions are not granted");
                }
            });
    private ActivityMainBinding binding;
    private Intent fetchUsersServiceIntent;

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void askApplicationPermissions() {
        String[] permissions = {android.Manifest.permission.INTERNET, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING};

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), Constants.REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                askApplicationPermissions();
            }
        }

        fetchUsersServiceIntent = new Intent(getApplicationContext(), FetchUsersService.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.lottieAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    retrieveLocalUserOnce(firebaseUser.getUid(), doc -> {
                        logger.v(doc.toString());
                        localUserSingleton.initializeLocalUserSingleton(firebaseUser.getUid(), doc.getString(Constants.UserFields.FIRST_NAME),
                                doc.getString(Constants.UserFields.SECOND_NAME), doc.getString(Constants.UserFields.EMAIL),
                                doc.getString(Constants.UserFields.PROFILE_PICTURE), (ArrayList<String>) doc.get(Constants.UserFields.FRIENDS));

                        logger.v(localUserSingleton.toString());
                        startService(fetchUsersServiceIntent);
                        startActivity(new Intent(MainActivity.this, MainHomeActivity.class));
                    });
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }
        });
    }

    protected void retrieveLocalUserOnce(@NonNull String uid, @NonNull Callback callback) {
        firebaseFirestore.collection(Constants.Firestore.USERS_COLLECTION).document(uid).get() // <-- one-time network + cache fetch
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        logger.success("Retrieved snapshot for: " + uid);
                        callback.onComplete(doc);
                    } else {
                        logger.w("No LocalUser doc for: " + uid);
                    }
                }).addOnFailureListener(e -> logger.e("Failed to retrieve LocalUser: " + uid, e));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    interface Callback {
        void onComplete(DocumentSnapshot docSnap);
    }
}
