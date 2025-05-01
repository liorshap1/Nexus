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
package com.example.nexus.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexus.R;
import com.example.nexus.core.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {
    private final List<Message> messageList;
    private OnMessageLongClickListener longClickListener;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getMessageType() == Message.MessageType.SENDER ? R.layout.sender_message : R.layout.receiver_message;
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ChatHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message.getText());

        holder.setOnMessageLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(position, message);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMessages(List<Message> messages) {
        messageList.clear();
        messageList.addAll(messages);
        notifyDataSetChanged();
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(int position, Message message);
    }
}
