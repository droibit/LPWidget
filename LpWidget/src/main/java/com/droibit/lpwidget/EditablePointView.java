package com.droibit.lpwidget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * LP編集用のビュー。キーボード入力及びボタンによるインクリメント/デクリメントが可能。
 *
 * @author kumagai
 * @since 2014/04/01.
 */
public class EditablePointView extends LinearLayout implements View.OnClickListener {

    private Button mButtonPlus;
    private Button mButtonMinus;
    private EditText mTextPoint;
    private int mNumOfMinPoint;
    private int mNumOfMaxPoint;

    private final TextWatcher mWatcher = new TextWatcher() {
        /** {@inheritDoc} */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        /** {@inheritDoc} */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        /** {@inheritDoc} */
        @Override
        public void afterTextChanged(Editable s) {
            if (!validText(s.toString())) {
                mTextPoint.requestFocus();
            }
        }
    };

    public EditablePointView(Context context) {
        this(context, null, 0);
    }

    public EditablePointView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditablePointView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(context, R.layout.layout_editable_point, this);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.EditablePointView);

       final TextView labelView = (TextView)findViewById(R.id.text_label);
        labelView.setText(a.getString(R.styleable.EditablePointView_point_text));

        mNumOfMinPoint = a.getInteger(R.styleable.EditablePointView_min_point, 1);
        mNumOfMaxPoint = a.getInteger(R.styleable.EditablePointView_max_point, 1);

        a.recycle();
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mButtonPlus = (Button) findViewById(R.id.button_plus);
        mButtonMinus = (Button) findViewById(R.id.button_minus);
        mTextPoint = (EditText) findViewById(R.id.edit_point);

        mButtonPlus.setOnClickListener(this);
        mButtonMinus.setOnClickListener(this);

        mTextPoint.addTextChangedListener(mWatcher);

        setPoint(String.valueOf(mNumOfMinPoint));
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(View v) {
        final String text = mTextPoint.getText().toString();
        if (!validText(text)) {
            return;
        }

        int rhsValue = 0;
        if (v.getId() == R.id.button_minus) {
            rhsValue = -1;
        } else {
            rhsValue = 1;
        }

        final int newValue = Integer.valueOf(text) + rhsValue;
        mTextPoint.setText(String.valueOf(clamp(newValue)));
    }

    public String getPoint() {
        return mTextPoint.getText().toString();
    }

    public int getPointInt() {
        return Integer.valueOf(mTextPoint.getText().toString());
    }

    public void setPoint(String point) {
        mTextPoint.setText(point);
    }

    public void toggleEnable(boolean toggle) {
        mButtonPlus.setEnabled(toggle);
        mButtonMinus.setEnabled(toggle);
        mTextPoint.setEnabled(toggle);
    }

    private boolean validText(String text) {
        boolean hasComplete = true;
        if (TextUtils.isEmpty(text) || !TextUtils.isDigitsOnly(text)) {
            mTextPoint.setError(getResources().getString(R.string.edit_text_point_error));
            hasComplete = false;
        } else if (Integer.valueOf(text) < mNumOfMinPoint) {
            mTextPoint.setError(getResources().getString(R.string.edit_text_point_error_min_format, mNumOfMinPoint));
            hasComplete = false;
        } else if (Integer.valueOf(text) > mNumOfMaxPoint) {
            mTextPoint.setError(getResources().getString(R.string.edit_text_point_error_max_format, mNumOfMaxPoint));
            hasComplete = false;
        }
        return hasComplete;
    }

    private int clamp(int currentPoint) {
        int point = currentPoint;
        if (currentPoint < mNumOfMinPoint) {
            point = mNumOfMinPoint;
        } else if (currentPoint > mNumOfMaxPoint) {
            point = mNumOfMaxPoint;
        }
        return point;
    }
}
