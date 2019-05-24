package com.tencent.qcloud.xiaoshipin.videorecord;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tencent.qcloud.xiaoshipin.R;


/**
 * Created by vinsonswang on 2017/9/8.
 * 按住拍的录制按钮
 */

public class ComposeRecordBtn extends RelativeLayout implements View.OnTouchListener {
    private Context mContext;
    private View mViewTakePhotoBkg;
    private View mViewTakePhoto;

    private View mViewRecordClickStartBkg;
    private View mViewRecordClickStart;
    private ImageView mIvRecordPause;

    private View mViewRecordTouchShotBkg;
    private View mViewRecordTouchShot;

    private IRecordButtonListener mIRecordButtonListener;

    public static final int RECORD_MODE_TAKE_PHOTO = 1;
    public static final int RECORD_MODE_CLICK_START = 2;
    public static final int RECORD_MODE_LONG_TOUCH = 3;

    private boolean mIsRecording = false;

    private int mRecordMode = RECORD_MODE_CLICK_START;

    private ViewGroup mRootLayout;

    public void setRecordMode(int mode) {
        mRecordMode = mode;

        mViewTakePhotoBkg.setVisibility(GONE);
        mViewTakePhoto.setVisibility(GONE);

        mViewRecordClickStartBkg.setVisibility(GONE);
        mViewRecordClickStart.setVisibility(GONE);

        mViewRecordTouchShotBkg.setVisibility(GONE);
        mViewRecordTouchShot.setVisibility(GONE);

        if (mRecordMode == RECORD_MODE_TAKE_PHOTO) {

            mViewTakePhotoBkg.setVisibility(VISIBLE);
            mViewTakePhoto.setVisibility(VISIBLE);

        } else if (mRecordMode == RECORD_MODE_CLICK_START) {

            mViewRecordClickStartBkg.setVisibility(VISIBLE);
            mViewRecordClickStart.setVisibility(VISIBLE);

        } else if (mRecordMode == RECORD_MODE_LONG_TOUCH) {

            mViewRecordTouchShotBkg.setVisibility(VISIBLE);
            mViewRecordTouchShot.setVisibility(VISIBLE);

        }
    }

    public int getRecordMode() {
        return mRecordMode;
    }

    public interface IRecordButtonListener {
        void onRecordStart();

        void onRecordPause();

        void onTakePhotoStart();

        void onTakePhotoFinish();
    }

    public void setOnRecordButtonListener(IRecordButtonListener iRecordButtonListener) {
        mIRecordButtonListener = iRecordButtonListener;
    }

    public ComposeRecordBtn(Context context) {
        super(context);
        init(context);
    }

    public ComposeRecordBtn(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ComposeRecordBtn(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.compose_record_btn, this);

        mRootLayout = (ViewGroup) findViewById(R.id.layout_compose_record_btn);

        mViewTakePhotoBkg = findViewById(R.id.view_take_photo_bkg);
        mViewTakePhoto = findViewById(R.id.view_take_photo);

        mViewRecordClickStartBkg = findViewById(R.id.view_record_click_shot_bkg);
        mViewRecordClickStart = findViewById(R.id.view_record_click_shot);

        mIvRecordPause = (ImageView) findViewById(R.id.iv_record_pause);

        mViewRecordTouchShotBkg = findViewById(R.id.view_record_touch_shot_bkg);
        mViewRecordTouchShot = findViewById(R.id.view_record_touch_shot);

        mViewTakePhotoBkg.setVisibility(GONE);
        mViewTakePhoto.setVisibility(GONE);

        mViewRecordClickStartBkg.setVisibility(VISIBLE);
        mViewRecordClickStart.setVisibility(VISIBLE);
        mIvRecordPause.setVisibility(GONE);

        mViewRecordTouchShotBkg.setVisibility(GONE);
        mViewRecordTouchShot.setVisibility(GONE);

        setOnTouchListener(this);
    }

