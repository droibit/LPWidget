package com.droibit.lpwidget;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * {@link CountDownTimer}を管理するためのサービス
 *
 * @author kumagai
 * @since 2014/04/01.
 */
public class TimerService extends Service {

    public static final String KEY_PREF_IS_PROGRESS = "isprogress";
    public static final String KEY_PREF_MILLIS_UNTIL_FINISHED = "millisUntilFinished";
    public static final String KEY_PREF_CURRENT_POINT = "currentpoint";
    public static final String KEY_PREF_MAX_POINT = "maxpoint";

    public static final String EXTRA_MILLIS_UNTIL_FINISHED = KEY_PREF_MILLIS_UNTIL_FINISHED;
    public static final String EXTRA_CURRENT_POINT = KEY_PREF_CURRENT_POINT;
    public static final String EXTRA_MAX_POINT = KEY_PREF_MAX_POINT;

    private static final long INTERVAL_MILLS_ONE_SECONDS = TimeUnit.SECONDS.toMillis(1);
    private static final long INTERVAL_MILLS_ONE_MINUTES = TimeUnit.MINUTES.toMillis(1);
    private static final long INTERVAL_MILLS_FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5);

    private final Binder mBinder = new Binder();
    private TimerCallbacks mCallbacks;
    private PointCounter mCountDownTimer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        mCallbacks = new NullCallbacks();
    }

    /** {@inheritDoc} */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("tag", "service#onstartcommand");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
            if (intent == null) {
                return START_NOT_STICKY;
            }
            // 「開始」ボタンから起動した場合は渡されるIntentから必要な情報を取得する
            mCountDownTimer = new PointCounter(
                    intent.getLongExtra(EXTRA_MILLIS_UNTIL_FINISHED, 0L),
                    INTERVAL_MILLS_ONE_SECONDS,
                    intent.getIntExtra(EXTRA_CURRENT_POINT, 0),
                    intent.getIntExtra(EXTRA_MAX_POINT, 0));
            prefs.edit().putBoolean(KEY_PREF_IS_PROGRESS, true).commit();
        } else {
            // 再起動した場合はprefsから必要な情報を取得する
            mCountDownTimer = new PointCounter(
                    prefs.getLong(KEY_PREF_MILLIS_UNTIL_FINISHED, 0L),
                    INTERVAL_MILLS_ONE_MINUTES,
                    prefs.getInt(KEY_PREF_CURRENT_POINT, 0),
                    prefs.getInt(KEY_PREF_MAX_POINT, 0));
        }
        mCountDownTimer.start();

        return START_STICKY;
    }

    /** {@inheritDoc} */
    @Override
    public void onDestroy() {
        setCallbacks(null);
        if (mCountDownTimer == null) {
            // #onStartCommandが呼ばれずに#onDestroyが呼ばれてしまう場合
            // ※ 予期せぬエラーもしくは唐突にタスクがkillされてしまった場合
            return;
        }

        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (mCountDownTimer.isFinished) {
           editor.putBoolean(KEY_PREF_IS_PROGRESS, false)
                 .putInt(KEY_PREF_CURRENT_POINT, mCountDownTimer.currentPoint)
                 .putLong(KEY_PREF_MILLIS_UNTIL_FINISHED, 0L);
        } else {
            editor.putBoolean(KEY_PREF_IS_PROGRESS, true)
                  .putInt(KEY_PREF_CURRENT_POINT, mCountDownTimer.currentPoint)
                  .putInt(KEY_PREF_MILLIS_UNTIL_FINISHED, mCountDownTimer.maxPoint)
                  .putLong(KEY_PREF_MILLIS_UNTIL_FINISHED, mCountDownTimer.millisUntilFinished);
        }
        editor.commit();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /** {@inheritDoc} */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //onDestroy();

        Notifications.cancel(this);

        // タスクkillされた際に呼ばれる
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(KEY_PREF_IS_PROGRESS, false);
        if (mCountDownTimer == null) {
            editor.commit();
            return;
        }

        editor.putInt(KEY_PREF_CURRENT_POINT, mCountDownTimer.currentPoint)
              .putLong(KEY_PREF_MILLIS_UNTIL_FINISHED, 0L)
              .commit();
    }

    public void onStop() {
        if (mCountDownTimer != null) {
            mCountDownTimer.isFinished = true;
        }
    }

    public void setCallbacks(TimerCallbacks callbacks) {
        if (callbacks == null) {
            callbacks = new NullCallbacks();
        }
        mCallbacks = callbacks;
    }

    /**
     * 1秒ずつカウントダウンするためのタイマークラス。<br/>
     * 5分経過したらアクティビティにコールバックする
     */
    class PointCounter extends CountDownTimer {

        long millisUntilFinished;
        long millisElapsed;
        int currentPoint;
        int maxPoint;
        boolean isFinished;

        PointCounter(long millisInFuture, long countDownInterval, int currentPoint, int maxPoint) {
            super (millisInFuture, countDownInterval);

            this.currentPoint = currentPoint;
            this.maxPoint = maxPoint;
            this.millisUntilFinished = millisInFuture;
            this.millisElapsed = 0L;
            this.isFinished = false;
        }

        /** {@inheritDoc} */
        @Override
        public void onTick(long millisUntilFinished) {
            this.millisElapsed += (this.millisUntilFinished - millisUntilFinished);
            if (this.millisElapsed >= INTERVAL_MILLS_FIVE_MINUTES &&
                    this.currentPoint <= this.maxPoint) {
                this.currentPoint++;
                this.millisElapsed = 0L;
                Notifications.notifiyProgress(TimerService.this,
                        String.valueOf(this.currentPoint), String.valueOf(this.maxPoint));
            }
            mCallbacks.onUpdate(millisUntilFinished, currentPoint);

            this.millisUntilFinished = millisUntilFinished;

            //Log.d("tag", String.format("Time: %d", millisUntilFinished));
        }

        /** {@inheritDoc} */
        @Override
        public void onFinish() {
            // #onTickないではLPがMAX-1までしかならないため、最大LPにしておく
            currentPoint = maxPoint;
            isFinished = true;
            mCallbacks.onFinish();
            cancel();
            stopSelf();

            Notifications.notifiyFinish(TimerService.this, String.valueOf(maxPoint));
        }
    }

    /**
     * アクティビティとサービスを関連付けるための{@link android.os.Binder}
     */
    public class Binder extends android.os.Binder {
        TimerService getService() {
            return TimerService.this;
        }
    };

    /**
     * {@link TimerCallbacks}のNULLオブジェクト
     */
    private class NullCallbacks implements TimerCallbacks {
        /** {@inheritDoc} */
        @Override
        public void onUpdate(long millisUntilFinished, int currentPoint) {
        }

        /** {@inheritDoc} */
        @Override
        public void onFinish() {
        }
    }
}