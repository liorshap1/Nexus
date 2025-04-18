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
package com.example.nexus.core;

import java.util.ArrayList;

public class LocalUserSingleton extends BaseUser {
  private ArrayList<String> friends;

  public LocalUserSingleton() {
    super();
  }

  public void initializeLocalUserSingleton(
      String uid,
      String firstName,
      String secondName,
      String email,
      String profilePicture,
      ArrayList<String> friends) {
    setUid(uid);
    setFirstName(firstName);
    setSecondName(secondName);
    setEmail(email);
    setProfilePicture(profilePicture);
    this.friends = friends;
  }

  public ArrayList<String> getFriends() {
    return friends;
  }

  public void setFriends(ArrayList<String> friends) {
    this.friends = friends;
  }
}
