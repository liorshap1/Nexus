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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nexus.Constants;
import com.example.nexus.adapters.OpenChatsAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.OpenChat;
import com.example.nexus.core.User;
import com.example.nexus.databinding.FragmentChatsBinding;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
@AndroidEntryPoint
public class ChatsFragment extends Fragment {

    @Inject
    AppLogger logger;
    @Inject
    FirebaseFirestore firestore;
    @Inject
    FirebaseAuth auth;
    @Inject
    FirebaseDatabase firebaseDatabase;
    @Inject
    LocalUserSingleton localUserSingleton;
    @Inject
    FirebaseStorage firebaseStorage;
    private FragmentChatsBinding binding;
    private final Gson gson = new Gson();
    private OpenChatsAdapter openChatsAdapter;
    private ValueEventListener chatsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        openChatsAdapter = new OpenChatsAdapter(logger, firebaseStorage, requireContext());
        binding.openChatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.openChatsRecyclerView.setAdapter(openChatsAdapter);

        fetchAllUserChats();

        return binding.getRoot();
    }

    private void fetchAllUserChats() {
        DatabaseReference chatsRef = firebaseDatabase.getReference(Constants.FIREBASE_DATABASE.CHATS);

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openChatsAdapter.clear(); // prevent duplicates

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.contains(localUserSingleton.getUid())) {
                        String deliverUid = getOtherUid(chatId);

                        fetchUser(deliverUid, new UserCallback() {
                            @Override
                            public void onUserFetched(User user) {
                                fetchLastMessage(chatSnapshot, message -> {
                                    OpenChat chat = new OpenChat(user, message);
                                    openChatsAdapter.addSingleChat(chat);
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                logger.e("Failed to fetch user", e);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.e("Chat listener cancelled: " + error.getMessage(), error.toException());
            }
        };

        chatsRef.addValueEventListener(chatsListener);
    }

    private void fetchLastMessage(DataSnapshot chatSnapshot, MessageCallback callback) {
        chatSnapshot.child(Constants.FIREBASE_DATABASE.MESSAGES).getRef().orderByChild(Constants.FIREBASE_DATABASE.TIMESTAMP).limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastMessage = "";
                        for (DataSnapshot message : snapshot.getChildren()) {
                            lastMessage = message.child(Constants.MessageFields.MESSAGE).getValue(String.class);
                        }
                        callback.onMessageFetched(lastMessage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        logger.e("Failed to fetch last message: " + error.getMessage(), error.toException());
                        callback.onMessageFetched(null);
                    }
                });
    }

    private void fetchUser(String uid, UserCallback callback) {
        String cached = SharedPreferencesUtils.getDataByKey(requireContext(), "user_" + uid);
        if (cached != null) {
            callback.onUserFetched(gson.fromJson(cached, User.class));
            return;
        }

        firestore.collection(Constants.Firestore.USERS_COLLECTION).document(uid).get().addOnSuccessListener(docSnap -> {
            if (docSnap.exists()) {
                User user = new User(uid, Objects.requireNonNull(docSnap.getString(Constants.UserFields.FIRST_NAME)),
                        Objects.requireNonNull(docSnap.getString(Constants.UserFields.SECOND_NAME)),
                        Objects.requireNonNull(docSnap.getString(Constants.UserFields.EMAIL)), docSnap.getString(Constants.UserFields.PROFILE_PICTURE));
                SharedPreferencesUtils.insertData(requireContext(), "user_" + uid, gson.toJson(user));
                callback.onUserFetched(user);
            }
        }).addOnFailureListener(callback::onError);
    }

    private String getOtherUid(String chatKey) {
        String[] parts = chatKey.split("_");
        return parts[0].equals(localUserSingleton.getUid()) ? parts[1] : parts[0];
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsListener != null) {
            firebaseDatabase.getReference(Constants.FIREBASE_DATABASE.CHATS).removeEventListener(chatsListener);
        }
        binding = null;
    }

    interface UserCallback {
        void onUserFetched(User user);
        void onError(Exception e);
    }

    interface MessageCallback {
        void onMessageFetched(String message);
    }
}
