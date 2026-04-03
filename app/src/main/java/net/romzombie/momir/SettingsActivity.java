package net.romzombie.momir;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private RadioGroup rgFormatStrategy;
    private RadioButton rbTextFormat;
    private RadioButton rbImageFormat;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgFormatStrategy = findViewById(R.id.rg_format_strategy);
        rbTextFormat = findViewById(R.id.rb_text_format);
        rbImageFormat = findViewById(R.id.rb_image_format);
        btnSave = findViewById(R.id.btn_save_settings);

        SharedPreferences prefs = getSharedPreferences("MomirPrefs", MODE_PRIVATE);
        String savedStrategy = prefs.getString("OutputFormatStrategy", "TextFormat");

        if ("ImageFormat".equals(savedStrategy)) {
            rbImageFormat.setChecked(true);
        } else {
            rbTextFormat.setChecked(true);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedStrategy = "TextFormat";
                if (rbImageFormat.isChecked()) {
                    selectedStrategy = "ImageFormat";
                }

                SharedPreferences.Editor editor = getSharedPreferences("MomirPrefs", MODE_PRIVATE).edit();
                editor.putString("OutputFormatStrategy", selectedStrategy);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "Settings Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
