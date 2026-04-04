package net.romzombie.momir;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.RadioButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsActivityTest {

    private SettingsActivity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SettingsActivity.class).create().visible().get();
    }

    @Test
    public void testInitialState_DefaultPrefs() {
        SharedPreferences prefs = activity.getSharedPreferences("MomirPrefs", Context.MODE_PRIVATE);
        // By default, text format should be selected
        assertEquals("TextFormat", prefs.getString("OutputFormatStrategy", "TextFormat"));

        RadioButton rbTextFormat = activity.findViewById(R.id.rb_text_format);
        RadioButton rbImageFormat = activity.findViewById(R.id.rb_image_format);

        assertTrue(rbTextFormat.isChecked());
        assertFalse(rbImageFormat.isChecked());
    }

    @Test
    public void testSaveImageFormat() {
        RadioButton rbImageFormat = activity.findViewById(R.id.rb_image_format);
        Button btnSave = activity.findViewById(R.id.btn_save_settings);

        // Select image format and save
        rbImageFormat.setChecked(true);
        btnSave.performClick();

        SharedPreferences prefs = activity.getSharedPreferences("MomirPrefs", Context.MODE_PRIVATE);
        assertEquals("ImageFormat", prefs.getString("OutputFormatStrategy", ""));
        assertTrue(activity.isFinishing());
    }
}
