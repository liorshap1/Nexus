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
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexus.R;
import com.example.nexus.adapters.SuggestedAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.databinding.ActivityMainHomeBinding;
import com.example.nexus.home.fragments.ChatsFragment;
import com.example.nexus.home.fragments.MenuFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.storage.FirebaseStorage;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

@AndroidEntryPoint
public class MainHomeActivity extends AppCompatActivity {
    @Inject
    AppLogger logger;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint({"CheckResult", "NotifyDataSetChanged"})
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FetchUsersService.LocalBinder binder = (FetchUsersService.LocalBinder) iBinder;
            fetchUsersService = binder.getService();
            isBound = true;

            fetchUsersService.observeCurrentUsers().observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                if (list != null) {
                    suggestedAdapter.setUsers(list);
                }
            }, error -> {
                logger.e("Error observing users: " + error.getMessage(), error);
            });

            logger.success("FetchUsersService bounded to MainHomeActivity");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            fetchUsersService = null;
            logger.w("Service disconnected");
        }
    };
    private ActivityMainHomeBinding binding;
    private FetchUsersService fetchUsersService;
    private SuggestedAdapter suggestedAdapter;
    @Inject
    FirebaseStorage firebaseStorage;
    private FragmentManager fragmentManager;
    private ChatsFragment chatsFragment;
    private boolean isBound;
    private MenuFragment menuFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        suggestedAdapter = new SuggestedAdapter(getApplicationContext(), firebaseStorage, logger);
        fragmentManager = getSupportFragmentManager();
        chatsFragment = new ChatsFragment();
        menuFragment = new MenuFragment();

        isBound = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.suggestedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.suggestedRecyclerView.setAdapter(suggestedAdapter);

        Intent serviceIntent = new Intent(this, FetchUsersService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        binding.suggestedRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                logger.i(rv.toString());
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), chatsFragment).addToBackStack(null).commit();
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeItem) {
                fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), chatsFragment).addToBackStack(null).commit();
                return true;
            }
            if (itemId == R.id.menuItem) {
                fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), menuFragment).addToBackStack(null).commit();
                return true;
            }
            return true;
        });

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
                if (newState == STATE_COLLAPSED || newState == STATE_HIDDEN) {
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