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
package com.example.nexus.chat;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.nexus.Constants;
import com.example.nexus.R;
import com.example.nexus.adapters.ChatAdapter;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.Message;
import com.example.nexus.core.User;
import com.example.nexus.databinding.ActivityChatBinding;
import com.example.nexus.utils.GetTextUtils;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.example.nexus.utils.SpeechRecognizerUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatActivity extends AppCompatActivity {
    private final List<Message> messages = new ArrayList<>();
    @Inject
    FirebaseStorage firebaseStorage;
    @Inject
    FirebaseDatabase firebaseDatabase;
    @Inject
    AppLogger logger;
    @Inject
    LocalUserSingleton localUserSingleton;
    private ChatAdapter chatAdapter;
    private ActivityChatBinding binding;
    private SpeechRecognizerUtils speechRecognizerUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        User user = getIntent().getParcelableExtra(Constants.ChatsActivity.USER);
        assert user != null;
        String currentChatId = getChatRoomId(localUserSingleton.getUid(), user.getUid());
        SharedPreferencesUtils.insertData(this, Constants.CURRENT_CHAT, currentChatId);

        chatAdapter = new ChatAdapter();
        chatAdapter.setOnMessageLongClickListener(this::showMessageOptionsDialog);
        chatAdapter.setOnMessageClickListener(this::showEmojiPopup);
        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerView.setAdapter(chatAdapter);

        fetchMessagesFromDatabase(currentChatId);

        fetchUserProfilePicture(user.getUid(), uri -> {
            logger.success("Fetched user profile image");
            Glide.with(ChatActivity.this).load(uri).circleCrop().into(binding.profilePicture);
        });

        binding.userName.setText(user.getFullName());
        binding.backButton.setOnClickListener(view -> finish());

        binding.sendButton.setOnClickListener(view -> {
            String inputText = GetTextUtils.getTextFromInput(binding.textInput);
            if (!inputText.isEmpty()) {
                insertMessageToDatabase(currentChatId, inputText);
                Objects.requireNonNull(binding.textInput.getEditText()).setText("");
            }
        });

        setupSpeechRecognizer();
    }

    private void fetchUserProfilePicture(String uid, @NonNull Callback callback) {
        String path = "user_profile" + uid;
        String localUri = SharedPreferencesUtils.getDataByKey(ChatActivity.this, path);

        if (localUri == null) {
            StorageReference storageRef = firebaseStorage.getReference().child(path);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                SharedPreferencesUtils.insertData(ChatActivity.this, path, uri.toString());
                logger.success("Fetched user profile picture");
                callback.onComplete(uri);
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        } else {
            callback.onComplete(Uri.parse(localUri));
        }
    }

    private void fetchMessagesFromDatabase(@NonNull String chatId) {
        DatabaseReference chatRef = firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).child(chatId)
                .child(Constants.FIREBASE_DATABASE.MESSAGES);

        chatRef.orderByChild(Constants.FIREBASE_DATABASE.TIMESTAMP).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String messageText = snapshot.child(Constants.MessageFields.MESSAGE).getValue(String.class);
                String messageDeliverUid = snapshot.child(Constants.MessageFields.DELIVER_UID).getValue(String.class);
                Long timestamp = snapshot.child(Constants.MessageFields.TIMESTAMP).getValue(Long.class);
                DataSnapshot reactionsSnapshot = snapshot.child(Constants.MessageFields.REACTIONS);
                Map<String, String> reactions = new HashMap<>();
                for (DataSnapshot entry : reactionsSnapshot.getChildren()) {
                    reactions.put(entry.getKey(), entry.getValue(String.class));
                }

                if (messageText != null && messageDeliverUid != null && timestamp != null) {
                    Message.MessageType messageType = messageDeliverUid.equals(localUserSingleton.getUid())
                            ? Message.MessageType.SENDER
                            : Message.MessageType.RECEIVER;
                    Message messageInstance = new Message(messageText, timestamp, messageType, messageDeliverUid, reactions);
                    messages.add(messageInstance);
                    chatAdapter.submitList(new ArrayList<>(messages));
                    binding.messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String messageText = snapshot.child(Constants.MessageFields.MESSAGE).getValue(String.class);
                String messageDeliverUid = snapshot.child(Constants.MessageFields.DELIVER_UID).getValue(String.class);
                Long timestamp = snapshot.child(Constants.MessageFields.TIMESTAMP).getValue(Long.class);
                DataSnapshot reactionsSnapshot = snapshot.child(Constants.MessageFields.REACTIONS);
                Map<String, String> reactions = new HashMap<>();
                for (DataSnapshot entry : reactionsSnapshot.getChildren()) {
                    reactions.put(entry.getKey(), entry.getValue(String.class));
                }

                if (messageText != null && messageDeliverUid != null && timestamp != null) {
                    Message.MessageType messageType = messageDeliverUid.equals(localUserSingleton.getUid())
                            ? Message.MessageType.SENDER
                            : Message.MessageType.RECEIVER;

                    Message updatedMessage = new Message(messageText, timestamp, messageType, messageDeliverUid, reactions);
                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i).getTimestamp() == timestamp) {
                            messages.set(i, updatedMessage);
                            chatAdapter.submitList(new ArrayList<>(messages));
                            break;
                        }
                    }
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.e(error.getMessage(), error.toException());
            }
        });
    }

    private void insertMessageToDatabase(@NonNull String chatId, String text) {
        DatabaseReference chatRef = firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).child(chatId)
                .child(Constants.FIREBASE_DATABASE.MESSAGES);

        String messageId = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put(Constants.MessageFields.DELIVER_UID, localUserSingleton.getUid());
        messageData.put(Constants.MessageFields.DELIVER_NAME, localUserSingleton.getFullName());
        messageData.put(Constants.MessageFields.MESSAGE, text);
        messageData.put(Constants.MessageFields.TIMESTAMP, timestamp);

        assert messageId != null;
        chatRef.child(messageId).setValue(messageData).addOnSuccessListener(unused -> logger.success("Added new message successfully"))
                .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
    }

    private void showMessageOptionsDialog(int position, @NonNull Message message) {
        new MaterialAlertDialogBuilder(this).setTitle("Message Options").setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
            if (which == 0) {
                logger.d("EDITING MESSAGE");
            } else if (which == 1) {
                deleteMessage(position, message);
            }
        }).setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_background)).show();
    }

    private void showEmojiPopup(@NonNull View anchor, @NonNull Message message) {
        @SuppressLint("InflateParams")
        View popupView = LayoutInflater.from(this).inflate(R.layout.reaction_popup, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setElevation(20);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(anchor, -anchor.getWidth() / 2, -anchor.getHeight() * 2);

        popupView.findViewById(R.id.reaction_like).setOnClickListener(v -> {
            addReactionToMessage(message, "👍");
            popupWindow.dismiss();
        });
        popupView.findViewById(R.id.reaction_heart).setOnClickListener(v -> {
            addReactionToMessage(message, "❤️");
            popupWindow.dismiss();
        });
    }

    private void addReactionToMessage(@NonNull Message message, String emoji) {
        String chatRoomId = SharedPreferencesUtils.getDataByKey(this, Constants.CURRENT_CHAT);
        if (chatRoomId == null)
            return;

        DatabaseReference messagesRef = firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).child(chatRoomId)
                .child(Constants.FIREBASE_DATABASE.MESSAGES);

        messagesRef.orderByChild(Constants.MessageFields.TIMESTAMP).equalTo(message.getTimestamp()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    messageSnapshot.getRef().child("reactions").child(localUserSingleton.getUid()).setValue(emoji);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.e(error.getMessage(), error.toException());
            }
        });
    }

    private void deleteMessage(int position, @NonNull Message message) {
        String chatRoomId = SharedPreferencesUtils.getDataByKey(this, Constants.CURRENT_CHAT);
        if (chatRoomId == null) {
            logger.e("Chat room ID is null", new Throwable("No chat ID found"));
            return;
        }

        DatabaseReference chatRef = firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).child(chatRoomId)
                .child(Constants.FIREBASE_DATABASE.MESSAGES);

        chatRef.orderByChild(Constants.MessageFields.TIMESTAMP).equalTo(message.getTimestamp()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    messageSnapshot.getRef().removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(ChatActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                        messages.remove(position);
                        chatAdapter.submitList(new ArrayList<>(messages));
                    }).addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        logger.e(e.getMessage(), e.getCause());
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                logger.e(error.getMessage(), error.toException());
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSpeechRecognizer() {
        speechRecognizerUtils = new SpeechRecognizerUtils(this, new SpeechRecognizerUtils.Callback() {
            @Override
            public void onSpeechResult(String text) {
                Objects.requireNonNull(binding.textInput.getEditText()).setText(text);
            }

            @Override
            public void onSpeechError(int error) {
                logger.e(String.valueOf(error), new Throwable("Speech recognizer error"));
            }
        });

        Handler motionHandler = new Handler();
        Runnable startSpeech = () -> {
            speechRecognizerUtils.startListening();
            Toast.makeText(ChatActivity.this, "Hold to record!", Toast.LENGTH_SHORT).show();
        };

        binding.microphoneButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                motionHandler.postDelayed(startSpeech, 2000);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                speechRecognizerUtils.stopListening();
            }
            return false;
        });
    }

    @NonNull
    private String getChatRoomId(@NonNull String uid1, @NonNull String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    interface Callback {
        void onComplete(Uri uri);
    }
}
