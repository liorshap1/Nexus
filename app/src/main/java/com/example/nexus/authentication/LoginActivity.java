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

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nexus.Constants;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.databinding.ActivityLoginBinding;
import com.example.nexus.utils.GetTextUtils;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import javax.inject.Inject;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {
  @Inject LocalUserSingleton localUserSingleton;
  @Inject FirebaseAuth firebaseAuth;
  @Inject AppLogger logger;
  private ActivityLoginBinding binding;
  private Disposable loginDisposable;
  private FetchUsersService fetchUsersService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityLoginBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
  }

  @SuppressLint("CheckResult")
  @Override
  protected void onStart() {
    super.onStart();

    var userEmail = SharedPreferencesUtils.getDataByKey(this, Constants.UserFields.EMAIL);
    var userPassword = SharedPreferencesUtils.getDataByKey(this, Constants.UserFields.PASSWORD);

    if (userEmail != null && userPassword != null) {
      GetTextUtils.setInputText(binding.emailInput, userEmail);
      GetTextUtils.setInputText(binding.passwordInput, userPassword);
    }

    Intent fetchUsersServiceIntent = new Intent(this, FetchUsersService.class);
    binding.loginButton.setOnClickListener(
        view -> {
          String email = GetTextUtils.getTextFromInput(binding.emailInput);
          String password = GetTextUtils.getTextFromInput(binding.passwordInput);

          loginDisposable =
              signIn(email, password)
                  .doOnSubscribe(
                      disposable -> {
                        binding.progressBar.setVisibility(VISIBLE);
                      })
                  .observeOn(AndroidSchedulers.mainThread())
                  .doOnSuccess(uid -> localUserSingleton.setUid(uid))
                  .flatMap(this::retrieveUser)
                  .subscribe(
                      docSnap -> {
                        localUserSingleton.initializeLocalUserSingleton(
                            docSnap.getString(Constants.UserFields.FIRST_NAME),
                            docSnap.getString(Constants.UserFields.SECOND_NAME),
                            docSnap.getString(Constants.UserFields.EMAIL),
                            docSnap.getString(Constants.UserFields.PROFILE_PICTURE),
                            localUserSingleton.getUid(),
                            (ArrayList<String>) docSnap.get(Constants.UserFields.FRIENDS));

                        fetchUsersServiceIntent.putExtra(
                            Constants.USERS_KEY, docSnap.getString(Constants.UserFields.FRIENDS));
                        startService(fetchUsersServiceIntent);
                        logger.success("Started Fetching Service, LoginActivity");

                        SharedPreferencesUtils.insertData(
                            LoginActivity.this, Constants.UserFields.EMAIL, email);
                        SharedPreferencesUtils.insertData(
                            LoginActivity.this, Constants.UserFields.PASSWORD, password);
                        logger.i("Inserted user credentials in shared preferences");
                      },
                      e -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                      });
        });

    binding.registerButton.setOnClickListener(
        view -> {
          Intent registerActivityIntent = new Intent(LoginActivity.this, RegisterActivity.class);
          startActivity(registerActivityIntent);
        });

    binding.forgotPasswordButton.setOnClickListener(
        view -> {
          Intent lostPasswordActivityIntent =
              new Intent(LoginActivity.this, LostPasswordActivity.class);
          startActivity(lostPasswordActivityIntent);
        });
  }

  protected Single<String> signIn(String email, String password) {
    return Single.create(
        emitter -> {
          firebaseAuth
              .signInWithEmailAndPassword(email, password)
              .addOnSuccessListener(
                  authResult -> {
                    String uid = authResult.getUser().getUid();
                    emitter.onSuccess(uid);
                  })
              .addOnFailureListener(emitter::onError);
        });
  }

  protected Single<DocumentSnapshot> retrieveUser(String uid) {
    return Single.create(
        emitter -> {
          FirebaseFirestore.getInstance()
              .collection("users")
              .document(uid)
              .get()
              .addOnSuccessListener(emitter::onSuccess)
              .addOnFailureListener(emitter::onError);
        });
  }

  @Override
  protected void onDestroy() {
    if (loginDisposable != null && !loginDisposable.isDisposed()) {
      loginDisposable.dispose();
    }
    super.onDestroy();
  }
}
