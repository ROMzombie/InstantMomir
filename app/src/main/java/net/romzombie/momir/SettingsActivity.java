package net.romzombie.momir;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgThemeMode;
    private RadioButton rbThemeSystem;
    private RadioButton rbThemeLight;
    private RadioButton rbThemeDark;

    private RadioGroup rgFormatStrategy;
    private RadioButton rbTextFormat;
    private RadioButton rbImageFormat;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgThemeMode = findViewById(R.id.rg_theme_mode);
        rbThemeSystem = findViewById(R.id.rb_theme_system);
        rbThemeLight = findViewById(R.id.rb_theme_light);
        rbThemeDark = findViewById(R.id.rb_theme_dark);

        rgFormatStrategy = findViewById(R.id.rg_format_strategy);
        rbTextFormat = findViewById(R.id.rb_text_format);
        rbImageFormat = findViewById(R.id.rb_image_format);
        btnSave = findViewById(R.id.btn_save_settings);

        SharedPreferences prefs = getSharedPreferences("MomirPrefs", MODE_PRIVATE);
        
        String savedTheme = prefs.getString("ThemeMode", "system");
        if ("light".equals(savedTheme)) {
            rbThemeLight.setChecked(true);
        } else if ("dark".equals(savedTheme)) {
            rbThemeDark.setChecked(true);
        } else {
            rbThemeSystem.setChecked(true);
        }

        String savedStrategy = prefs.getString("OutputFormatStrategy", "TextFormat");

        if ("ImageFormat".equals(savedStrategy)) {
            rbImageFormat.setChecked(true);
        } else {
            rbTextFormat.setChecked(true);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedTheme = "system";
                int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                
                if (rbThemeLight.isChecked()) {
                    selectedTheme = "light";
                    nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                } else if (rbThemeDark.isChecked()) {
                    selectedTheme = "dark";
                    nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                }

                String selectedStrategy = "TextFormat";
                if (rbImageFormat.isChecked()) {
                    selectedStrategy = "ImageFormat";
                }

                SharedPreferences.Editor editor = getSharedPreferences("MomirPrefs", MODE_PRIVATE).edit();
                editor.putString("ThemeMode", selectedTheme);
                editor.putString("OutputFormatStrategy", selectedStrategy);
                editor.apply();

                AppCompatDelegate.setDefaultNightMode(nightMode);

                Toast.makeText(SettingsActivity.this, "Settings Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
