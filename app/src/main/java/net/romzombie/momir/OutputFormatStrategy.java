package net.romzombie.momir;

import android.content.Context;
import org.json.JSONObject;

public interface OutputFormatStrategy {
    byte[] format(Context context, JSONObject cardData) throws Exception;
    int getWidth();
    int getHeight();
}
