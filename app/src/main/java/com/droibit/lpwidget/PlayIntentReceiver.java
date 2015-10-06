package com.droibit.lpwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * スクフェスアプリを起動するために使用する。<br>
 * 通知のアクションから直接アプリを起動すると通知自体がキャンセルされないため。
 *
 * @author kumagai
 */
public class PlayIntentReceiver extends BroadcastReceiver {

    public static final String ACTION_PLAY = "com.droibit.lpwidget.ACTION_PLAY";
    public static final String EXTRA_PLAY_INTENT = "EXTRA_PLAY_INTENT";

    /** {@inheritDoc} */
    @Override
    public void onReceive(Context context, Intent intent) {
        Notifications.cancel(context);

        final Intent addIntent = intent.getParcelableExtra(EXTRA_PLAY_INTENT);
        context.startActivity(addIntent);
    }
}
