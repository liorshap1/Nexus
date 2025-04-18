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

import androidx.annotation.Nullable;
import com.example.nexus.core.User;
import java.util.List;

public interface IFetchUsersService {

  // Enqueue a user to be fetched later
  void enqueueUser(String uid);

  // Trigger an immediate fetch for a specific user
  void fetchUser(String uid);

  // Bulk fetch
  void fetchUsers();

  // Returns list of successfully fetched users
  List<User> getFetchedUsers();

  // Get a specific user, if already fetched
  @Nullable
  User getUser(String uid);

  // Check if a user is already fetched
  boolean isUserFetched(String uid);

  // Register a listener for updates (observer pattern)
  void addFetchListener(FetchListener listener);

  // Clear all internal states (cache, queue, etc.)
  void clear();

  // Interface for callback listeners
  interface FetchListener {
    void onUserFetched(User user);

    void onFetchFailed(String uid, Exception e);

    void onQueueUpdated(List<String> pendingUids);
  }
}
