package com.geoattend.utils;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

public class MapUtils {

    public static void applyPremiumDarkStyle(MapView mapView) {
        // Modern Premium Dark Slate - Improved visibility and contrast
        float[] matrix = {
            -0.7f, 0, 0, 0, 210, 
            0, -0.7f, 0, 0, 210, 
            0, 0, -0.7f, 0, 255,
            0, 0, 0, 1, 0        
        };
        
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(new ColorMatrix(matrix));
        mapView.getOverlayManager().getTilesOverlay().setColorFilter(filter);
        
        // HR Grid Overlay for "Blueprint" feel
        mapView.getOverlays().add(new GridOverlay());
    }

    public static void styleGeofenceCircle(org.osmdroid.views.overlay.Polygon circle) {
        circle.getFillPaint().setColor(0x102563EB); // Faint primary blue
        circle.getOutlinePaint().setColor(0x802563EB); // Semi-transparent blue stroke
        circle.getOutlinePaint().setStrokeWidth(2f);
    }

    public static class GridOverlay extends Overlay {
        private final Paint gridPaint;

        public GridOverlay() {
            gridPaint = new Paint();
            gridPaint.setColor(0x0AFFFFFF); // Reduced from 0x1A for better focus
            gridPaint.setStrokeWidth(0.5f);
            gridPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow) return;

            int width = mapView.getWidth();
            int height = mapView.getHeight();

            // Draw vertical lines
            for (int x = 0; x < width; x += 100) {
                canvas.drawLine(x, 0, x, height, gridPaint);
            }

            // Draw horizontal lines
            for (int y = 0; y < height; y += 100) {
                canvas.drawLine(0, y, width, y, gridPaint);
            }
        }
    }
}
