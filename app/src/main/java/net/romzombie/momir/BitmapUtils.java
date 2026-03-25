package net.romzombie.momir;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class BitmapUtils {

    // Standard 2-inch mini printer width in dots (often 384)
    public static final int PRINTER_WIDTH = 384;

    public static Bitmap createCardBitmap(String name, String manaCost, String typeLine, String oracleText, String power, String toughness) {
        // Prepare text paints
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(32);
        titlePaint.setFakeBoldText(true);

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(24);

        int padding = 16;
        int maxTextWidth = PRINTER_WIDTH - (padding * 2);

        // Layouts for multiline text
        StaticLayout titleLayout = new StaticLayout(name + "  " + manaCost, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        StaticLayout typeLayout = new StaticLayout(typeLine, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        StaticLayout oracleLayout = new StaticLayout(oracleText, bodyPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        
        int ptHeight = 0;
        StaticLayout ptLayout = null;
        if (power != null && !power.isEmpty() && toughness != null && !toughness.isEmpty()) {
            String powerToughness = power + "/" + toughness;
            ptLayout = new StaticLayout(powerToughness, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_OPPOSITE, 1.0f, 0.0f, false);
            ptHeight = ptLayout.getHeight() + padding;
        }

        // Calculate total height
        int totalHeight = padding 
                + titleLayout.getHeight() + padding 
                + typeLayout.getHeight() + padding 
                + oracleLayout.getHeight() + padding 
                + ptHeight + padding;

        // Ensure width is a multiple of 8 for easy byte packing
        int finalWidth = (PRINTER_WIDTH + 7) / 8 * 8; 

        Bitmap bitmap = Bitmap.createBitmap(finalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Fill white background
        canvas.drawColor(Color.WHITE);

        int currentY = padding;

        // Draw Title
        canvas.save();
        canvas.translate(padding, currentY);
        titleLayout.draw(canvas);
        canvas.restore();
        currentY += titleLayout.getHeight() + padding;

        // Draw Divider
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(padding, currentY - (padding / 2), finalWidth - padding, currentY - (padding / 2), linePaint);

        // Draw Type
        canvas.save();
        canvas.translate(padding, currentY);
        typeLayout.draw(canvas);
        canvas.restore();
        currentY += typeLayout.getHeight() + padding;
        
        canvas.drawLine(padding, currentY - (padding / 2), finalWidth - padding, currentY - (padding / 2), linePaint);

        // Draw Oracle Text
        canvas.save();
        canvas.translate(padding, currentY);
        oracleLayout.draw(canvas);
        canvas.restore();
        currentY += oracleLayout.getHeight() + padding;

        // Draw PT
        if (ptLayout != null) {
            canvas.save();
            canvas.translate(padding, currentY);
            ptLayout.draw(canvas);
            canvas.restore();
        }

        return bitmap;
    }

    /**
     * Converts a 32-bit ARGB bitmap into a 1-bit monochrome byte array.
     * Each bit represents a pixel: 1 for black (ink), 0 for white (no ink).
     */
    public static byte[] convertToMonochrome(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Assuming width is a multiple of 8
        int bytesPerLine = width / 8;
        int totalBytes = bytesPerLine * height;
        byte[] monoData = new byte[totalBytes];

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                
                // Extract RGB
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Simple luminance threshold (0.299*R + 0.587*G + 0.114*B)
                int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                // If luminance is dark, bit is 1. If light, bit is 0.
                if (luminance < 128) {
                    int byteIndex = (y * bytesPerLine) + (x / 8);
                    int bitPosition = 7 - (x % 8); // MSB First
                    monoData[byteIndex] |= (1 << bitPosition);
                }
            }
        }

        return monoData;
    }
}
