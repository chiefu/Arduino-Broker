package pl.chiefu.arduino_broker.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {

    //----------------------------------------------------------------------------------------------
    // Deklaracje
    //----------------------------------------------------------------------------------------------
    public final static int RECIEVE_MESSAGE = 1;
    public final static int BLUETOOTH_ENABLE = 2;
    public final static int HTTP_RESPONSE= 3;
    public final static int PRINT = 4;

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String NAME = "Arduino Broker";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String ADDRESS = "98:D3:31:B2:03:B4";

    private BluetoothAdapter adapter = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;

    private Handler handler;
    private StringBuilder builder = new StringBuilder();

    private ScrollView scrollView;
    private TextView textView;
    private Button buttonStart;

    ConnectionThread connectionThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Automatyczne przewijanie
         */
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        textView =(TextView) findViewById(R.id.textView);
        buttonStart = (Button) findViewById(R.id.buttonStart);

        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        builder.append((String) msg.obj);
                        int endOfLineIndex = builder.indexOf("\r\n");
                        if (endOfLineIndex > 0) {
                            String sbprint = builder.substring(0, endOfLineIndex);
                            builder.delete(0, builder.length());
                            Log.d(TAG, getString(R.string.odebrano_bt) + ": " + sbprint);
                            textView.append(getString(R.string.odebrano_bt) + ": " + sbprint + "\n");
                            /*
                            scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                }
                            });
                            */

                            if(isConnected()){
                                String url = "http://home-chiefu.rhcloud.com/logger/" + sbprint;
                                Log.d(TAG, "URL: " + url);
                                new HttpAsyncTask().execute(url);
                            }
                        }
                        break;
                    case BLUETOOTH_ENABLE:
                        if((Boolean)msg.obj) {
                            buttonStart.setText(R.string.stop);
                        }
                        break;
                    case HTTP_RESPONSE:
                        if(msg.obj != null && msg.obj instanceof String) {
                            String response = msg.obj.toString();
                            if(!response.isEmpty()) {
                                try {
                                    JSONObject json = new JSONObject(response);
                                    Log.d(TAG, getString(R.string.odebrano_http) + json.toString());
                                    //Toast.makeText(getBaseContext(), (json.getBoolean("error") ? "Błąd! " : "") + json.getString("message") , Toast.LENGTH_LONG).show();
                                    textView.append(getString(R.string.odebrano_http) + ":\n"  + json.toString(4) + "\n");
                                } catch (Exception ex) {
                                    Log.e(TAG, ex.getLocalizedMessage());
                                }
                            }
                        }
                        break;
                    case PRINT:
                        if(msg.obj != null && msg.obj instanceof String) {
                            textView.append(msg.obj + "\n");
                        }
                }
            }
        };
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void buttonStartClick(View view) {

        if(buttonStart.getText().equals(getString(R.string.start))) {
            if(connectionThread != null) {
                connectionThread.cancel(false);
            }
            adapter = BluetoothAdapter.getDefaultAdapter();
            new WaitForBluetoothThread().start();

            if (BrokerService.isServiceAlive()) {
                stopService(new Intent(MainActivity.this, BrokerService.class));
            } else {
                startService(new Intent(MainActivity.this, BrokerService.class));
            }
        } else {
            if(connectionThread != null) {
                connectionThread.cancel(true);
            }
            buttonStart.setText(R.string.start);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Metody prywatne
    //----------------------------------------------------------------------------------------------
    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Komunikacja z serwerem HTTP
     */
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            handler.obtainMessage(HTTP_RESPONSE, result).sendToTarget();
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;

    }

    private boolean isConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    public static String GET(String url){
        String result = "";
        try {
            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            // receive response as inputStream
            InputStream inputStream = httpResponse.getEntity().getContent();
            // convert inputStream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return result;
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Klasa uruchamiająca moduł i usługę Bluetooth
     */
    private class WaitForBluetoothThread extends Thread {

        public void run() {
            if (adapter == null) {
                errorExit("Fatal Error", "Brak Bluetooth w urządzeniu");
            }
            if (!adapter.isEnabled()) {
                Log.d(TAG, "Próba włączenia Bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            while (!adapter.isEnabled()) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex) {
                    // unexpected interruption while enabling bluetooth
                    Thread.currentThread().interrupt(); // restore interrupted flag
                    return;
                }
            }
            Log.d(TAG, "Włączono Bluetooth");

            handler.obtainMessage(BLUETOOTH_ENABLE, true).sendToTarget();

            connectRemoteDevice();
        }
    }

    //----------------------------------------------------------------------------------------------
    public void connectRemoteDevice() {
        if(BluetoothAdapter.checkBluetoothAddress(ADDRESS)) {
            BluetoothDevice device = adapter.getRemoteDevice(ADDRESS);
            Log.d(TAG, "Łączenie z ... " + device);
            handler.obtainMessage(PRINT, "Łączenie z " + device + " ...").sendToTarget();;
            adapter.cancelDiscovery();
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                Log.d(TAG, "Połączono");
                handler.obtainMessage(PRINT, "... Połączono").sendToTarget();;
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Nie można zakończyć połączenia");
                }
                Log.e(TAG, "Błąd w czasie tworzenia socket", e);
            }

            if(socket != null) {
                connectionThread = new ConnectionThread(socket, handler);
                connectionThread.start();
            }
        } else {
            Log.e(TAG, "Niepoprawny adres urządzenia: " + ADDRESS);
        }
    }

}
