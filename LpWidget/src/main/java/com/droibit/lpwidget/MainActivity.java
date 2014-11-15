package com.droibit.lpwidget;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import static com.droibit.lpwidget.TimerService.EXTRA_CURRENT_POINT;
import static com.droibit.lpwidget.TimerService.EXTRA_MAX_POINT;
import static com.droibit.lpwidget.TimerService.EXTRA_MILLIS_UNTIL_FINISHED;
import static com.droibit.lpwidget.TimerService.KEY_PREF_CURRENT_POINT;
import static com.droibit.lpwidget.TimerService.KEY_PREF_IS_PROGRESS;
import static com.droibit.lpwidget.TimerService.KEY_PREF_MAX_POINT;

/**
 * LPの回復のための{@link TimerService}を管理するためのアクティビティ
 *
 * @author kumagai
 * @since 2014/04/01.
 */
public class MainActivity extends Activity implements TimerCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private EditablePointView mEditCurrentPoint;
    private EditablePointView mEditMaxPoint;
    private TextView mTextTimer;
    private Button mButtonControl;

    private TimerService mService;


    private ServiceConnection mConnection = new ServiceConnection() {
        /** {@inheritDoc} */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TimerService.Binder) service).getService();
            mService.setCallbacks(MainActivity.this);
        }

        /** {@inheritDoc} */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.setCallbacks(null);
            mService = null;
        }
    };

    /** {@inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditCurrentPoint = (EditablePointView) findViewById(R.id.edit_current);
        mEditMaxPoint = (EditablePointView) findViewById(R.id.edit_max);
        mTextTimer = (TextView) findViewById(R.id.text_timer);
        mButtonControl = (Button) findViewById(R.id.button_control);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditMaxPoint.setPoint(String.valueOf(prefs.getInt(KEY_PREF_MAX_POINT,
                getResources().getInteger(R.integer.num_of_min_max_point))));
        mEditCurrentPoint.setPoint(String.valueOf(prefs.getInt(KEY_PREF_CURRENT_POINT, 0)));

        if (prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
            toggleEnableEditViews(false);
        } else {
            toggleEnableEditViews(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onStart() {
        super.onStart();

        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Log.d(TAG, String.format("isProgress : %s", prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)
                                                            ? "true" : "false"));
            // カウント中の場合は、経過がビューに反映される用サービスに接続する
            if (prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
                bindService(new Intent(this, TimerService.class), mConnection, BIND_AUTO_CREATE);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /** {@inheritDoc} */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            mService.setCallbacks(null);
        }

        // ダイアログが非表示になる際にサービスの接続を解除しておく
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
                unbindService(mConnection);
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onUpdate(long millisUntilFinished, int currentPoint) {

        final long hours;
        // 経過時間が1日を超える場合は計算方法を代えないと丸められてしまう。
        if (TimeUnit.MILLISECONDS.toDays(1) < millisUntilFinished) {
            hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
        } else {
            hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24;
        }
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;

        mEditCurrentPoint.setPoint(String.valueOf(currentPoint));
        // LPが999の場合でも97時間のためフォーマットは変更しない。
        mTextTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    /** {@inheritDoc} */
    @Override
    public void onFinish() {
        // ダイアログが表示されている場合のみこのメソッドは呼ばれる。
        // #onDestroyが呼ばれる際にサービスのコールバックをクリアしているため
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
            unbindService(mConnection);
        }

        toggleEnableEditViews(true);
        mEditCurrentPoint.setPoint(mEditMaxPoint.getPoint());

        Log.d(TAG, String.format("Finish: %s", mEditMaxPoint.getPoint()));
    }

    public void onToggleState(View v) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(KEY_PREF_IS_PROGRESS, false)) {
            toggleEnableEditViews(true);

            if (mService != null) {
                mService.onStop();
            }

            try {
                unbindService(mConnection);
                stopService(new Intent(this, TimerService.class));
                Notifications.cancel(this);
            } catch (IllegalArgumentException e) {
                prefs.edit().putBoolean(KEY_PREF_IS_PROGRESS, false).commit();
                Log.d(TAG, e.getMessage(), e);
            }
            Log.d(TAG, "Stop");
            return;
        }

        // 入力ポイントが正常かどうか
        final String resText = validPoint();
        if (!TextUtils.isEmpty(resText)) {
            Toast.makeText(this, resText, Toast.LENGTH_SHORT).show();
            return;
        }
        // 変更された最大LPはカウントの開始タイミングで保存する
        prefs.edit().putInt(KEY_PREF_MAX_POINT, mEditMaxPoint.getPointInt()).commit();

        final int diffPoint = mEditMaxPoint.getPointInt() - mEditCurrentPoint.getPointInt();
        if (diffPoint == 0) {
            Toast.makeText(this, R.string.toast_point_is_full, Toast.LENGTH_SHORT).show();
            return;
        }
        final long diffTimeMills = TimeUnit.MINUTES.toMillis(diffPoint * 6); // TimeUnit.SECONDS.toMillis(10);

        toggleEnableEditViews(false);
        // カウント前に時間およびポイントをビューに反映させておく
        onUpdate(diffTimeMills, mEditCurrentPoint.getPointInt());

        final Intent service = new Intent(this, TimerService.class);
        service.putExtra(EXTRA_MILLIS_UNTIL_FINISHED, diffTimeMills);
        service.putExtra(EXTRA_CURRENT_POINT, mEditCurrentPoint.getPointInt());
        service.putExtra(EXTRA_MAX_POINT, mEditMaxPoint.getPointInt());

        startService(service);
        bindService(service, mConnection, BIND_AUTO_CREATE);

        Notifications.notifiyProgress(this, mEditCurrentPoint.getPoint(), mEditMaxPoint.getPoint());

        Log.d(TAG, String.format("Start: %s/%s", mEditCurrentPoint.getPoint(), mEditMaxPoint.getPoint()));
    }

    private void toggleEnableEditViews(boolean toggle) {
        mEditCurrentPoint.toggleEnable(toggle);
        mEditMaxPoint.toggleEnable(toggle);

        if (toggle) {
            mButtonControl.setText(R.string.button_text_play);
            mTextTimer.setText(R.string.text_remaining_time);
        } else {
            mButtonControl.setText(R.string.button_text_stop);
        }
    }

    private String validPoint() {
        if (TextUtils.isEmpty(mEditCurrentPoint.getPoint()) ||
            TextUtils.isEmpty(mEditMaxPoint.getPoint())) {
            // ポイントが空の場合
            return getString(R.string.toast_point_is_empty);
        } else if (!TextUtils.isDigitsOnly(mEditCurrentPoint.getPoint()) ||
                   !TextUtils.isDigitsOnly(mEditMaxPoint.getPoint())) {
            // 数値以外の値が入力されている場合
            return getString(R.string.toast_point_contains_character);
        } else if (mEditCurrentPoint.getPointInt() > mEditMaxPoint.getPointInt()) {
            // 現在LP > 最大LPの場合
            return getString(R.string.toast_point_reversal);
        }
        return null;
    }
}
