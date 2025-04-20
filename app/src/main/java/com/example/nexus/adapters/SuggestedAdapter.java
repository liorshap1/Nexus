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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nexus.R;
import com.example.nexus.core.User;
import java.util.List;

public class SuggestedAdapter extends RecyclerView.Adapter<SuggestedViewHolder> {
    private final List<User> usersList;

    public SuggestedAdapter(List<User> usersList) {
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public SuggestedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new SuggestedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestedViewHolder holder, int position) {
        String email = usersList.get(position).getEmail();
        String username = usersList.get(position).getFullName();

        holder.bind(username, email);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }
}
