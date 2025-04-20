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

/**
 * Contract for FetchUsersService, allowing enqueueing and retrieval of users,
 * as well as registering listeners for fetch completion events.
 */
public interface IFetchUsersService {
    /**
     * Add a user UID to the fetch queue.
     *
     * @param uid
     *            Firebase UID of user
     */
    void enqueueUser(String uid);

    /** Remove all pending UIDs and clear cached users. */
    void clear();

    /**
     * Get all fetched users (unmodifiable list).
     *
     * @return list of users
     */
    List<User> getFetchedUsers();

    /**
     * Get a specific fetched user by UID.
     *
     * @param uid
     *            Firebase UID of user
     * @return User or null if not fetched
     */
    @Nullable
    User getUser(String uid);

    /**
     * Check if a user has already been fetched.
     *
     * @param uid
     *            Firebase UID of user
     * @return true if in cache
     */
    boolean isUserFetched(String uid);

    /**
     * Register a listener to be notified when new users are fetched.
     *
     * @param listener
     *            callback invoked on main thread
     */
    void addFetchListener(FetchListener listener);

    /**
     * Unregister a previously added listener.
     *
     * @param listener
     *            callback to remove
     */
    void removeFetchListener(FetchListener listener);

    /** Listener callback for fetch completion events. */
    interface FetchListener {
        /**
         * Called with the current list of fetched users.
         *
         * @param users
         *            snapshot of fetched users
         */
        void onUsersFetched(List<User> users);
    }
}
