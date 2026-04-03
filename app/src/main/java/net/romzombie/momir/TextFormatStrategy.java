package net.romzombie.momir;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONObject;

public class TextFormatStrategy implements OutputFormatStrategy {

    private int width = 0;
    private int height = 0;

    @Override
    public byte[] format(Context context, JSONObject cardData) throws Exception {
        String name = cardData.getString("name");
        String manaCost = cardData.optString("mana_cost", "");
        String typeLine = cardData.optString("type_line", "");
        String oracleText = cardData.optString("oracle_text", "");
        String power = cardData.optString("power", "");
        String toughness = cardData.optString("toughness", "");

        Bitmap cardBitmap = BitmapUtils.createCardBitmap(context, name, manaCost, typeLine, oracleText, power, toughness);
        this.width = cardBitmap.getWidth();
        this.height = cardBitmap.getHeight();
        
        return BitmapUtils.convertToMonochrome(cardBitmap);
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
