package pl.chiefu.arduino_broker.app;

import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by chiefu on 17.03.14.
 */
public class ConnectionThread extends Thread {

    private static final String TAG = "ConnectionThread";

    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private Handler handler;

    public ConnectionThread(BluetoothSocket socket, Handler handler) {
        this.handler = handler;
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        if(socket != null) {
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        } else {
            inputStream = null;
            outputStream = null;
        }
    }

    public void run() {
        if(inputStream != null) {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    handler.obtainMessage(MainActivity.RECIEVE_MESSAGE, new String(buffer, 0, bytes)).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    break;
                }
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel(boolean message) {
        try {
            Log.d(TAG, "Zamykam BluetoothSocket");
            if(inputStream != null) {
                inputStream.close();
            }
            if(outputStream != null) {
                outputStream.close();
            }
            if(socket != null) {
                socket.close();
            }
            if(message) {
                handler.obtainMessage(MainActivity.PRINT, "Połączenie zakończono").sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
}
