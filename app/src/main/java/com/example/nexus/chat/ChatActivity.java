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
import android.content.Intent;
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
import androidx.annotation.Nullable;
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
import com.example.nexus.utils.SnackbarType;
import com.example.nexus.utils.SnackbarUtils;
import com.example.nexus.utils.SpeechRecognizerUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

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
    private DatabaseReference chatRef;
    private ChildEventListener messageListener;
    private SpeechRecognizerUtils speechRecognizerUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent ChatActivityIntent = getIntent();
        User user = (User) ChatActivityIntent.getParcelableExtra(Constants.ChatsActivity.USER);
        if (user != null) {
            String chatId = getChatRoomId(localUserSingleton.getUid(), user.getUid());
            SharedPreferencesUtils.insertData(this, Constants.CURRENT_CHAT, chatId);
            chatRef = firebaseDatabase.getReference().child(Constants.FIREBASE_DATABASE.CHATS).child(chatId).child(Constants.FIREBASE_DATABASE.MESSAGES);
            fetchMessages();
        }

        chatAdapter = new ChatAdapter();
        chatAdapter.setOnMessageLongClickListener(this::showMessageOptionsDialog);
        chatAdapter.setOnMessageClickListener(this::showEmojiPopup);
        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerView.setAdapter(chatAdapter);

        fetchUserProfilePicture(user.getUid(), uri -> Glide.with(ChatActivity.this).load(uri).circleCrop().into(binding.profilePicture));
        binding.userName.setText(user.getFullName());
        binding.backButton.setOnClickListener(view -> finish());
        binding.sendButton.setOnClickListener(view -> {
            String inputText = GetTextUtils.getTextFromInput(binding.textInput);
            if (!inputText.isEmpty()) {
                insertMessage(inputText);
                Objects.requireNonNull(binding.textInput.getEditText()).setText("");
            }
        });
    }

    private void fetchMessages() {
        messageListener = chatRef.orderByChild(Constants.MessageFields.TIMESTAMP).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = parseMessage(snapshot);
                if (message != null) {
                    messages.add(message);
                    chatAdapter.submitList(new ArrayList<>(messages));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = parseMessage(snapshot);
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getMessageKey().equals(message.getMessageKey())) {
                        messages.set(i, message);
                        chatAdapter.submitList(new ArrayList<>(messages));
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String removeMessageKey = snapshot.getKey();
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getMessageKey().equals(removeMessageKey)) {
                        messages.remove(i);
                        chatAdapter.submitList(new ArrayList<>(messages));
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private Message parseMessage(@NonNull DataSnapshot snapshot) {
        String key = snapshot.getKey();
        String text = snapshot.child(Constants.MessageFields.MESSAGE).getValue(String.class);
        String uid = snapshot.child(Constants.MessageFields.DELIVER_UID).getValue(String.class);
        Long timestamp = snapshot.child(Constants.MessageFields.TIMESTAMP).getValue(Long.class);

        if (text == null || uid == null || timestamp == null)
            return null;

        Map<String, String> reactions = new HashMap<>();
        for (DataSnapshot entry : snapshot.child(Constants.MessageFields.REACTIONS).getChildren()) {
            reactions.put(entry.getKey(), entry.getValue(String.class));
        }

        Message.MessageType type = uid.equals(localUserSingleton.getUid()) ? Message.MessageType.SENDER : Message.MessageType.RECEIVER;
        return new Message(text, timestamp, type, uid, reactions, key);
    }

    private void insertMessage(String text) {
        String messageKey = chatRef.push().getKey();
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.MessageFields.DELIVER_UID, localUserSingleton.getUid());
        data.put(Constants.MessageFields.DELIVER_NAME, localUserSingleton.getFullName());
        data.put(Constants.MessageFields.MESSAGE, text);
        data.put(Constants.MessageFields.TIMESTAMP, timestamp);

        assert messageKey != null;
        chatRef.child(messageKey).setValue(data).addOnSuccessListener(unused -> logger.success("Message sent successfully!"))
                .addOnFailureListener(e -> logger.e(e.getMessage(), e));
    }

    private void removeMessage(Message message) {
        String messageKey = message.getMessageKey();
        if (messageKey == null || message.getMessageType().equals(Message.MessageType.RECEIVER)) {
            logger.e("Attempted to delete message with null key", null);
            return;
        }

        chatRef.child(messageKey).removeValue().addOnSuccessListener(
                unused -> SnackbarUtils.build(ChatActivity.this).setSnackbarType(SnackbarType.SUCCESS).setMessage("Message removed successfully!").show())
                .addOnFailureListener(e -> SnackbarUtils.build(ChatActivity.this).setSnackbarType(SnackbarType.ERROR)
                        .setMessage("Failed to remove message: " + e.getMessage()).show());
    }

    private void fetchUserProfilePicture(String uid, @NonNull Callback callback) {
        String path = "user_profile" + uid;
        String cachedUri = SharedPreferencesUtils.getDataByKey(this, path);

        if (cachedUri == null) {
            firebaseStorage.getReference().child(path).getDownloadUrl().addOnSuccessListener(uri -> {
                SharedPreferencesUtils.insertData(this, path, uri.toString());
                callback.onComplete(uri);
            }).addOnFailureListener(e -> logger.e(e.getMessage(), e));
        } else {
            callback.onComplete(Uri.parse(cachedUri));
        }
    }

    private void showMessageOptionsDialog(int position, @NonNull Message message) {
        new MaterialAlertDialogBuilder(this).setTitle("Message Options").setItems(new String[]{"Delete"}, (dialog, which) -> {
            if (which == 0)
                removeMessage(message);
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
        String messageKey = message.getMessageKey();
        chatRef.child(messageKey).child(Constants.MessageFields.REACTIONS).child(localUserSingleton.getUid()).setValue(emoji)
                .addOnSuccessListener(unused -> logger.success("Reaction added successfully!")).addOnFailureListener(e -> logger.e(e.getMessage(), e));

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
                logger.e(String.valueOf(error), new Throwable("Speech error"));
            }
        });

        Handler handler = new Handler();
        Runnable speechStart = () -> {
            speechRecognizerUtils.startListening();
            Toast.makeText(ChatActivity.this, "Hold to record!", Toast.LENGTH_SHORT).show();
        };

        binding.microphoneButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                handler.postDelayed(speechStart, 2000);
            else if (event.getAction() == MotionEvent.ACTION_UP)
                speechRecognizerUtils.stopListening();
            return false;
        });
    }

    @NonNull
    private String getChatRoomId(@NonNull String uid1, @NonNull String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @Override
    protected void onDestroy() {
        SharedPreferencesUtils.insertData(getApplicationContext(), Constants.CURRENT_CHAT, "none");
        if (chatRef != null && messageListener != null) {
            chatRef.removeEventListener(messageListener);
        }
        super.onDestroy();
    }

    interface Callback {
        void onComplete(Uri uri);
    }
}