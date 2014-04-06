package com.droibit.lpwidget;

/**
 * {@link com.droibit.lpwidget.TimerService}から呼ばれるコールバックインターフェース。<br>
 * LPが1回復した時と、LPが全回復した際に呼ばれる。
 *
 * @author kumagai
 * @since 2014/04/04.
 */
public interface TimerCallbacks {

    /**
     * LPが1回復した際に呼ばれる処理
     *
     * @param millisUntilFinished 残り時間
     * @param currentPoint 現在のLP
     */
    void onUpdate(long millisUntilFinished, int currentPoint);

    /**
     * LPが全回復した際に呼ばれる処理
     */
    void onFinish();
}
