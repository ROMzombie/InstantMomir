package net.romzombie.momir;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitmapUtils {

    // Standard 2-inch mini printer width in dots (often 384)
    public static final int PRINTER_WIDTH = 384;

    // Mana font codepoint mapping (Andrew Gioia "Mana" font)
    private static final Map<String, Character> MANA_GLYPHS = new HashMap<>();
    static {
        // Basic mana colors
        MANA_GLYPHS.put("W", '\ue600');
        MANA_GLYPHS.put("U", '\ue601');
        MANA_GLYPHS.put("B", '\ue602');
        MANA_GLYPHS.put("R", '\ue603');
        MANA_GLYPHS.put("G", '\ue604');
        // Colorless
        MANA_GLYPHS.put("C", '\ue904');
        // Generic mana 0–15
        MANA_GLYPHS.put("0", '\ue605');
        MANA_GLYPHS.put("1", '\ue606');
        MANA_GLYPHS.put("2", '\ue607');
        MANA_GLYPHS.put("3", '\ue608');
        MANA_GLYPHS.put("4", '\ue609');
        MANA_GLYPHS.put("5", '\ue60a');
        MANA_GLYPHS.put("6", '\ue60b');
        MANA_GLYPHS.put("7", '\ue60c');
        MANA_GLYPHS.put("8", '\ue60d');
        MANA_GLYPHS.put("9", '\ue60e');
        MANA_GLYPHS.put("10", '\ue60f');
        MANA_GLYPHS.put("11", '\ue610');
        MANA_GLYPHS.put("12", '\ue611');
        MANA_GLYPHS.put("13", '\ue612');
        MANA_GLYPHS.put("14", '\ue613');
        MANA_GLYPHS.put("15", '\ue614');
        MANA_GLYPHS.put("16", '\ue62a');
        MANA_GLYPHS.put("17", '\ue62b');
        MANA_GLYPHS.put("18", '\ue62c');
        MANA_GLYPHS.put("19", '\ue62d');
        MANA_GLYPHS.put("20", '\ue62e');
        // Special symbols
        MANA_GLYPHS.put("X", '\ue615');
        MANA_GLYPHS.put("Y", '\ue616');
        MANA_GLYPHS.put("Z", '\ue617');
        MANA_GLYPHS.put("S", '\ue619');  // Snow
        MANA_GLYPHS.put("T", '\ue61a');  // Tap
        MANA_GLYPHS.put("Q", '\ue61b');  // Untap
        MANA_GLYPHS.put("E", '\ue907');  // Energy
        MANA_GLYPHS.put("P", '\ue618');  // Phyrexian
    }

    // Regex to match {W}, {2}, {W/U}, {W/P}, etc.
    private static final Pattern MANA_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * Custom span that switches to the Mana icon typeface inline.
     */
    private static class ManaTypefaceSpan extends MetricAffectingSpan {
        private final Typeface manaTypeface;

        ManaTypefaceSpan(Typeface typeface) {
            this.manaTypeface = typeface;
        }

        @Override
        public void updateMeasureState(TextPaint tp) {
            tp.setTypeface(manaTypeface);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setTypeface(manaTypeface);
        }
    }

    /**
     * Replace mana tokens like {W}, {3}, {W/U} with Mana font glyphs.
     * Tokens that have no mapping are left as-is.
     */
    private static SpannableStringBuilder formatManaSymbols(String text, Typeface manaTypeface) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher matcher = MANA_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before this match
            sb.append(text, lastEnd, matcher.start());

            String token = matcher.group(1); // e.g. "W", "2", "W/U", "W/P"
            String glyphStr = resolveGlyph(token);

            if (glyphStr != null) {
                int start = sb.length();
                sb.append(glyphStr);
                int end = sb.length();
                sb.setSpan(new ManaTypefaceSpan(manaTypeface), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // Unknown symbol — keep original text
                sb.append(matcher.group(0));
            }

            lastEnd = matcher.end();
        }
        // Append remaining text
        sb.append(text, lastEnd, text.length());
        return sb;
    }

    /**
     * Resolve a mana token to its glyph character(s).
     * Handles simple tokens ("W", "3"), hybrid ("W/U"), and Phyrexian ("W/P").
     */
    private static String resolveGlyph(String token) {
        // Direct lookup first
        Character glyph = MANA_GLYPHS.get(token);
        if (glyph != null) {
            return String.valueOf(glyph);
        }

        // Handle hybrid/split tokens like "W/U", "2/W", "W/P"
        if (token.contains("/")) {
            String[] parts = token.split("/");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                Character partGlyph = MANA_GLYPHS.get(part);
                if (partGlyph != null) {
                    result.append(partGlyph);
                } else {
                    return null; // Unknown part — bail
                }
            }
            return result.toString();
        }

        return null;
    }

    public static Bitmap createCardBitmap(Context context, String name, String manaCost, String typeLine, String oracleText, String power, String toughness) {
        // Load the Mana icon typeface from assets
        Typeface manaTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/mana.ttf");

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

        // Line spacing: default 1.0x — section gaps handle overlap prevention
        float lineSpacingMult = 1.0f;
        float lineSpacingAdd = 0.0f;

        // Build spannable strings with mana icons
        SpannableStringBuilder titleSpan = formatManaSymbols(name + "  " + manaCost, manaTypeface);
        SpannableStringBuilder typeSpan = formatManaSymbols(typeLine, manaTypeface);
        SpannableStringBuilder oracleSpan = formatManaSymbols(oracleText, manaTypeface);

        // Layouts for multiline text
        StaticLayout titleLayout = new StaticLayout(titleSpan, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, lineSpacingMult, lineSpacingAdd, false);
        StaticLayout typeLayout = new StaticLayout(typeSpan, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, lineSpacingMult, lineSpacingAdd, false);
        StaticLayout oracleLayout = new StaticLayout(oracleSpan, bodyPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, lineSpacingMult, lineSpacingAdd, false);

        int ptHeight = 0;
        StaticLayout ptLayout = null;
        if (power != null && !power.isEmpty() && toughness != null && !toughness.isEmpty()) {
            String powerToughness = power + "/" + toughness;
            ptLayout = new StaticLayout(powerToughness, titlePaint, maxTextWidth, Layout.Alignment.ALIGN_OPPOSITE, lineSpacingMult, lineSpacingAdd, false);
            ptHeight = ptLayout.getHeight() + padding;
        }

        // Extra gap after each divider so first line of each section doesn't overlap
        int sectionGap = padding;

        // Calculate total height — extra padding at bottom for whitespace
        int totalHeight = padding
                + titleLayout.getHeight() + padding + sectionGap
                + typeLayout.getHeight() + padding + sectionGap
                + oracleLayout.getHeight() + padding
                + ptHeight + padding
                + padding + padding; // extra bottom padding

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
        currentY += sectionGap; // extra gap after divider

        // Draw Type
        canvas.save();
        canvas.translate(padding, currentY);
        typeLayout.draw(canvas);
        canvas.restore();
        currentY += typeLayout.getHeight() + padding;

        canvas.drawLine(padding, currentY - (padding / 2), finalWidth - padding, currentY - (padding / 2), linePaint);
        currentY += sectionGap; // extra gap after divider

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
