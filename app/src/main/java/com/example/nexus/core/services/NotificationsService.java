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
package com.example.nexus.core.services;

import static com.example.nexus.Constants.FOREGROUND_NOTIFICATION_ID;
import static com.example.nexus.Constants.NOTIFICATION_ID;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.nexus.Constants;
import com.example.nexus.core.Message;
import com.example.nexus.core.User;
import com.example.nexus.utils.SharedPreferencesUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;

import javax.inject.Inject;

public class NotificationsService extends Service {
    private final Gson gson = new Gson();
    private ChildEventListener messageListener;
    private DatabaseReference messagesRef;
    private final IBinder binder = new NotificationsServiceBinder();
    @Inject
    FirebaseDatabase firebaseDatabase;
    @Inject
    FirebaseAuth firebaseAuth;

    public NotificationsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class NotificationsServiceBinder extends Binder {
        public NotificationsService getService() {
            return NotificationsService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        NotificationCompat.Builder foregroundBuilder = new NotificationCompat.Builder(this, Constants.CHAT_CHANNEL_ID).setContentTitle("Notification Service Running")
                .setContentText("Listening for new messages").setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundBuilder.build());
        ArrayList<String> chatsIds = intent.getStringArrayListExtra(Constants.CHATS_INTENT);
        if (chatsIds != null && !chatsIds.isEmpty()) {
            for (String chatRoomId : chatsIds) {
                startServiceListener(chatRoomId);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelTitle = "Nexus";
            String channelDescription = "Nexus Notifications Center";
            int channelImportance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel("chat_channel_id", channelTitle, channelImportance);
            notificationChannel.setDescription(channelDescription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void createNotification(Message message) {
        // Untested
        var sensitizedUserJson = SharedPreferencesUtils.getDataByKey(getApplicationContext(), "user_" + message.getMessageDeliverUid());
        User user = gson.fromJson(sensitizedUserJson, User.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "chat_channel_id").setContentTitle(user.getFullName())
                .setContentText("Sent: " + message.getText()).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ChatNotificationService", "POST_NOTIFICATIONS permission not granted");
                return;
            }
        }

        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
    }

    private void startServiceListener(String chatId) {
        // Reference to the messages node for the given chat room.
        messagesRef = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DATABASE.CHATS).child(chatId).child(Constants.FIREBASE_DATABASE.MESSAGES);

        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Trigger a notification if the user is not in the current chat.
                String currentChatId = SharedPreferencesUtils.getDataByKey(getApplicationContext(), Constants.CURRENT_CHAT);
                if (!currentChatId.equals(chatId)) {
                    String senderUid = snapshot.child(Constants.MessageFields.SENDER_UID).getValue(String.class);
                    if (senderUid != null && !senderUid.equals(firebaseAuth.getCurrentUser().getUid())) {
                        String message = snapshot.child(Constants.MessageFields.MESSAGE).getValue(String.class);
                        createNotification(new Message(message, null, null, senderUid));
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        messagesRef.addChildEventListener(messageListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messagesRef.removeEventListener(messageListener);
    }
}
