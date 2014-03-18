package pl.chiefu.arduino_broker.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by chiefu on 17.03.14.
 */
public class BrokerService extends Service {

    private static boolean serviceAlive = false;

    private static final int BROKER_NOTIFICATION = 1;
    private NotificationManager notificationManager;
    private Notification notification;

    //----------------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        /*
        if (serverThread != null) {
            serverThread.interrupt();
        }

        Toast.makeText(getApplicationContext(), "Service stopped",
                Toast.LENGTH_SHORT).show();
        serviceAlive = false;
        sendMessage("Label", getString(R.string.start));
        stopNotification();
        */
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isServiceAlive() {
        return serviceAlive;
    }

    //----------------------------------------------------------------------------------------------
    /*
    private void initNotificationManager() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void initNotification(String tickerText) {
        int icon = R.drawable.ic_stat_arduino;
        long when = 0;
        notification = new Notification(icon, tickerText, when);
        notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_arduino)
                .build();
    }

    private void startNotification(String contentText) {
        initNotification(contentText);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        notification.setLatestEventInfo(this, getString(R.string.app_name),
                contentText, pendingIntent);
        notificationManager.notify(BROKER_NOTIFICATION, notification);
    }

    private void stopNotification() {
        notificationManager.cancel(BROKER_NOTIFICATION);
    }
    */
    //----------------------------------------------------------------------------------------------
}
