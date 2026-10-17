package net.wastu.customtiles;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private TextView mTextRootStatus;
    private TextView mTextHardwareType;
    private TextView mTextSaturationValue;
    private SeekBar mSeekbarSaturation;
    private Button mBtnTestLightColor;
    private Button mBtnTestHbm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextRootStatus = findViewById(R.id.text_root_status);
        mTextHardwareType = findViewById(R.id.text_hardware_type);
        mTextSaturationValue = findViewById(R.id.text_saturation_value);
        mSeekbarSaturation = findViewById(R.id.seekbar_saturation);
        mBtnTestLightColor = findViewById(R.id.btn_test_light_color);
        mBtnTestHbm = findViewById(R.id.btn_test_hbm);

        int currentLevel = DisplayHardware.getTargetLightColorLevel(this);
        mSeekbarSaturation.setProgress(currentLevel);
        updateSaturationLabel(currentLevel);

        mSeekbarSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSaturationLabel(progress);
                if (fromUser) {
                    mExecutor.execute(() -> DisplayHardware.setTargetLightColorLevel(MainActivity.this, progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                Toast.makeText(MainActivity.this, "Light Color saturation set to " + progress + "%", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnTestLightColor.setOnClickListener(v -> mExecutor.execute(() -> {
            boolean active = DisplayHardware.isLightColorActive(MainActivity.this);
            DisplayHardware.setLightColorEnabled(MainActivity.this, !active);
            runOnUiThread(() -> {
                boolean nowActive = DisplayHardware.isLightColorActive(MainActivity.this);
                mBtnTestLightColor.setText(nowActive ? "Restore Normal Saturation" : "Toggle Light Colors Test");
            });
        }));

        mBtnTestHbm.setOnClickListener(v -> mExecutor.execute(() -> {
            boolean active = DisplayHardware.isHbmActive(MainActivity.this);
            DisplayHardware.setHbmEnabled(MainActivity.this, !active);
            runOnUiThread(() -> {
                boolean nowActive = DisplayHardware.isHbmActive(MainActivity.this);
                mBtnTestHbm.setText(nowActive ? "Turn Off Max Brightness" : "Toggle Max Brightness Test");
            });
        }));

        checkHardware();
    }

    private void updateSaturationLabel(int progress) {
        String note = (progress == 50) ? " (Recommended)" : (progress == 0) ? " (Monochrome)" : "";
        mTextSaturationValue.setText(progress + "%" + note);
    }

    private void checkHardware() {
        mExecutor.execute(() -> {
            boolean rooted = RootShell.isRootAvailable();
            String hardware = DisplayHardware.getHardwareDescription();
            boolean hbmActive = DisplayHardware.isHbmActive(this);
            boolean lightColorActive = DisplayHardware.isLightColorActive(this);

            runOnUiThread(() -> {
                mTextRootStatus.setText("Root: " + (rooted ? "Granted (KernelSU / Magisk / APatch)" : "Not Detected"));
                mTextRootStatus.setTextColor(rooted ? 0xFF81C784 : 0xFFE57373);
                mTextHardwareType.setText("Display Engine: " + hardware);
                mBtnTestHbm.setText(hbmActive ? "Turn Off Max Brightness" : "Toggle Max Brightness Test");
                mBtnTestLightColor.setText(lightColorActive ? "Restore Normal Saturation" : "Toggle Light Colors Test");
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkHardware();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}
