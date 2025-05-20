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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexus.Constants;
import com.example.nexus.R;
import com.example.nexus.adapters.SuggestedAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.chat.ChatActivity;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.core.services.FetchUsersService;
import com.example.nexus.core.services.NotificationsService;
import com.example.nexus.databinding.ActivityMainHomeBinding;
import com.example.nexus.home.fragments.ChatsFragment;
import com.example.nexus.home.fragments.MenuFragment;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

@AndroidEntryPoint
public class MainHomeActivity extends AppCompatActivity {
    @Inject
    FirebaseStorage firebaseStorage;
    @Inject
    FirebaseDatabase firebaseDatabase;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    AppLogger logger;
    private ActivityMainHomeBinding binding;
    @Nullable
    private FetchUsersService fetchUsersService;
    private SuggestedAdapter suggestedAdapter;
    private FragmentManager fragmentManager;
    private ChatsFragment chatsFragment;
    private MenuFragment menuFragment;
    private boolean isBound;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        suggestedAdapter = new SuggestedAdapter(getApplicationContext(), firebaseStorage, logger);
        fragmentManager = getSupportFragmentManager();
        chatsFragment = new ChatsFragment();
        menuFragment = new MenuFragment();

        fetchUserChats(new Callback() {
            @Override
            public void onComplete(@NonNull List<String> chatIds) {
                Intent serviceIntent = new Intent(MainHomeActivity.this, NotificationsService.class);
                logger.d(String.valueOf(chatIds));
                serviceIntent.putStringArrayListExtra(Constants.FIREBASE_DATABASE.CHATS, new ArrayList<>(chatIds));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                logger.e(e.getMessage(), e);
            }
        });

        isBound = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.suggestedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.suggestedRecyclerView.setAdapter(suggestedAdapter);

        Intent serviceIntent = new Intent(this, FetchUsersService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        SharedPreferencesUtils.insertData(getApplicationContext(), Constants.CURRENT_CHAT, "none");

        binding.suggestedRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            final GestureDetector gestureDetector = new GestureDetector(MainHomeActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
                    return true;
                }
            });

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    int position = rv.getChildAdapterPosition(child);
                    User user = suggestedAdapter.getUsers().get(position);

                    Intent openChatIntent = new Intent(MainHomeActivity.this, ChatActivity.class);
                    openChatIntent.putExtra(Constants.ChatsActivity.USER, user);
                    startActivity(openChatIntent);

                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), chatsFragment).addToBackStack(null).commit();
        BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setState(STATE_COLLAPSED);

        binding.openFriends.setOnClickListener(view -> {
            bottomSheetBehavior.setState(STATE_HALF_EXPANDED);
            binding.openFriends.setVisibility(INVISIBLE);
        });

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeItem) {
                fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), chatsFragment).addToBackStack(null).commit();
                return true;
            }
            if (itemId == R.id.menuItem) {
                fragmentManager.beginTransaction().replace(binding.fragmentContainer.getId(), menuFragment).addToBackStack(null).commit();
                bottomSheetBehavior.setState(STATE_HIDDEN);
                return true;
            }
            return true;
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

    private void fetchUserChats(@NonNull Callback callback) {
        firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> chatIdsList = new ArrayList<>();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(localUserSingleton.getUid())) {
                        chatIdsList.add(chatId);
                    }
                }
                // NOW that the list is populated, invoke the callback:
                callback.onComplete(chatIdsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.e("fetchUserChats cancelled", error.toException());
                // You may want to signal an error via the callback too:
                callback.onError(error.toException());
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

    interface Callback {
        void onComplete(List<String> list);

        void onError(Exception e);
    }
}