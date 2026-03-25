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
import java.util.List;


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
                        try {
                            if (!mBluetoothAdapter.isEnabled() || !mBluetoothSocket.isConnected()) {
                                disconnected = true;
                            } else {
                                // Active probe: InputStream.available() will throw
                                // IOException if the remote device is unreachable
                                mBluetoothSocket.getInputStream().available();
                            }
                        } catch (IOException e) {
                            Log.w(TAG, "Polling detected disconnection", e);
                            disconnected = true;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
                HttpURLConnection conn = null;
                try {
                    // HARDCODED CPCL PAYLOAD TEST
                    // format: ! {offset} {200 dpi} {200 dpi} {y-height} {qty}
                    // T {font} {size} {x} {y} {data}
                    OutputStream os = mBluetoothSocket.getOutputStream();
                    
                    String cpcl = "! 0 200 200 400 1\r\n" +
                                  "T 5 0 20 20 IF YOU CAN READ THIS\r\n" +
                                  "T 5 0 20 60 THE PRINTER SUPPORTS CPCL\r\n" +
                                  "T 5 0 20 100 HELLO WORLD\r\n" +
                                  "FORM\r\n" +
                                  "PRINT\r\n";

                    os.write(cpcl.getBytes("US-ASCII"));
                    os.flush();
                    URL url = new URL("https://api.scryfall.com/cards/random?q=type%3Acreature%20cmc%3D" + manaValue);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "InstantMomir/1.0");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        JSONObject cardData = new JSONObject(response.toString());
                        String name = cardData.getString("name");
                        String manaCost = cardData.optString("mana_cost", "");
                        String typeLine = cardData.optString("type_line", "");
                        String oracleText = cardData.optString("oracle_text", "");
                        String power = cardData.optString("power", "");
                        String toughness = cardData.optString("toughness", "");

                        android.graphics.Bitmap cardBitmap = BitmapUtils.createCardBitmap(MainActivity.this, name, manaCost, typeLine, oracleText, power, toughness);
                        byte[] monochromeData = BitmapUtils.convertToMonochrome(cardBitmap);
                        final boolean success = sendPoooliPrintJob(monochromeData);

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
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private boolean sendPoooliPrintJob(byte[] monoData) {
        try {
            int widthPx = 384;
            int heightPx = monoData.length / (widthPx / 8);
            Log.d(TAG, "Printing " + widthPx + "x" + heightPx + " image (" + monoData.length + " bytes)");
            
            OutputStream os = mBluetoothSocket.getOutputStream();
            
            // Build uncompressed raster packet (GS v 0 mode 0x00)
            List<byte[]> packets = PoooliProtocol.buildPrintPackets(monoData, widthPx, heightPx);
            
            for (byte[] packet : packets) {
                os.write(packet);
                os.flush();
            }
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
            // Use Insecure RFCOMM socket to bypass newer Android Bluetooth encryption.
            // Many legacy generic thermal printers advertise encryption but fail to decrypt,
            // resulting in silently dropped payloads.
            mBluetoothSocket = mBluetoothDevice
                    .createInsecureRfcommSocketToServiceRecord(applicationUUID);
            
            // Fallback to reflection method if insecure connection fails
            // mBluetoothSocket = (BluetoothSocket) mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mBluetoothDevice, 1);
            
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
