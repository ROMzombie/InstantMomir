package net.romzombie.momir;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements Runnable {

    protected static final String TAG = "MainActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    Button mScan;

    BluetoothAdapter mBluetoothAdapter;
    private UUID applicationUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog mBluetoothConnectProgressDialog;
    private BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;
    private boolean isAutoReconnecting = false;

    private Handler pollingHandler = new Handler();
    private Runnable pollingRunnable;

    TextView stat;
    LinearLayout layout;
    GridLayout gridManaValues;

    private void updateUIForDisconnect() {
        stat.setText("Disconnected");
        stat.setTextColor(Color.rgb(199, 59, 59));
        mScan.setEnabled(true);
        mScan.setText("Connect");
        mScan.setBackgroundResource(R.color.colorAccent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (stat != null && "Connected".equals(stat.getText().toString())) {
                    if (mBluetoothAdapter != null && mBluetoothSocket != null) {
                        boolean disconnected = false;
                        if (!mBluetoothAdapter.isEnabled()) {
                            disconnected = true;
                        } else {
                            try {
                                // Actively write a harmless NUL byte to detect a broken pipe
                                // isConnected() does not return false if the remote device turns off unexpectedly
                                mBluetoothSocket.getOutputStream().write(new byte[]{0});
                            } catch (Exception e) {
                                disconnected = true;
                            }
                        }

                        if (disconnected) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUIForDisconnect();
                                }
                            });
                        }
                    }
                }
                pollingHandler.postDelayed(this, 5000);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, 5000);

        stat = findViewById(R.id.bpstatus);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        layout = findViewById(R.id.layout);
        gridManaValues = findViewById(R.id.grid_mana_values);

        // Add 15 buttons programmatically to the GridLayout
        for (int i = 1; i <= 15; i++) {
            final int manaValue = i;
            Button button = new Button(this);
            button.setText(String.valueOf(manaValue));
            button.setTextSize(32);
            button.setBackgroundColor(Color.LTGRAY);
            button.setTextColor(Color.BLACK);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(16, 16, 16, 16);
            button.setLayoutParams(params);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchAndPrintCard(manaValue, (Button) v);
                }
            });
            gridManaValues.addView(button);
        }

        mScan = findViewById(R.id.Scan);
        mScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View mView) {

                if (mScan.getText().equals("Connect")) {
                    if (!checkBluetoothPermissions()) {
                        return;
                    }
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter == null) {
                        Toast.makeText(MainActivity.this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(
                                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent,
                                    REQUEST_ENABLE_BT);
                        } else {
                            connectOrShowDeviceList();
                        }
                    }

                } else if (mScan.getText().equals("Disconnect")) {
                    if (mBluetoothAdapter != null)
                        mBluetoothAdapter.disable();
                    try {
                        if (mBluetoothSocket != null) mBluetoothSocket.close();
                    } catch (Exception e) {}
                    updateUIForDisconnect();
                }
            }
        });
    }

    private void fetchAndPrintCard(final int manaValue, final Button clickedButton) {
        if (mBluetoothSocket == null || !mBluetoothSocket.isConnected()) {
            Toast.makeText(this, "Please connect to a printer first", Toast.LENGTH_SHORT).show();
            return;
        }

        clickedButton.setBackgroundColor(Color.YELLOW);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = "https://api.scryfall.com/cards/random?q=t%3Acreature+mv%3A" + manaValue;
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "InstantMomir/1.0");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    final int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }

                        JSONObject json = new JSONObject(sb.toString());
                        final String name = json.optString("name", "Unknown");
                        String manaCost = json.optString("mana_cost", "");
                        String typeLine = json.optString("type_line", "");
                        String oracleText = json.optString("oracle_text", "");
                        String power = json.optString("power", "");
                        String toughness = json.optString("toughness", "");

                        final boolean success = printCard(name, manaCost, typeLine, oracleText, power, toughness);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (success) {
                                    clickedButton.setBackgroundColor(Color.GREEN);
                                    Toast.makeText(MainActivity.this, "Successfully printed " + name, Toast.LENGTH_SHORT).show();
                                } else {
                                    clickedButton.setBackgroundColor(Color.RED);
                                    Toast.makeText(MainActivity.this, "Failed to send data to printer", Toast.LENGTH_SHORT).show();
                                }
                                resetButtonColor(clickedButton);
                            }
                        });
                    } else if (responseCode == 404) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clickedButton.setBackgroundColor(Color.RED);
                                Toast.makeText(MainActivity.this, "No creature found with MV " + manaValue, Toast.LENGTH_SHORT).show();
                                resetButtonColor(clickedButton);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clickedButton.setBackgroundColor(Color.RED);
                                Toast.makeText(MainActivity.this, "Failed to fetch card metadata (HTTP " + responseCode + ")", Toast.LENGTH_SHORT).show();
                                resetButtonColor(clickedButton);
                            }
                        });
                    }
                    conn.disconnect();
                } catch (final Exception e) {
                    Log.e(TAG, "Fetch Error", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            clickedButton.setBackgroundColor(Color.RED);
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            resetButtonColor(clickedButton);
                        }
                    });
                }
            }
        }).start();
    }

    private String sanitizeForPrinter(String input) {
        if (input == null) return "";
        // Replace common unicode typographic characters with ASCII equivalents
        input = input.replace("—", "-");
        input = input.replace("’", "'");
        input = input.replace("‘", "'");
        input = input.replace("“", "\"");
        input = input.replace("”", "\"");
        input = input.replace("•", "*");
        input = input.replace("½", "1/2");
        // Strip out any remaining non-ASCII characters
        return input.replaceAll("[^\\x00-\\x7F]", "");
    }

    private boolean printCard(String name, String manaCost, String typeLine, String oracleText, String power, String toughness) {
        try {
            OutputStream os = mBluetoothSocket.getOutputStream();

            // 1. Initialize Printer (ESC @) to clear buffer and reset modes
            os.write(new byte[]{0x1B, 0x40});

            String blank = "\n\n";
            String header = "===============================\n";
            String title = sanitizeForPrinter(name) + "  " + sanitizeForPrinter(manaCost) + "\n";
            String type = sanitizeForPrinter(typeLine) + "\n";
            String divider = "-------------------------------\n";
            String oracle = sanitizeForPrinter(oracleText) + "\n";

            String pt = "";
            if (!power.isEmpty() && !toughness.isEmpty()) {
                pt = "\n                " + sanitizeForPrinter(power) + "/" + sanitizeForPrinter(toughness) + "\n";
            }

            String footer = "===============================\n\n\n\n\n\n";

            // 2. Write out data
            os.write(blank.getBytes());
            os.write(header.getBytes());
            os.write(title.getBytes());
            os.write(divider.getBytes());
            os.write(type.getBytes());
            os.write(divider.getBytes());
            os.write(oracle.getBytes());
            os.write(pt.getBytes());
            os.write(footer.getBytes());

            // 3. Restore the printer-specific magic bytes that were in the original PrintDemo app
            // Setting height
            int gs = 29;
            os.write(intToByteArray(gs));
            int h = 150;
            os.write(intToByteArray(h));
            int n = 170;
            os.write(intToByteArray(n));

            // Setting Width
            int gs_width = 29;
            os.write(intToByteArray(gs_width));
            int w = 119;
            os.write(intToByteArray(w));
            int n_width = 2;
            os.write(intToByteArray(n_width));

            os.flush();

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Print Error", e);
            return false;
        }
    }

    private void resetButtonColor(final Button button) {
        button.postDelayed(new Runnable() {
            @Override
            public void run() {
                button.setBackgroundColor(Color.LTGRAY);
            }
        }, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        /* Terminate bluetooth connection and close all sockets opened */
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Exe ", e);
        }
    }

    public void onActivityResult(int mRequestCode, int mResultCode,
                                 Intent mDataIntent) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);

        switch (mRequestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (mResultCode == Activity.RESULT_OK) {
                    Bundle mExtra = mDataIntent.getExtras();
                    String mDeviceAddress = mExtra.getString("DeviceAddress");
                    Log.v(TAG, "Coming incoming address " + mDeviceAddress);
                    mBluetoothDevice = mBluetoothAdapter
                            .getRemoteDevice(mDeviceAddress);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this,
                            "Connecting...", mBluetoothDevice.getName() + " : "
                                    + mBluetoothDevice.getAddress(), true, false);
                    Thread mBlutoothConnectThread = new Thread(this);
                    mBlutoothConnectThread.start();
                }
                break;

            case REQUEST_ENABLE_BT:
                if (mResultCode == Activity.RESULT_OK) {
                    connectOrShowDeviceList();
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to any device", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void ListPairedDevices() {
        Set<BluetoothDevice> mPairedDevices = mBluetoothAdapter
                .getBondedDevices();
        if (mPairedDevices.size() > 0) {
            for (BluetoothDevice mDevice : mPairedDevices) {
                Log.v(TAG, "PairedDevices: " + mDevice.getName() + "  "
                        + mDevice.getAddress());
            }
        }
    }

    private void showDeviceList() {
        ListPairedDevices();
        Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
    }

    private void connectOrShowDeviceList() {
        SharedPreferences prefs = getSharedPreferences("MomirPrefs", MODE_PRIVATE);
        String savedMac = prefs.getString("LastPrinterMac", null);
        if (savedMac != null) {
            isAutoReconnecting = true;
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(savedMac);
            mBluetoothConnectProgressDialog = ProgressDialog.show(MainActivity.this,
                    "Connecting...", (mBluetoothDevice.getName() != null ? mBluetoothDevice.getName() : "Device") + " : "
                            + mBluetoothDevice.getAddress(), true, false);
            Thread mBlutoothConnectThread = new Thread(MainActivity.this);
            mBlutoothConnectThread.start();
        } else {
            showDeviceList();
        }
    }

    public void run() {
        try {
            mBluetoothSocket = mBluetoothDevice
                    .createRfcommSocketToServiceRecord(applicationUUID);
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothSocket.connect();
            mHandler.sendEmptyMessage(0);
        } catch (IOException eConnectException) {
            Log.d(TAG, "CouldNotConnectToSocket", eConnectException);
            closeSocket(mBluetoothSocket);
            mHandler.sendEmptyMessage(1);
            return;
        }
    }

    private void closeSocket(BluetoothSocket nOpenSocket) {
        try {
            nOpenSocket.close();
            Log.d(TAG, "SocketClosed");
        } catch (IOException ex) {
            Log.d(TAG, "CouldNotCloseSocket");
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mBluetoothConnectProgressDialog != null && mBluetoothConnectProgressDialog.isShowing()) {
                mBluetoothConnectProgressDialog.dismiss();
            }
            if (msg.what == 0) {
                stat.setText("");
                stat.setText("Connected");
                stat.setTextColor(Color.rgb(97, 170, 74));
                mScan.setText("Disconnect");
                mScan.setBackgroundColor(Color.rgb(97, 170, 74));
                if (mBluetoothDevice != null) {
                    SharedPreferences prefs = getSharedPreferences("MomirPrefs", MODE_PRIVATE);
                    prefs.edit().putString("LastPrinterMac", mBluetoothDevice.getAddress()).apply();
                }
                isAutoReconnecting = false;
            } else if (msg.what == 1) {
                if (isAutoReconnecting) {
                    isAutoReconnecting = false;
                    Toast.makeText(MainActivity.this, "Auto-reconnect failed, moving to device list", Toast.LENGTH_SHORT).show();
                    showDeviceList();
                } else {
                    Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private static final int REQUEST_PERMISSIONS_BT = 3;

    private boolean checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_PERMISSIONS_BT);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_BT) {
            boolean allGranted = true;
            if (grantResults.length == 0) return;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                mScan.performClick();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static byte intToByteArray(int value) {
        byte[] b = ByteBuffer.allocate(4).putInt(value).array();
        for (int k = 0; k < b.length; k++) {
            System.out.println("Selva  [" + k + "] = " + "0x"
                    + UnicodeFormatter.byteToHex(b[k]));
        }
        return b[3];
    }
}