    public void startRecord() {
        if (mRecordMode == RECORD_MODE_CLICK_START) {
            ObjectAnimator btnBkgZoomOutXAn = ObjectAnimator.ofFloat(mViewRecordClickStartBkg, "scaleX",
                    ((float) mRootLayout.getWidth()) / mViewRecordClickStartBkg.getWidth());
            ObjectAnimator btnBkgZoomOutYAn = ObjectAnimator.ofFloat(mViewRecordClickStartBkg, "scaleY",
                    ((float) mRootLayout.getHeight()) / mViewRecordClickStartBkg.getHeight());

            ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewRecordClickStart, "scaleX", 0.95f);
            ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewRecordClickStart, "scaleY", 0.95f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(80);
            animatorSet.setInterpolator(new LinearInterpolator());
            animatorSet.play(btnBkgZoomOutXAn).with(btnBkgZoomOutYAn).with(btnZoomInXAn).with(btnZoomInYAn);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIRecordButtonListener != null) {
                        mIRecordButtonListener.onRecordStart();
                        mIsRecording = true;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
            mIvRecordPause.setVisibility(View.VISIBLE);
        } else {
            ObjectAnimator btnBkgZoomOutXAn = ObjectAnimator.ofFloat(mViewRecordTouchShotBkg, "scaleX",
                    ((float) mRootLayout.getWidth()) / mViewRecordTouchShotBkg.getWidth());
            ObjectAnimator btnBkgZoomOutYAn = ObjectAnimator.ofFloat(mViewRecordTouchShotBkg, "scaleY",
                    ((float) mRootLayout.getHeight()) / mViewRecordTouchShotBkg.getHeight());

            ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewRecordTouchShot, "scaleX", 0.95f);
            ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewRecordTouchShot, "scaleY", 0.95f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(80);
            animatorSet.setInterpolator(new LinearInterpolator());
            animatorSet.play(btnBkgZoomOutXAn).with(btnBkgZoomOutYAn).with(btnZoomInXAn).with(btnZoomInYAn);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIRecordButtonListener != null) {
                        mIRecordButtonListener.onRecordStart();
                        mIsRecording = true;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
        }
    }

    public void pauseRecord() {
        if (mRecordMode == RECORD_MODE_CLICK_START) {
            ObjectAnimator btnBkgZoomInXAn = ObjectAnimator.ofFloat(mViewRecordClickStartBkg, "scaleX", 1f);
            ObjectAnimator btnBkgZoomIntYAn = ObjectAnimator.ofFloat(mViewRecordClickStartBkg, "scaleY", 1f);

            ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewRecordClickStart, "scaleX", 1f);
            ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewRecordClickStart, "scaleY", 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(80);
            animatorSet.setInterpolator(new LinearInterpolator());
            animatorSet.play(btnBkgZoomInXAn).with(btnBkgZoomIntYAn).with(btnZoomInXAn).with(btnZoomInYAn);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIRecordButtonListener != null) {
                        mIRecordButtonListener.onRecordPause();
                        mIsRecording = false;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();

            mIvRecordPause.setVisibility(View.GONE);
        } else {
            ObjectAnimator btnBkgZoomInXAn = ObjectAnimator.ofFloat(mViewRecordTouchShotBkg, "scaleX", 1f);
            ObjectAnimator btnBkgZoomIntYAn = ObjectAnimator.ofFloat(mViewRecordTouchShotBkg, "scaleY", 1f);

            ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewRecordTouchShot, "scaleX", 1f);
            ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewRecordTouchShot, "scaleY", 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(80);
            animatorSet.setInterpolator(new LinearInterpolator());
            animatorSet.play(btnBkgZoomInXAn).with(btnBkgZoomIntYAn).with(btnZoomInXAn).with(btnZoomInYAn);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIRecordButtonListener != null) {
                        mIRecordButtonListener.onRecordPause();
                        mIsRecording = false;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();

            mIvRecordPause.setVisibility(View.GONE);
        }
    }

    public void takePhoto() {
        ObjectAnimator btnBkgZoomOutXAn = ObjectAnimator.ofFloat(mViewTakePhotoBkg, "scaleX",
                ((float) mRootLayout.getWidth()) / mViewTakePhotoBkg.getWidth());
        ObjectAnimator btnBkgZoomOutYAn = ObjectAnimator.ofFloat(mViewTakePhotoBkg, "scaleY",
                ((float) mRootLayout.getHeight()) / mViewTakePhotoBkg.getHeight());

        ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewTakePhoto, "scaleX", 0.95f);
        ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewTakePhoto, "scaleY", 0.95f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(80);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(btnBkgZoomOutXAn).with(btnBkgZoomOutYAn).with(btnZoomInXAn).with(btnZoomInYAn);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIRecordButtonListener != null) {
                    mIRecordButtonListener.onTakePhotoStart();

                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    public void finishTakePhoto() {
        ObjectAnimator btnBkgZoomInXAn = ObjectAnimator.ofFloat(mViewTakePhotoBkg, "scaleX", 1f);
        ObjectAnimator btnBkgZoomIntYAn = ObjectAnimator.ofFloat(mViewTakePhotoBkg, "scaleY", 1f);

        ObjectAnimator btnZoomInXAn = ObjectAnimator.ofFloat(mViewTakePhoto, "scaleX", 1f);
        ObjectAnimator btnZoomInYAn = ObjectAnimator.ofFloat(mViewTakePhoto, "scaleY", 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(80);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.play(btnBkgZoomInXAn).with(btnBkgZoomIntYAn).with(btnZoomInXAn).with(btnZoomInYAn);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIRecordButtonListener != null) {
                    mIRecordButtonListener.onTakePhotoFinish();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (mRecordMode == RECORD_MODE_TAKE_PHOTO) {
                    takePhoto();
                } else if (mRecordMode == RECORD_MODE_CLICK_START) {
                    if (mIsRecording) {
                        pauseRecord();
                    } else {
                        startRecord();
                    }
                } else if (mRecordMode == RECORD_MODE_LONG_TOUCH) {
                    startRecord();
                    mIsRecording = true;
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mRecordMode == RECORD_MODE_TAKE_PHOTO) {
                    finishTakePhoto();
                } else if (mRecordMode == RECORD_MODE_CLICK_START) {

                } else if (mRecordMode == RECORD_MODE_LONG_TOUCH) {
                    pauseRecord();
                }

                break;
            }

        }
        return true;
    }
}
