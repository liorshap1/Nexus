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
package com.example.nexus;

import android.app.Application;

import com.airbnb.lottie.Lottie;
import com.airbnb.lottie.LottieConfig;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyNexusApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Lottie.initialize(new LottieConfig.Builder().setEnableSystraceMarkers(true)
                // Optional: customize network fetcher and cache here
                .build());
        final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseApp.initializeApp(getApplicationContext());
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder(firebaseFirestore.getFirestoreSettings())
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build();
        firebaseFirestore.setFirestoreSettings(settings);
    }
}
