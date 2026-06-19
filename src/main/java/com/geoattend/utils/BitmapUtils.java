package com.geoattend.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class BitmapUtils {
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), 
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
