package com.tencent.qcloud.xiaoshipin.videoeditor.bgm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCUtils;
import com.tencent.qcloud.xiaoshipin.videoeditor.time.view.RangeSlider;


public class TCBGMPannel2 extends RelativeLayout implements SeekBar.OnSeekBarChangeListener, RangeSlider.OnRangeChangeListener, View.OnClickListener {
    private Context mContext;
    private SeekBar mBGMVolumeSeekBar;
    private int mBGMVolume = 100;
    private BGMChangeListener mBGMChangeListener;
    private RangeSlider mRangeSlider;
    private long mBgmDuration;
    private Button mBtnConfirm;
    private TextView mTVStartTime;
    private ImageView mBtnReplace;
    private ImageView mBtnDelete;
    private TextView mTxMusicName;

    public TCBGMPannel2(Context context) {
        super(context);
        init(context);
    }

    public TCBGMPannel2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TCBGMPannel2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_bgm_edit2, this);
        mBGMVolumeSeekBar = (SeekBar) findViewById(R.id.seekbar_bgm_volume);
        mBGMVolumeSeekBar.setOnSeekBarChangeListener(this);

        mRangeSlider = (RangeSlider) findViewById(R.id.bgm_range_slider);
        mRangeSlider.setRangeChangeListener(this);

        mBtnConfirm = (Button) findViewById(R.id.btn_bgm_confirm);
        mBtnConfirm.setOnClickListener(this);
        mBtnReplace = (ImageView) findViewById(R.id.btn_bgm_replace);
        mBtnReplace.setOnClickListener(this);
        mBtnDelete = (ImageView) findViewById(R.id.btn_bgm_delete);
        mBtnDelete.setOnClickListener(this);

        mTVStartTime = (TextView) findViewById(R.id.tv_bgm_start_time);
        mTVStartTime.setText(String.format(getResources().getString(R.string.bgm_start_position), "00:00"));

        mTxMusicName = (TextView)findViewById(R.id.tx_music_name);
        mTxMusicName.setText("");
        mTxMusicName.setSelected(true);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    public void setMusicName(String musicName){
        mTxMusicName.setText(musicName);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.seekbar_bgm_volume) {
            mBGMVolume = progress;
            if (mBGMChangeListener != null) {
                mBGMChangeListener.onBGMVolumChanged(mBGMVolume / (float) 100);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void setBgmDuration(long duration) {
        mBgmDuration = duration;
    }

    public void setOnBGMChangeListener(BGMChangeListener volumeChangeListener) {
        mBGMChangeListener = volumeChangeListener;
    }

    public void resetRangePos() {
        mRangeSlider.resetRangePos();
    }

    /******** RangeSlider callback start *********/
    @Override
    public void onKeyDown(int type) {

    }

    @Override
    public void onKeyUp(int type, int leftPinIndex, int rightPinIndex) {
        long leftTime = mBgmDuration * leftPinIndex / 100; //ms
        long rightTime = mBgmDuration * rightPinIndex / 100;

        if (mBGMChangeListener != null) {
            mBGMChangeListener.onBGMTimeChanged(leftTime, rightTime);
        }

        mTVStartTime.setText(String.format(getResources().getString(R.string.bgm_start_position), TCUtils.millsecondToMinuteSecond((int) leftTime)));
    }

    public void updateBGMStartTime(long startTime) {
        mTVStartTime.setText(String.format(getResources().getString(R.string.bgm_start_position), TCUtils.millsecondToMinuteSecond((int) startTime)));
    }

    public void setCutRange(long startTime, long endTime) {
        if (mRangeSlider != null && mBgmDuration != 0) {
            int left = (int) (startTime * 100 / mBgmDuration);
            int right = (int) (endTime * 100 / mBgmDuration);
            mRangeSlider.setCutRange(left, right);
        }
        if (mTVStartTime != null) {
            mTVStartTime.setText(String.format(getResources().getString(R.string.bgm_start_position), TCUtils.millsecondToMinuteSecond((int) startTime)));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_bgm_confirm:
                if (mBGMChangeListener != null) {
                    mBGMChangeListener.onClickConfirm();
                }
                break;
            case R.id.btn_bgm_replace:
                if (mBGMChangeListener != null) {
                    mBGMChangeListener.onClickReplace();
                }
                break;
            case R.id.btn_bgm_delete:
                if (mBGMChangeListener != null) {
                    mBGMChangeListener.onClickDelete();
                }
                break;
            default:
                break;
        }
    }


    public interface BGMChangeListener {
        // 操作当前BGM

        void onBGMVolumChanged(float volume);

        void onBGMTimeChanged(long startTime, long endTime);

        void onClickReplace();

        void onClickDelete();

        void onClickConfirm();

    }
}
