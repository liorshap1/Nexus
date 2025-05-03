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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

// ChatGPT
public class GenerateAvatarUtils {
    @NonNull
    public static String getInitials(@NonNull String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        String initials = "";
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            initials += parts[i].substring(0, 1).toUpperCase();
        }
        return initials;
    }

    public static int getRandomColor(@NonNull String key) {
        int[] colors = {Color.parseColor("#F44336"), // Red
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FFC107"), // Amber
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#795548") // Brown
        };
        int hash = Math.abs(key.hashCode());
        return colors[hash % colors.length];
    }

    @NonNull
    public static Bitmap generateInitialsAvatar(@NonNull String name, int sizeInDp, @NonNull Context context) {
        // Convert DP to pixels
        float scale = context.getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (sizeInDp * scale + 0.5f);

        // Get initials
        String initials = getInitials(name);

        // Create a bitmap
        Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw background circle with random color
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(getRandomColor(name)); // consistent for same name
        canvas.drawCircle(sizeInPx / 2f, sizeInPx / 2f, sizeInPx / 2f, paint);

        // Draw initials
        paint.setColor(Color.WHITE);
        paint.setTextSize(sizeInPx / 2f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        // Center text
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float y = sizeInPx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(initials, sizeInPx / 2f, y, paint);

        return bitmap;
    }

}
