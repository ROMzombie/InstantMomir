package net.romzombie.momir;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageFormatStrategy implements OutputFormatStrategy {

    private int width = 0;
    private int height = 0;

    @Override
    public byte[] format(Context context, JSONObject cardData) throws Exception {
        String imageUrl = null;
        if (cardData.has("image_uris")) {
            JSONObject imageUris = cardData.getJSONObject("image_uris");
            if (imageUris.has("normal")) {
                imageUrl = imageUris.getString("normal");
            } else if (imageUris.has("large")) {
                imageUrl = imageUris.getString("large");
            }
        }
        
        // Handle double-faced cards which might have image_uris under card_faces
        if (imageUrl == null && cardData.has("card_faces")) {
            JSONObject face = cardData.getJSONArray("card_faces").getJSONObject(0);
            if (face.has("image_uris")) {
                JSONObject imageUris = face.getJSONObject("image_uris");
                if (imageUris.has("normal")) {
                    imageUrl = imageUris.getString("normal");
                } else if (imageUris.has("large")) {
                    imageUrl = imageUris.getString("large");
                }
            }
        }

        if (imageUrl == null) {
            throw new Exception("No suitable image found for card.");
        }

        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "InstantMomir/1.0");
        conn.setRequestProperty("Accept", "image/jpeg, image/png");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        Bitmap srcBitmap = null;
        try (InputStream in = conn.getInputStream()) {
            srcBitmap = BitmapFactory.decodeStream(in);
        } finally {
            conn.disconnect();
        }

        if (srcBitmap == null) {
            throw new Exception("Failed to decode image from URL");
        }

        // Scale to PRINTER_WIDTH width, preserving aspect ratio
        int targetWidth = BitmapUtils.PRINTER_WIDTH;
        float ratio = (float) targetWidth / srcBitmap.getWidth();
        int targetHeight = Math.round(srcBitmap.getHeight() * ratio);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true);
        if (scaledBitmap != srcBitmap) {
            srcBitmap.recycle();
        }

        // Apply high contrast filter
        int bottomPadding = 48 + 24; // equivalent to padding + padding + padding from BitmapUtils, plus 24 extra
        int totalHeight = targetHeight + bottomPadding;
        Bitmap contrastBitmap = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(contrastBitmap);
        canvas.drawColor(android.graphics.Color.WHITE);
        Paint paint = new Paint();
        
        ColorMatrix cm = new ColorMatrix();
        float contrast = 2.0f; // High contrast
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        cm.set(new float[] {
            contrast, 0, 0, 0, translate,
            0, contrast, 0, 0, translate,
            0, 0, contrast, 0, translate,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);
        scaledBitmap.recycle();

        this.width = contrastBitmap.getWidth();
        this.height = contrastBitmap.getHeight();

        byte[] monoData = BitmapUtils.convertToMonochrome(contrastBitmap);
        contrastBitmap.recycle();

        return monoData;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
