package com.droibit.lpwidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.droibit.lpwidget.PlayIntentReceiver.ACTION_PLAY;
import static com.droibit.lpwidget.PlayIntentReceiver.EXTRA_PLAY_INTENT;

/**
 * LPの回復開始および終了時の通知を出すためのユーティリティクラス
 *
 * @author kumagai
 * @since 2014/04/04.
 */
public final class Notifications {

    private static final int ID = 1;

    private static final String PACKAGE_NAME = "klb.android.lovelive";


    /**
     * LPが回復する度に通知を更新する
     *
     * @param context コンテキスト
     * @param currentPoint 現在のLP
     * @param maxPoint 最大のLP
     */
    public static final void notifiyProgress(Context context, String currentPoint, String maxPoint) {
        final Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentIntent)
                .setTicker(context.getString(R.string.notification_begin_ticker))
                .setContentTitle(context.getString(R.string.notification_begin_title))
                .setContentText(context.getString(R.string.notification_begin_content_format, currentPoint, maxPoint))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        // v5.0から通知の外観が変わっているためバージョンによってアイコンを切り替える。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_notification_progress1);
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification_progress1))
                    .setSmallIcon(R.drawable.ic_notification_progress2);
        }

        final NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(ID, builder.build());
    }

    /**
     * LPが全回復したことを通知する。
     *
     * @param context コンテキスト
     * @param point 最大LP
     */
    public static final void notifiyFinish(Context context, String point) {
        final Intent contentintent = new Intent(context, MainActivity.class);
        final PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, 0, contentintent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_notification_complete)
                .setTicker(context.getString(R.string.notification_begin_ticker))
                .setContentTitle(context.getString(R.string.notification_finish_title))
                .setContentText(context.getString(R.string.notification_finish_content_format, point))
                .setWhen(System.currentTimeMillis())
                .setVibrate(new long[] {100, 200, 100, 500})
                .setLights(Color.WHITE, 1000, 500)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_notification_progress1));
        }

        // スクフェスがインストールされている場合はアプリ起動アクションを追加
        if (isInstalledSchoolIdolFestival(context)) {
            final Intent broadcastIntent = new Intent(ACTION_PLAY)
                    .setClass(context, PlayIntentReceiver.class)
                    .putExtra(EXTRA_PLAY_INTENT, context.getPackageManager()
                            .getLaunchIntentForPackage(PACKAGE_NAME));
            final PendingIntent broadcastPendingIntent = PendingIntent.getBroadcast(
                    context, 0, broadcastIntent, FLAG_UPDATE_CURRENT);
            final NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                        R.drawable.ic_full_play,
                        context.getString(R.string.notification_action_play),
                        broadcastPendingIntent)
                    .build();
            builder.addAction(R.drawable.ic_notification_play, context.getString(R.string.notification_action_play),
                        broadcastPendingIntent)
                   .extend(new NotificationCompat.WearableExtender().addAction(action));
        }

        final NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(ID, builder.build());
    }

    /**
     * 通知を非表示にする
     *
     * @param context コンテキスト
     */
    public static final void cancel(Context context) {
        final NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(ID);
    }

    private static final boolean isInstalledSchoolIdolFestival(Context context) {
        try {
            final PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(PACKAGE_NAME, PackageManager.GET_ACTIVITIES) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
