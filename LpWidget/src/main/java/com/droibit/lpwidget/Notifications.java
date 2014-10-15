package com.droibit.lpwidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

/**
 * LPの回復開始および終了時の通知を出すためのユーティリティクラス
 *
 * @author kumagai
 * @since 2014/04/04.
 */
public final class Notifications {

    private static final int ID = 1;

    public static final void notifiyProgress(Context context, String currentPoint, String maxPoint) {
        final Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_count)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                .setTicker(context.getString(R.string.notification_begin_ticker))
                .setContentTitle(context.getString(R.string.notification_begin_title))
                .setContentText(context.getString(R.string.notification_begin_content_format, currentPoint, maxPoint))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        final NotificationManager nm = (NotificationManager) context.getSystemService(
                                                                    Service.NOTIFICATION_SERVICE);
        nm.notify(ID, builder.build());
    }

    public static final void notifiyFinish(Context context, String point) {
        final Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_complete)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                .setTicker(context.getString(R.string.notification_begin_ticker))
                .setContentTitle(context.getString(R.string.notification_finish_title))
                .setContentText(context.getString(R.string.notification_finish_content_format, point))
                .setWhen(System.currentTimeMillis())
                .setVibrate(new long[] {100, 200, 100, 500})
                .setLights(Color.WHITE, 1000, 500)
                .setAutoCancel(true);

        final NotificationManager nm = (NotificationManager) context.getSystemService(
                Service.NOTIFICATION_SERVICE);
        nm.notify(ID, builder.build());
    }


    public static final void cancel(Context context) {
        final NotificationManager nm = (NotificationManager) context.getSystemService(
                Service.NOTIFICATION_SERVICE);
        nm.cancel(ID);
    }
}
