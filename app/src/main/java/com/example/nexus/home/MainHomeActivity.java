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

import static android.view.View.INVISIBLE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nexus.adapters.SuggestedAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.User;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.databinding.ActivityMainHomeBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainHomeActivity extends AppCompatActivity {
    private final List<User> allUserList = new ArrayList<>();
    private final List<User> filteredList = new ArrayList<>();
    @Inject
    AppLogger appLogger;
    private ActivityMainHomeBinding binding;
    private FetchUsersService fetchUsersService;
    private SuggestedAdapter suggestedAdapter;
    private boolean isBound;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("CheckResult")
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FetchUsersService.LocalBinder binder = (FetchUsersService.LocalBinder) iBinder;
            fetchUsersService = binder.getService();
            isBound = true;

            appLogger.success("Service connected. Subscribing to users...");
            fetchUsersService.getUsersObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(users -> {
                allUserList.clear();
                filteredList.clear();

                allUserList.addAll(users);
                filteredList.addAll(users);
                suggestedAdapter.notifyDataSetChanged();

            }, throwable -> appLogger.e("Error observing users", throwable));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            fetchUsersService = null;
            appLogger.w("Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        suggestedAdapter = new SuggestedAdapter(filteredList);
        binding.suggestedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.suggestedRecyclerView.setAdapter(suggestedAdapter);

        isBound = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(this, FetchUsersService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setState(STATE_COLLAPSED);

        binding.openFriends.setOnClickListener(view -> {
            bottomSheetBehavior.setState(STATE_HALF_EXPANDED);
            binding.openFriends.setVisibility(INVISIBLE);
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == STATE_COLLAPSED) {
                    binding.openFriends.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        binding.addFriendsButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, FriendsActivity.class);
            startActivity(intent);
        });

        binding.searchInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence == null || charSequence.toString().trim().isEmpty()) {
                    filteredList.clear();
                    filteredList.addAll(allUserList);
                    suggestedAdapter.notifyDataSetChanged();
                } else {
                    filteredList.clear();
                    String lowerQuery = charSequence.toString().toLowerCase();
                    for (User user : allUserList) {
                        boolean matchesFirst = user.getFirstName() != null && user.getFirstName().toLowerCase().contains(lowerQuery);
                        boolean matchesLast = user.getSecondName() != null && user.getSecondName().toLowerCase().contains(lowerQuery);
                        boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery);

                        if (matchesFirst || matchesLast || matchesEmail) {
                            filteredList.add(user); // Add only relevant results
                        }
                    }
                }

                suggestedAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
