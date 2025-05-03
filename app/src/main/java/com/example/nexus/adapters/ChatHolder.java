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

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexus.R;

import java.util.HashSet;
import java.util.Map;

public class ChatHolder extends RecyclerView.ViewHolder {
    private final TextView message;
    private final TextView reactions;

    public ChatHolder(@NonNull View itemView) {
        super(itemView);

        message = itemView.findViewById(R.id.text);
        reactions = itemView.findViewById(R.id.reactions);
    }

    public void bind(String text, @Nullable Map<String, String> reactionsMap) {
        message.setText(text);
        if (reactionsMap != null && !reactionsMap.isEmpty()) {
            StringBuilder reactionText = new StringBuilder();
            for (String emoji : new HashSet<>(reactionsMap.values())) {
                reactionText.append(emoji).append(" ");
            }
            Log.d("ARAB", reactionText.toString());
            reactions.setText(reactionText.toString().trim());
            reactions.setVisibility(View.VISIBLE);
        } else {
            reactions.setText("");
            reactions.setVisibility(View.GONE);
        }
    }

    public void setOnMessageLongClickListener(View.OnLongClickListener listener) {
        itemView.setOnLongClickListener(listener);
    }
}
