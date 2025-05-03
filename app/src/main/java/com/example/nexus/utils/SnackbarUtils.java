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
package com.example.nexus.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nexus.R;
import com.google.android.material.snackbar.Snackbar;

interface ISnackUtils {
    ISnackUtils setMessage(String text);

    void show();
}

public class SnackbarUtils implements ISnackUtils {
    private Context context;
    private String text;

    private SnackbarUtils(Context context) {
        this.context = context;
    }

    public static SnackbarUtils build(Context context) {
        return new SnackbarUtils(context);
    }

    @Override
    public SnackbarUtils setMessage(String text) {
        this.text = text;
        return this;
    }

    @Override
    public void show() {
        if (!(context instanceof Activity))
            return;

        Activity activity = (Activity) context;
        View rootView = activity.findViewById(android.R.id.content);

        Snackbar snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT);

        // Inflate custom view
        View customView = LayoutInflater.from(context).inflate(R.layout.custom_snackbar, null);

        TextView title = customView.findViewById(R.id.snackbar_title);
        TextView message = customView.findViewById(R.id.snackbar_message);
        ImageView icon = customView.findViewById(R.id.snackbar_icon);

        title.setText(text);
        message.setText(text); // from setMessage()

        @SuppressLint("RestrictedApi")
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setPadding(0, 0, 0, 0);
        layout.setBackgroundColor(Color.TRANSPARENT);
        layout.removeAllViews();
        layout.addView(customView);

        snackbar.show();
    }
}
