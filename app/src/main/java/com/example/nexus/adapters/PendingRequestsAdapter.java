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
import com.example.nexus.Constants;
import com.example.nexus.R;
import com.example.nexus.applogger.AppLogger;
import com.example.nexus.core.LocalUserSingleton;
import com.example.nexus.core.User;
import com.example.nexus.core.services.FetchUsersService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsViewHolder> {
    private final List<User> pendingUsersList;
    private final LocalUserSingleton localUser;
    private final FirebaseFirestore firestore;
    private final AppLogger logger;
    private final FetchUsersService fetchUsersService;

    public PendingRequestsAdapter(List<User> pendingUsersList, LocalUserSingleton localUser, FirebaseFirestore firestore, AppLogger appLogger, FetchUsersService fetchUsersService) {
        this.pendingUsersList = pendingUsersList;
        this.localUser = localUser;
        this.firestore = firestore;
        this.logger = appLogger;
        this.fetchUsersService = fetchUsersService;
    }

    @NonNull
    @Override
    public PendingRequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_friend_card, parent, false);
        return new PendingRequestsViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull PendingRequestsViewHolder holder, int position) {
        User pendingUser = pendingUsersList.get(position);
        String email = pendingUser.getEmail();
        String username = pendingUser.getFullName();
        String otherUserUid = pendingUser.getUid();

        holder.bind(username, email);
        holder.getAddButton().setOnClickListener(view -> {
            List<String> updatedFriends = new ArrayList<>(localUser.getFriends());
            updatedFriends.add(otherUserUid);

            pendingUsersList.remove(position);
            notifyItemRemoved(position);
            notifyDataSetChanged();

            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.UserFields.FRIENDS, updatedFriends);
            updates.put(Constants.UserFields.PENDING_REQUESTS, pendingUsersList);

            DocumentReference documentReference = firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUser.getUid());
            documentReference.update(updates).addOnSuccessListener(aVoid -> logger.success("Updated friends list")).addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));

            fetchUsersService.addToCachedUsers(pendingUser);
        });

        holder.getRemoveButton().setOnClickListener(view -> {
            pendingUsersList.remove(position);
            notifyItemRemoved(position);

            DocumentReference documentReference = firestore.collection(Constants.Firestore.USERS_COLLECTION).document(localUser.getUid());

            documentReference.update(Constants.UserFields.PENDING_REQUESTS, pendingUsersList).addOnSuccessListener(aVoid -> logger.success("Declined user successfully"))
                    .addOnFailureListener(e -> logger.e(e.getMessage(), e.getCause()));
        });
    }

    @Override
    public int getItemCount() {
        return pendingUsersList.size();
    }
}
