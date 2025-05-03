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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexus.R;
import com.example.nexus.core.User;

public class OpenChatsHolder extends RecyclerView.ViewHolder {
    private final TextView username;
    private final TextView email;
    private final ImageView profilePicture;
    public OpenChatsHolder(@NonNull View itemView) {
        super(itemView);

        username = itemView.findViewById(R.id.user_name);
        email = itemView.findViewById(R.id.user_email);
        profilePicture = itemView.findViewById(R.id.profileImage);

        if (username == null || email == null) {
            Log.e("VH_BIND", "ViewHolder failed to find one of the text views!");
        }
    }

    public void bind(@NonNull User user, String lastMessage) {
        this.username.setText(user.getFullName());
        this.email.setText(lastMessage);
    }

    public TextView getEmail() {
        return email;
    }

    public ImageView getProfilePicture() {
        return profilePicture;
    }
}
