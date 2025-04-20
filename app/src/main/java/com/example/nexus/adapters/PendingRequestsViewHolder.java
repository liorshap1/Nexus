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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nexus.R;
import com.google.android.material.button.MaterialButton;

public class PendingRequestsViewHolder extends RecyclerView.ViewHolder {
    private final TextView username;
    private final TextView email;
    private final MaterialButton addButton;
    private final MaterialButton removeButton;

    public PendingRequestsViewHolder(@NonNull View itemView) {
        super(itemView);

        username = itemView.findViewById(R.id.user_name);
        email = itemView.findViewById(R.id.user_email);
        addButton = itemView.findViewById(R.id.addButton);
        removeButton = itemView.findViewById(R.id.removeButton);
    }

    public void bind(String username, String email) {
        this.username.setText(username);
        this.email.setText(email);
    }

    public MaterialButton getAddButton() {
        return addButton;
    }

    public MaterialButton getRemoveButton() {
        return removeButton;
    }
}
