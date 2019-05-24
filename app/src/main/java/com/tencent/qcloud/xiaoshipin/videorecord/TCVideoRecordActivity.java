package com.tencent.qcloud.xiaoshipin.videorecord;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.activity.TCBaseActivity;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.common.widget.BeautySettingPannel;
import com.tencent.qcloud.xiaoshipin.login.TCUserMgr;
import com.tencent.qcloud.xiaoshipin.videoeditor.TCVideoPreprocessActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.BGMSelectActivity;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.view.SoundEffectsSettingPannel;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.view.TCBGMPannel2;
import com.tencent.qcloud.xiaoshipin.videorecord.draft.RecordDraftMgr;
import com.tencent.qcloud.xiaoshipin.videorecord.draft.RecordDraftInfo;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLog;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.ugc.TXRecordCommon;
import com.tencent.ugc.TXUGCRecord;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;
import com.tencent.ugc.TXVideoInfoReader;
import com.tencent.ugc.TXVideoJoiner;
import com.umeng.socialize.UMShareAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.view.View.GONE;

/**
 * UGC短视频录制界面
 */
public class TCVideoRecordActivity extends TCBaseActivity implements View.OnClickListener, BeautySettingPannel.IOnBeautyParamsChangeListener
        , TXRecordCommon.ITXVideoRecordListener, View.OnTouchListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener
        , TXVideoJoiner.TXVideoJoinerListener {

    private static final String TAG = "TCVideoRecordActivity";
    private int mRecordType = TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD;
    private boolean mRecording = false;
    private boolean mStartPreview = false;
    private boolean mFront = true;
    private TXUGCRecord mTXCameraRecord;

    private BeautySettingPannel.BeautyParams mBeautyParams = new BeautySettingPannel.BeautyParams();
    private TXCloudVideoView mVideoView;
    private LinearLayout backLL;
    private TextView mTvNextStep;
    private TextView mProgressTime;
    private ProgressDialog mCompleteProgressDialog;
    private ImageView mIvTorch;
    private ImageView mIvBeauty;
    private ImageView mIvScale;
    private ComposeRecordBtn mComposeRecordBtn;
    private RelativeLayout mRlAspectSelect;
    private ImageView mIvAspectSelectFirst;
    private ImageView mIvAspectSelectSecond;
    private ImageView mIvScaleMask;
    private ImageView mIvMusicMask;
    private ImageView mIvSoundEffectMask;
    private boolean mAspectSelectShow = false;

    private BeautySettingPannel mBeautyPannelView;
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusListener;
    private boolean mPause = false;

    private TCBGMPannel2 mTCBGMPannel;
    private SoundEffectsSettingPannel mSoundEffectsSettingPannel;
    private long mBGMStartTime, mBgmEndTime;
    private int mBgmPosition = -1;
    private int mCurrentAspectRatio = TXRecordCommon.VIDEO_ASPECT_RATIO_9_16; // 视频比例;
    private int mFirstSelectScale;
    private int mSecondSelectScale;
    private RelativeLayout mRecordRelativeLayout = null;
    private FrameLayout mMaskLayout;
    private RecordProgressView mRecordProgressView;
    private ImageView mIvDeleteLastPart;
    private boolean isSelected = false; // 回删状态
    private boolean mIsTorchOpen = false; // 闪光灯的状态

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor;
    private float mLastScaleFactor;

    private int mMinDuration = 2 * 1000;
    private int mMaxDuration = 16 * 1000;
    private int mHomeOrientation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN; // 录制方向
    private int mRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT; // 渲染方向
    private String mBGMPath;
    private String mBGMName;
    private String mBGMPlayingPath;
    private int mBGMDuration;
    private RadioGroup mRadioGroup;
    private int mRecordSpeed = TXRecordCommon.RECORD_SPEED_NORMAL;
    private RadioButton mRbSloweset;
    private RadioButton mRbFast;
    private RadioButton mRbFastest;
    private RadioButton mRbNormal;
    private RadioButton mRbSlow;
    private LinearLayout mLayoutLeftPanel;
    private RelativeLayout mLayoutRightPanel;

    private RelativeLayout mLayoutRightUtils;

    // 合拍
    private View mFollowShotLayout;
    private TXCloudVideoView mVideoViewFollowShotRecord;
    private FrameLayout mVideoViewPlay;
    private long mFollowShotVideoDuration; // 合拍视频的时长ms
    // 合拍中的播放用TXEditer播放，也可以使用TXVodPlayer播放
    private TXVideoEditer mTXVideoEditer;
    private String mFollowShotVideoPath;
    private String mRecordVideoPath;
    // 合拍接口
    private TXVideoJoiner mTXVideoJoiner;
    private int mFollowShotVideoFps; // 跟拍视频的fps
    private int mFollowShotAudioSampleRateType; // 跟拍视频的音频采样率
    private BackGroundHandler mBgHandler;
    private HandlerThread mHandlerThread;
    private TXVideoEditConstants.TXVideoInfo recordVideoInfo;
    private TXVideoEditConstants.TXVideoInfo followVideoInfo;
    private final int MSG_LOAD_VIDEO_INFO = 1000;
    private String mFollowShotVideoOutputPath;
    private boolean isReadyJoin = false;

    private RelativeLayout mLayoutRightUtilsForFollowShot;

    private ImageView mIvMusicMaskForFollowShot;
    private TextView mTxTimerNumber;

    //拍摄方式选择
    private LinearLayout mLayoutRecordModeSelect;
    private TextView mTextTakePhoto;
    private TextView mTextClickStart;
    private TextView mTextTouchShot;
    private View mViewRecordModeInstruction;

    private MediaPlayer mShootMediaPlayer = null;

    private MyHandler mMyHandler = new MyHandler();

    private ImageView mIvSnapshotPhoto;

    //标记是否在拍照中，包括拍照，存储，动画的整个过程之前把该值设为真，过程结束后设为假
    //在进行拍照之前，如果该值为真，则不进入拍照过程，以防止多个拍照过程重叠而产生的异常
    private boolean mIsTakingPhoto = false;

    /**
     * ------------------------ 滑动滤镜相关 ------------------------
     */
    private TextView mTvFilter;
    private int mCurrentIndex = 0; // 当前滤镜Index
    private int mLeftIndex = 0, mRightIndex = 1;// 左右滤镜的Index
    private int mLastLeftIndex = -1, mLastRightIndex = -1; // 之前左右滤镜的Index
    private float mLeftBitmapRatio;      // 左侧滤镜的比例
    private float mMoveRatio;      // 滑动的比例大小
    private boolean mStartScroll;  // 已经开始滑动了标记
    private boolean mMoveRight;    // 滑动是否往右
    private boolean mIsNeedChange;    // 滤镜的是否需要发生改变
    private ValueAnimator mFilterAnimator;
    private boolean mIsDoingAnimator;

    private Bitmap mLeftBitmap;
    private Bitmap mRightBitmap;
    //--------------------------------------------------------------

    //
    private boolean mTouchFocus = true; // 开启手动聚焦；自动聚焦设置为false
    // 草稿箱
    private RecordDraftMgr mRecordDraftMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_record);

        initViews();

        getData();

        updateView();

        initRecordDraft();
    }

    private void initRecordDraft() {
        if(mRecordType != TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD){
            // 合唱没加草稿箱功能
            return;
        }
        mRecordDraftMgr = new RecordDraftMgr(this);
        RecordDraftInfo lastDraftInfo = mRecordDraftMgr.getLastDraftInfo();
        if(lastDraftInfo == null){
            return;
        }
        mCurrentAspectRatio = lastDraftInfo.getAspectRatio();
        setSelectAspect();
        List<RecordDraftInfo.RecordPart> recordPartList = lastDraftInfo.getPartList();
        if(recordPartList != null && recordPartList.size() > 0){
            mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
            long duration = 0;
            for(int i = 0; i < recordPartList.size(); i++){
                RecordDraftInfo.RecordPart recordPart = recordPartList.get(i);
                mTXCameraRecord.getPartsManager().insertPart(recordPart.getPath(), i);
                TXVideoEditConstants.TXVideoInfo txVideoInfo = TXVideoInfoReader.getInstance().getVideoFileInfo(recordPart.getPath());
                duration = duration + txVideoInfo.duration;
                mRecordProgressView.setProgress((int) duration);
                mRecordProgressView.clipComplete();
            }
            processProgressTime(mTXCameraRecord.getPartsManager().getDuration());
            mIvMusicMask.setVisibility(View.VISIBLE);
            mIvScaleMask.setVisibility(View.VISIBLE);
        }
    }

    private void updateView() {
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
            mLayoutRightUtils.setVisibility(GONE);
            mRadioGroup.setVisibility(GONE);
            mTextTakePhoto.setVisibility(View.INVISIBLE);
        } else {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
            mLayoutRightUtils.setVisibility(View.VISIBLE);
            mRadioGroup.setVisibility(View.VISIBLE);
            mTextTakePhoto.setVisibility(View.VISIBLE);
        }
    }

    private static final int MSG_TAKE_PHOTO_SUCCESS = 1;
    private static final int MSG_TAKE_PHOTO_FAIL = 2;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TAKE_PHOTO_SUCCESS: {
                    Bitmap bitmap = (Bitmap)msg.obj;
                    showSnapshotPhoto(bitmap);
                    break;
                }
                case MSG_TAKE_PHOTO_FAIL: {
                    Toast.makeText(TCVideoRecordActivity.this, getResources().getString(R.string.activity_video_record_take_photo_fail), Toast.LENGTH_SHORT).show();
                    mIsTakingPhoto = true;
                    break;
                }
                default:
                    break;

            }
        }
    }

    private void getData() {
        Intent intent = getIntent();
        if (intent == null) {
            TXCLog.e(TAG, "intent is null");
            return;
        }

        mRecordType = intent.getIntExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD);
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mFollowShotLayout.setVisibility(View.VISIBLE);
            // 录制的界面
            mVideoView = mVideoViewFollowShotRecord;
            // 播放的界面
            mFollowShotVideoPath = intent.getStringExtra(TCConstants.VIDEO_EDITER_PATH);
            mFollowShotVideoDuration = (int)(intent.getFloatExtra(TCConstants.VIDEO_RECORD_DURATION, 0) * 1000);
            initPlayer();
            // 录制进度条以跟拍视频的进度为最大长度，fps以跟拍视频的fps为准
            mMaxDuration = (int)mFollowShotVideoDuration;
            mFollowShotVideoFps = intent.getIntExtra(TCConstants.RECORD_CONFIG_FPS, 20);
            mFollowShotAudioSampleRateType = intent.getIntExtra(TCConstants.VIDEO_RECORD_AUDIO_SAMPLE_RATE_TYPE, TXRecordCommon.AUDIO_SAMPLERATE_48000);
            // 初始化合拍的接口
            mTXVideoJoiner = new TXVideoJoiner(this);
            mTXVideoJoiner.setVideoJoinerListener(this);
            // 初始化子线程
            mHandlerThread = new HandlerThread("FollowShotThread");
            mHandlerThread.start();
            mBgHandler = new BackGroundHandler(mHandlerThread.getLooper());
        }else{
            mRecordType = TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD;
        }

        mRecordProgressView.setMaxDuration(mMaxDuration);
        mRecordProgressView.setMinDuration(mMinDuration);
    }

    private void initPlayer() {
        if(mTXVideoEditer != null){
            return;
        }
        mTXVideoEditer = new TXVideoEditer(this);
        mTXVideoEditer.setVideoPath(mFollowShotVideoPath);
        TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
        param.videoView = mVideoViewPlay;
        param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
        mTXVideoEditer.initWithPreview(param);
    }

    private void startCameraPreview() {
        if (mStartPreview) return;
        mStartPreview = true;

        mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        mTXCameraRecord.setVideoRecordListener(this);
        // 推荐配置
        TXRecordCommon.TXUGCCustomConfig customConfig = new TXRecordCommon.TXUGCCustomConfig();
        customConfig.minDuration = mMinDuration;
        customConfig.maxDuration = mMaxDuration;
        customConfig.isFront = mFront;
        customConfig.touchFocus = mTouchFocus;
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            customConfig.videoFps = mFollowShotVideoFps;
            customConfig.needEdit = false;
            mTXCameraRecord.setVideoRenderMode(TXRecordCommon.VIDEO_RENDER_MODE_ADJUST_RESOLUTION); // 设置渲染模式为自适应模式
            mTXCameraRecord.setMute(true); // 跟拍不从喇叭录制声音，因为跟拍的视频声音也会从喇叭发出来被麦克风录制进去，造成跟原视频声音的"二重唱"。
        }else{
            customConfig.needEdit = true;
            mTXCameraRecord.setMute(false);
        }

        mTXCameraRecord.setHomeOrientation(mHomeOrientation);
        mTXCameraRecord.setRenderRotation(mRenderRotation);
        mTXCameraRecord.startCameraCustomPreview(customConfig, mVideoView);
        mTXCameraRecord.setAspectRatio(mCurrentAspectRatio);

        mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyLevel, mBeautyParams.mWhiteLevel, mBeautyParams.mRuddyLevel);
        mTXCameraRecord.setFaceScaleLevel(mBeautyParams.mFaceSlimLevel);
        mTXCameraRecord.setEyeScaleLevel(mBeautyParams.mBigEyeLevel);
        mTXCameraRecord.setSpecialRatio(mBeautyParams.mFilterMixLevel / 10.0f);
        mTXCameraRecord.setFilter(mBeautyParams.mFilterBmp);
        mTXCameraRecord.setGreenScreenFile(mBeautyParams.mGreenFile, true);
        mTXCameraRecord.setMotionTmpl(mBeautyParams.mMotionTmplPath);
        mTXCameraRecord.setFaceShortLevel(mBeautyParams.mFaceShortLevel);
        mTXCameraRecord.setFaceVLevel(mBeautyParams.mFaceVLevel);
        mTXCameraRecord.setChinLevel(mBeautyParams.mChinSlimLevel);
        mTXCameraRecord.setNoseSlimLevel(mBeautyParams.mNoseScaleLevel);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initViews() {
        backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(this);

        mMaskLayout = (FrameLayout) findViewById(R.id.mask);
        mMaskLayout.setOnTouchListener(this);

        mTvNextStep = (TextView) findViewById(R.id.tv_next_step);
        mTvNextStep.setVisibility(View.GONE);
        mTvFilter = (TextView) findViewById(R.id.record_tv_filter);

        mBeautyPannelView = (BeautySettingPannel) findViewById(R.id.beauty_pannel);
        mBeautyPannelView.setBeautyParamsChangeListener(this);

        mTCBGMPannel = (TCBGMPannel2) findViewById(R.id.tc_record_bgm_pannel);
        mTCBGMPannel.setOnBGMChangeListener(new TCBGMPannel2.BGMChangeListener() {

            @Override
            public void onBGMVolumChanged(float volume) {
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setBGMVolume(volume);
                }
            }

            @Override
            public void onBGMTimeChanged(long startTime, long endTime) {
                if (mTXCameraRecord != null) {
                    mBGMStartTime = startTime;
                    mBgmEndTime = endTime;
                    mTXCameraRecord.playBGMFromTime((int) startTime, (int) endTime);
                }
            }

            @Override
            public void onClickReplace() {
                chooseBGM();
            }

            @Override
            public void onClickDelete() {
                prepareToDeleteBGM();
            }

            @Override
            public void onClickConfirm() {
                hideBgmPannel();
                stopPreviewBGM();
            }

        });

        mSoundEffectsSettingPannel = (SoundEffectsSettingPannel) findViewById(R.id.sound_effects_setting_pannel);
        mSoundEffectsSettingPannel.setSoundEffectsSettingPannelListener(new SoundEffectsSettingPannel.SoundEffectsSettingPannelListener() {
            @Override
            public void onMicVolumeChanged(float volume) {
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setMicVolume(volume);
                }
            }

            @Override
            public void onClickConfirm() {
                hideSoundEffectPannel();
            }

            @Override
            public void onClickVoiceChanger(int type) {
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setVoiceChangerType(type);
                }
            }

            @Override
            public void onClickReverb(int type) {
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setReverb(type);
                }
            }
        });

        mVideoView = (TXCloudVideoView) findViewById(R.id.video_view);

        mFollowShotLayout = findViewById(R.id.follow_shot_layout);
        mVideoViewPlay = (FrameLayout) findViewById(R.id.video_view_follow_shot_play);
        mVideoViewFollowShotRecord = (TXCloudVideoView) findViewById(R.id.video_view_follow_shot_record);

        mProgressTime = (TextView) findViewById(R.id.progress_time);
        mProgressTime.setText(0.0f + getResources().getString(R.string.unit_second));
        mIvDeleteLastPart = (ImageView) findViewById(R.id.btn_delete_last_part);

        mLayoutLeftPanel = (LinearLayout) findViewById(R.id.record_left_panel);
        mLayoutRightPanel = (RelativeLayout) findViewById(R.id.record_right_panel);

        mIvScale = (ImageView) findViewById(R.id.iv_scale);
        mIvScaleMask = (ImageView) findViewById(R.id.iv_scale_mask);
        mIvAspectSelectFirst = (ImageView) findViewById(R.id.iv_scale_first);
        mIvAspectSelectSecond = (ImageView) findViewById(R.id.iv_scale_second);
        mRlAspectSelect = (RelativeLayout) findViewById(R.id.layout_aspect_select);

        mIvMusicMask = (ImageView) findViewById(R.id.iv_music_mask);
        mIvSoundEffectMask = (ImageView) findViewById(R.id.iv_sound_effect_mask);

        mIvBeauty = (ImageView) findViewById(R.id.btn_beauty);

        mRecordRelativeLayout = (RelativeLayout) findViewById(R.id.record_layout);
        mRecordProgressView = (RecordProgressView) findViewById(R.id.record_progress_view);

        mGestureDetector = new GestureDetector(this, this);
        mScaleGestureDetector = new ScaleGestureDetector(this, this);

        mCompleteProgressDialog = new ProgressDialog(this);
        mCompleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // 设置进度条的形式为圆形转动的进度条
        mCompleteProgressDialog.setCancelable(false);                           // 设置是否可以通过点击Back键取消
        mCompleteProgressDialog.setCanceledOnTouchOutside(false);               // 设置在点击Dialog外是否取消Dialog进度条

        mIvTorch = (ImageView) findViewById(R.id.btn_torch);
        mIvTorch.setOnClickListener(this);

        if (mFront) {
            mIvTorch.setVisibility(View.GONE);
            mIvTorch.setImageResource(R.drawable.ugc_torch_disable);
        } else {
            mIvTorch.setImageResource(R.drawable.selector_torch_close);
            mIvTorch.setVisibility(View.VISIBLE);
        }
        mComposeRecordBtn = (ComposeRecordBtn) findViewById(R.id.compose_record_btn);
        mComposeRecordBtn.setOnRecordButtonListener(new ComposeRecordBtn.IRecordButtonListener() {
            @Override
            public void onRecordStart() {
                if (mAspectSelectShow) {
                    hideAspectSelectAnim();
                    mAspectSelectShow = !mAspectSelectShow;
                }
                if (!mRecording || mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0) {
                    TXCLog.i(TAG, "startRecord");
                    mTXCameraRecord.setRecordSpeed(mRecordSpeed);
                    startRecord();
                } else if (mPause) {
                    TXCLog.i(TAG, "resumeRecord");
                    resumeRecord();
                }
            }

            @Override
            public void onRecordPause() {
                if (mRecording && !mPause) {
                    TXCLog.i(TAG, "pauseRecord");
                    pauseRecord();
                }
            }

            @Override
            public void onTakePhotoStart() {
                if(mIsTakingPhoto){
                    return;
                }
                mIsTakingPhoto = true;
                AudioManager meng = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

                if (volume != 0) {
                    if (mShootMediaPlayer == null) {
                        mShootMediaPlayer = MediaPlayer.create(TCVideoRecordActivity.this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                    }
                    if (mShootMediaPlayer != null) {
                        mShootMediaPlayer.start();
                    }
                }

                mTXCameraRecord.snapshot(new TXRecordCommon.ITXSnapshotListener() {
                    @Override
                    public void onSnapshot(Bitmap bitmap) {
                        String fileName = System.currentTimeMillis() + ".jpg";
                        MediaStore.Images.Media.insertImage(TCVideoRecordActivity.this.getContentResolver(), bitmap, fileName, null);

                        Message message = new Message();
                        message.what = MSG_TAKE_PHOTO_SUCCESS;
                        message.obj = bitmap;
                        mMyHandler.sendMessage(message);
                    }
                });
            }

            @Override
            public void onTakePhotoFinish() {

            }
        });
        mRadioGroup = (RadioGroup) findViewById(R.id.rg_record_speed);
        mRbFast = (RadioButton) findViewById(R.id.rb_fast);
        mRbFastest = (RadioButton) findViewById(R.id.rb_fastest);
        mRbNormal = (RadioButton) findViewById(R.id.rb_normal);
        mRbSlow = (RadioButton) findViewById(R.id.rb_slow);
        mRbSloweset = (RadioButton) findViewById(R.id.rb_slowest);
        ((RadioButton) findViewById(R.id.rb_normal)).setChecked(true);
        mRbNormal.setBackground(ContextCompat.getDrawable(this, R.drawable.record_mid_bg));
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.rb_fast:
                        mRbFast.setBackground(ContextCompat.getDrawable(TCVideoRecordActivity.this, R.drawable.record_mid_bg));
                        mRbFastest.setBackground(null);
                        mRbNormal.setBackground(null);
                        mRbSlow.setBackground(null);
                        mRbSloweset.setBackground(null);
                        mRecordSpeed = TXRecordCommon.RECORD_SPEED_FAST;
                        break;
                    case R.id.rb_fastest:
                        mRbFastest.setBackground(ContextCompat.getDrawable(TCVideoRecordActivity.this, R.drawable.record_right_bg));
                        mRbFast.setBackground(null);
                        mRbNormal.setBackground(null);
                        mRbSlow.setBackground(null);
                        mRbSloweset.setBackground(null);
                        mRecordSpeed = TXRecordCommon.RECORD_SPEED_FASTEST;
                        break;
                    case R.id.rb_normal:
                        mRbNormal.setBackground(ContextCompat.getDrawable(TCVideoRecordActivity.this, R.drawable.record_mid_bg));
                        mRbFastest.setBackground(null);
                        mRbFast.setBackground(null);
                        mRbSlow.setBackground(null);
                        mRbSloweset.setBackground(null);
                        mRecordSpeed = TXRecordCommon.RECORD_SPEED_NORMAL;
                        break;
                    case R.id.rb_slow:
                        mRbSlow.setBackground(ContextCompat.getDrawable(TCVideoRecordActivity.this, R.drawable.record_mid_bg));
                        mRbFastest.setBackground(null);
                        mRbFast.setBackground(null);
                        mRbNormal.setBackground(null);
                        mRbSloweset.setBackground(null);
                        mRecordSpeed = TXRecordCommon.RECORD_SPEED_SLOW;
                        break;
                    case R.id.rb_slowest:
                        mRbSloweset.setBackground(ContextCompat.getDrawable(TCVideoRecordActivity.this, R.drawable.record_left_bg));
                        mRbFastest.setBackground(null);
                        mRbFast.setBackground(null);
                        mRbNormal.setBackground(null);
                        mRbSlow.setBackground(null);
                        mRecordSpeed = TXRecordCommon.RECORD_SPEED_SLOWEST;
                        break;
                }
            }
        });

        mLayoutRecordModeSelect = (LinearLayout) findViewById(R.id.layout_record_mode_selector);
        mTextTakePhoto = (TextView) findViewById(R.id.text_take_photo);
        mTextClickStart = (TextView) findViewById(R.id.text_click_shot);
        mTextTouchShot = (TextView) findViewById(R.id.text_touch_shot);

        mViewRecordModeInstruction = findViewById(R.id.view_record_mode_instruction);

        mTextClickStart.setSelected(true);
        mTextTakePhoto.setOnClickListener(mRecordModeSelectTextsOnClickListener);
        mTextClickStart.setOnClickListener(mRecordModeSelectTextsOnClickListener);
        mTextTouchShot.setOnClickListener(mRecordModeSelectTextsOnClickListener);

        mLayoutRightUtils = (RelativeLayout)findViewById(R.id.layout_right_utils);
        mLayoutRightUtilsForFollowShot = (RelativeLayout)findViewById(R.id.layout_right_utils_for_follow_shot);

        mIvMusicMaskForFollowShot = (ImageView)findViewById(R.id.iv_music_mask_for_follow_shot);
        mTxTimerNumber = (TextView)findViewById(R.id.tx_timer_number);

        mIvSnapshotPhoto = (ImageView)findViewById(R.id.iv_snapshot_photo);

        hideBgmPannel();

    }

    private void showSnapshotPhoto(Bitmap bitmap){
        mIvSnapshotPhoto.setTranslationX(0);
        mIvSnapshotPhoto.setTranslationY(0);
        mIvSnapshotPhoto.setScaleX(1);
        mIvSnapshotPhoto.setScaleY(1);
        mIvSnapshotPhoto.setPivotX(0);
        mIvSnapshotPhoto.setPivotY(0);
        mIvSnapshotPhoto.setAlpha(1.0f);
        mIvSnapshotPhoto.setImageBitmap(bitmap);
        mIvSnapshotPhoto.setVisibility(View.VISIBLE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float screenWidth = dm.widthPixels;

        float vWidth = mIvSnapshotPhoto.getWidth();

        float density = getResources().getDisplayMetrics().density;

        float targetWidthInDP = 80;

        float targetWidth = targetWidthInDP * density;

        float scale = targetWidth / vWidth;

        float targetLocalX = screenWidth - 40 * density - targetWidth;
        float targetLocalY = 40 * density;

        float translationX = targetLocalX - 0;
        float translationY = targetLocalY- 0;

        ObjectAnimator animatorScaleX = ObjectAnimator.ofFloat(mIvSnapshotPhoto, "scaleX", 1, scale);
        ObjectAnimator animatorScaleY = ObjectAnimator.ofFloat(mIvSnapshotPhoto, "scaleY", 1, scale);
        ObjectAnimator animatorTranslationX = ObjectAnimator.ofFloat(mIvSnapshotPhoto, "translationX", 0, translationX);
        ObjectAnimator animatorTranslationY = ObjectAnimator.ofFloat(mIvSnapshotPhoto, "translationY", 0, translationY);

        AnimatorSet animatorSet1 = new AnimatorSet();
        animatorSet1.setDuration(500);
        animatorSet1.setInterpolator(new DecelerateInterpolator());
        animatorSet1.play(animatorScaleX).with(animatorScaleY).with(animatorTranslationX).with(animatorTranslationY);

        ObjectAnimator animatorFadeOut = ObjectAnimator.ofFloat(mIvSnapshotPhoto, "alpha", 1.0f, 1.0f, 0.0f);


        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.setDuration(500);
        animatorSet2.setInterpolator(new LinearInterpolator());
        animatorSet2.play(animatorFadeOut);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animatorSet1);
        animatorSet.play(animatorSet2).after(animatorSet1);

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIvSnapshotPhoto.setVisibility(View.INVISIBLE);
                Toast.makeText(TCVideoRecordActivity.this, getResources().getString(R.string.activity_video_record_take_photo_success),
                        Toast.LENGTH_SHORT).show();
                mIsTakingPhoto = false;

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

    private View.OnClickListener mRecordModeSelectTextsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.isSelected()) {
                return;
            }

            float xGap = 0;
            switch (v.getId()) {
                case R.id.text_take_photo: {

                    if (mTextClickStart.isSelected()) {
                        xGap = 1.0f / 3;
                    } else if (mTextTouchShot.isSelected()) {
                        xGap = 2.0f / 3;
                    }

                    mTextTakePhoto.setSelected(true);
                    mTextClickStart.setSelected(false);
                    mTextTouchShot.setSelected(false);

                    mComposeRecordBtn.setRecordMode(ComposeRecordBtn.RECORD_MODE_TAKE_PHOTO);

                    break;
                }
                case R.id.text_click_shot: {

                    if (mTextTakePhoto.isSelected()) {
                        xGap = -1.0f / 3;
                    } else if (mTextTouchShot.isSelected()) {
                        xGap = 1.0f / 3;
                    }

                    mTextTakePhoto.setSelected(false);
                    mTextClickStart.setSelected(true);
                    mTextTouchShot.setSelected(false);

                    mComposeRecordBtn.setRecordMode(ComposeRecordBtn.RECORD_MODE_CLICK_START);

                    break;
                }
                case R.id.text_touch_shot: {

                    if (mTextTakePhoto.isSelected()) {
                        xGap = -2.0f / 3;
                    } else if (mTextClickStart.isSelected()) {
                        xGap = -1.0f / 3;
                    }

                    mTextTakePhoto.setSelected(false);
                    mTextClickStart.setSelected(false);
                    mTextTouchShot.setSelected(true);

                    mComposeRecordBtn.setRecordMode(ComposeRecordBtn.RECORD_MODE_LONG_TOUCH);

                    break;
                }
                default:
                    break;
            }

            float x1 = mLayoutRecordModeSelect.getTranslationX();
            float x2 = x1 + mLayoutRecordModeSelect.getWidth() * xGap;

            ObjectAnimator animator = ObjectAnimator.ofFloat(mLayoutRecordModeSelect, "translationX", x1, x2);
            animator.setDuration(400);
            animator.start();
        }
    };

    private void showBgmPannel() {
        backLL.setVisibility(View.GONE);
        mTCBGMPannel.setVisibility(View.VISIBLE);
        mProgressTime.setVisibility(View.GONE);
        mRecordRelativeLayout.setVisibility(View.INVISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);


        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
        } else {
            mLayoutRightUtils.setVisibility(View.GONE);
        }
    }

    private void hideBgmPannel() {
        backLL.setVisibility(View.VISIBLE);
        mTCBGMPannel.setVisibility(View.GONE);
        mProgressTime.setVisibility(View.VISIBLE);
        mRecordRelativeLayout.setVisibility(View.VISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.VISIBLE);
        mViewRecordModeInstruction.setVisibility(View.VISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
        } else {
            mLayoutRightUtils.setVisibility(View.VISIBLE);
        }
    }

    private void showSoundEffectPannel() {
        backLL.setVisibility(View.INVISIBLE);
        mProgressTime.setVisibility(View.INVISIBLE);
        mRecordRelativeLayout.setVisibility(View.INVISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);

        mSoundEffectsSettingPannel.setVisibility(View.VISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
        } else {
            mLayoutRightUtils.setVisibility(View.GONE);
        }
    }

    private void hideSoundEffectPannel() {
        backLL.setVisibility(View.VISIBLE);
        mProgressTime.setVisibility(View.VISIBLE);
        mRecordRelativeLayout.setVisibility(View.VISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.VISIBLE);
        mViewRecordModeInstruction.setVisibility(View.VISIBLE);

        mSoundEffectsSettingPannel.setVisibility(View.GONE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
        } else {
            mLayoutRightUtils.setVisibility(View.VISIBLE);
        }

    }


    private void showEffectPannel() {
        backLL.setVisibility(View.INVISIBLE);
        mProgressTime.setVisibility(View.INVISIBLE);
        mRecordRelativeLayout.setVisibility(View.INVISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);

        mBeautyPannelView.setVisibility(View.VISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
        } else {
            mLayoutRightUtils.setVisibility(View.GONE);
        }
    }

    private void hideBeautyPannel() {
        backLL.setVisibility(View.VISIBLE);
        mProgressTime.setVisibility(View.VISIBLE);
        mRecordRelativeLayout.setVisibility(View.VISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.VISIBLE);
        mViewRecordModeInstruction.setVisibility(View.VISIBLE);

        mBeautyPannelView.setVisibility(View.GONE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
        } else {
            mLayoutRightUtils.setVisibility(View.VISIBLE);
        }
    }

    private void previewBGM(long startTime, long endTime) {
        if (!TextUtils.isEmpty(mBGMPath)) {
            // 保证在试听的时候音乐是正常播放的
            mTXCameraRecord.setRecordSpeed(TXRecordCommon.RECORD_SPEED_NORMAL);
            mTXCameraRecord.playBGMFromTime((int) startTime, (int) endTime);
        }
    }

    private void stopPreviewBGM() {
        // 选择完音乐返回时试听结束
        if (!TextUtils.isEmpty(mBGMPath)) {
            mTXCameraRecord.stopBGM();
            // 在试听结束时，再设置回原来的速度
            mTXCameraRecord.setRecordSpeed(mRecordSpeed);
        }
    }

    private void chooseBGM() {
        Intent bgmIntent = new Intent(this, BGMSelectActivity.class);
        bgmIntent.putExtra(TCConstants.BGM_POSITION, mBgmPosition);
        startActivityForResult(bgmIntent, TCConstants.ACTIVITY_BGM_REQUEST_CODE);
    }

    private void prepareToDeleteBGM() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.setTitle(getString(R.string.tips)).setCancelable(false).setMessage(R.string.delete_bgm_or_not)
                .setPositiveButton(R.string.confirm_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mBGMPath = null;
                        mBgmPosition = -1;
                        mTXCameraRecord.stopBGM();
                        mTXCameraRecord.setBGM(null);
                        mTCBGMPannel.setMusicName("");

                        // 录制添加BGM后是录制不了人声的，而音效是针对人声有效的，因此删除BGM后可以再次选择音效
                        mIvSoundEffectMask.setVisibility(View.GONE);

                        hideBgmPannel();
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();
    }

    public interface OnItemClickListener {
        void onBGMSelect(String path);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setSelectAspect();

        if (hasPermission()) {
            startCameraPreview();
        }

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            initPlayer();
            mTXVideoEditer.startPlayFromTime(0, mFollowShotVideoDuration);
            TXCLog.i(TAG, "onStart, mTXVideoEditer.startPlayFromTime");
    }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mComposeRecordBtn.pauseRecord();
        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopCameraPreview();
            mStartPreview = false;
        }
        if (mRecording && !mPause) {
            pauseRecord();
        }
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT && mTXVideoEditer != null) {
            mTXVideoEditer.stopPlay();
        }
        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseBGM();
        }

        // 设置闪光灯的状态为关闭
        if (mIsTorchOpen) {
            mIsTorchOpen = false;
            if (mFront) {
                mIvTorch.setVisibility(View.GONE);
                mIvTorch.setImageResource(R.drawable.ugc_torch_disable);
            } else {
                mIvTorch.setImageResource(R.drawable.selector_torch_close);
                mIvTorch.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRecordProgressView != null) {
            mRecordProgressView.release();
        }

        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopBGM();
            mTXCameraRecord.stopCameraPreview();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord.getPartsManager().deleteAllParts();
            mTXCameraRecord.release();
            mTXCameraRecord = null;
            mStartPreview = false;
        }
        // 界面退出，删除草稿箱视频片段
        if(mRecordDraftMgr != null){
            mRecordDraftMgr.deleteLastRecordDraft();
        }
        abandonAudioFocus();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mTXCameraRecord.stopCameraPreview();
        if (mRecording && !mPause) {
            pauseRecord();
        }

        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseBGM();
        }

        mStartPreview = false;
        startCameraPreview();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_ll:
                back();
                break;
            case R.id.btn_beauty:
                showEffectPannel();
                break;
            case R.id.btn_sound_effect: {
                showSoundEffectPannel();
                break;
            }
            case R.id.btn_switch_camera:
                mFront = !mFront;
                mIsTorchOpen = false;
                if (mFront) {
                    mIvTorch.setVisibility(View.GONE);
                    mIvTorch.setImageResource(R.drawable.ugc_torch_disable);
                } else {
                    mIvTorch.setImageResource(R.drawable.selector_torch_close);
                    mIvTorch.setVisibility(View.VISIBLE);
                }
                if (mTXCameraRecord != null) {
                    TXCLog.i(TAG, "switchCamera = " + mFront);
                    mTXCameraRecord.switchCamera(mFront);
                }
                break;
            case R.id.btn_music_pannel:
                if (TextUtils.isEmpty(mBGMPath)) {
                    chooseBGM();
                } else {
                    showBgmPannel();
                    mTXCameraRecord.setBGM(mBGMPath);
                    previewBGM(mBGMStartTime, mBgmEndTime);
                }

                if (mBeautyPannelView.getVisibility() == View.VISIBLE) {
                    mBeautyPannelView.setVisibility(GONE);
                    mIvBeauty.setImageResource(R.drawable.ugc_record_beautiful_girl);
                }
                break;
            case R.id.tv_next_step:
                nextStep();
                break;
            case R.id.iv_scale:
                scaleDisplay();
                break;
            case R.id.iv_scale_first:
                selectAnotherAspect(mFirstSelectScale);
                break;
            case R.id.iv_scale_second:
                selectAnotherAspect(mSecondSelectScale);
                break;
            case R.id.btn_delete_last_part:
                deleteLastPart();
                break;
            case R.id.btn_torch:
                toggleTorch();
                break;
            case R.id.btn_beauty_for_follow_shot: {
                showEffectPannel();
                break;
            }
            case R.id.btn_timer_for_follow_shot: {
                playTimerAnimation();
                break;
            }
            case R.id.btn_music_pannel_for_follow_shot: {
                if (TextUtils.isEmpty(mBGMPath)) {
                    chooseBGM();
                } else {
                    showBgmPannel();
                    mTXCameraRecord.setBGM(mBGMPath);
                    previewBGM(mBGMStartTime, mBgmEndTime);
                }

                if (mBeautyPannelView.getVisibility() == View.VISIBLE) {
                    mBeautyPannelView.setVisibility(GONE);
                    mIvBeauty.setImageResource(R.drawable.ugc_record_beautiful_girl);
                }
                break;
            }

            default:
                if (mTCBGMPannel != null && mTCBGMPannel.getVisibility() == View.VISIBLE) {
                    mTCBGMPannel.onClick(view);
                }
                break;
        }
    }

    private void playTimerAnimation() {
        mLayoutRightPanel.setVisibility(View.GONE);
        mLayoutLeftPanel.setVisibility(GONE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);

        mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);

        mComposeRecordBtn.setVisibility(GONE);

        mProgressTime.setVisibility(GONE);
        mRecordProgressView.setVisibility(GONE);
        playTimerAnimation(3);
    }

    private void playTimerAnimation(final int num) {
        if (num <= 0) {
            mTxTimerNumber.setVisibility(GONE);
            mTextTakePhoto.setSelected(false);
            mTextClickStart.setSelected(true);
            mTextTouchShot.setSelected(false);
            mLayoutRecordModeSelect.setTranslationX(0);
            mComposeRecordBtn.setRecordMode(ComposeRecordBtn.RECORD_MODE_CLICK_START);
            mComposeRecordBtn.setVisibility(View.VISIBLE);
            mComposeRecordBtn.startRecord();
            mProgressTime.setVisibility(View.VISIBLE);
            mRecordProgressView.setVisibility(View.VISIBLE);
            return;
        }
        mTxTimerNumber.setVisibility(View.VISIBLE);
        mTxTimerNumber.setText(Integer.toString(num));

        ObjectAnimator animatorX = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleX", 0, 1.1f);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleY", 0, 1.1f);

        ObjectAnimator animatorX2 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleX", 1.1f, 1);
        ObjectAnimator animatorY2 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleY", 1.1f, 1);

        ObjectAnimator animatorX3 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleX", 1, 1);
        ObjectAnimator animatorY3 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleY", 1, 1);

        ObjectAnimator animatorX4 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleX", 1, 0);
        ObjectAnimator animatorY4 = ObjectAnimator.ofFloat(mTxTimerNumber, "scaleY", 1, 0);


        AnimatorSet animatorSet1 = new AnimatorSet();
        animatorSet1.play(animatorX).with(animatorY);
        animatorSet1.setDuration(100);
        animatorSet1.setInterpolator(new AccelerateInterpolator());

        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.play(animatorX2).with(animatorY2);
        animatorSet2.setDuration(100);
        animatorSet2.setInterpolator(new DecelerateInterpolator());

        AnimatorSet animatorSet3 = new AnimatorSet();
        animatorSet3.play(animatorX3).with(animatorY3);
        animatorSet3.setDuration(500);
        animatorSet3.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet4 = new AnimatorSet();
        animatorSet4.play(animatorX4).with(animatorY4);
        animatorSet4.setDuration(300);
        animatorSet4.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.play(animatorSet1);
        animatorSet.play(animatorSet2).after(animatorSet1);
        animatorSet.play(animatorSet3).after(animatorSet2);
        animatorSet.play(animatorSet4).after(animatorSet3);

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                playTimerAnimation(num - 1);
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

    private void toggleTorch() {
        if (mIsTorchOpen) {
            mTXCameraRecord.toggleTorch(false);
            mIvTorch.setImageResource(R.drawable.selector_torch_close);
        } else {
            mTXCameraRecord.toggleTorch(true);
            mIvTorch.setImageResource(R.drawable.selector_torch_open);
        }
        mIsTorchOpen = !mIsTorchOpen;
    }

    private void deleteLastPart() {
        if (mRecording && !mPause) {
            return;
        }
        if(mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0){
            return;
        }
        if (!isSelected) {
            isSelected = true;
            mRecordProgressView.selectLast();
        } else {
            isSelected = false;
            mRecordProgressView.deleteLast();
            isReadyJoin = false;
            mTXCameraRecord.getPartsManager().deleteLastPart();
            // 草稿也相应删除
            if(mRecordDraftMgr != null){
                mRecordDraftMgr.deleteLastPart();
            }
            float timeSecondFloat = mTXCameraRecord.getPartsManager().getDuration() / 1000;
            mProgressTime.setText(String.format(Locale.CHINA, "%.1f", timeSecondFloat) + getResources().getString(R.string.unit_second));
            if (timeSecondFloat < mMinDuration / 1000 || mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
                mTvNextStep.setVisibility(View.GONE);
            } else {
                mTvNextStep.setVisibility(View.VISIBLE);
            }

            if (mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0) {
                // 重新开始录
                mRecording = false;
                mPause = false;
                mIvMusicMask.setVisibility(View.GONE);
                mIvMusicMaskForFollowShot.setVisibility(GONE);
                mIvScaleMask.setVisibility(GONE);
                mIvScale.setEnabled(true);
            }
        }
    }

    private void setSelectAspect() {
        if (mCurrentAspectRatio == TXRecordCommon.VIDEO_ASPECT_RATIO_9_16) {
            mIvScale.setImageResource(R.drawable.selector_aspect169);
            mFirstSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_1_1;
            mIvAspectSelectFirst.setImageResource(R.drawable.selector_aspect11);

            mSecondSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_3_4;
            mIvAspectSelectSecond.setImageResource(R.drawable.selector_aspect43);
        } else if (mCurrentAspectRatio == TXRecordCommon.VIDEO_ASPECT_RATIO_1_1) {
            mIvScale.setImageResource(R.drawable.selector_aspect11);
            mFirstSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_3_4;
            mIvAspectSelectFirst.setImageResource(R.drawable.selector_aspect43);

            mSecondSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_9_16;
            mIvAspectSelectSecond.setImageResource(R.drawable.selector_aspect169);
        } else {
            mIvScale.setImageResource(R.drawable.selector_aspect43);
            mFirstSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_1_1;
            mIvAspectSelectFirst.setImageResource(R.drawable.selector_aspect11);

            mSecondSelectScale = TXRecordCommon.VIDEO_ASPECT_RATIO_9_16;
            mIvAspectSelectSecond.setImageResource(R.drawable.selector_aspect169);
        }
    }

    private void scaleDisplay() {
        if (!mAspectSelectShow) {
            showAspectSelectAnim();
        } else {
            hideAspectSelectAnim();
        }

        mAspectSelectShow = !mAspectSelectShow;
    }

    private void selectAnotherAspect(int targetScale) {
        if (mTXCameraRecord != null) {
            scaleDisplay();

            mCurrentAspectRatio = targetScale;

            if (mCurrentAspectRatio == TXRecordCommon.VIDEO_ASPECT_RATIO_9_16) {
                mTXCameraRecord.setAspectRatio(TXRecordCommon.VIDEO_ASPECT_RATIO_9_16);

            } else if (mCurrentAspectRatio == TXRecordCommon.VIDEO_ASPECT_RATIO_3_4) {
                mTXCameraRecord.setAspectRatio(TXRecordCommon.VIDEO_ASPECT_RATIO_3_4);

            } else if (mCurrentAspectRatio == TXRecordCommon.VIDEO_ASPECT_RATIO_1_1) {
                mTXCameraRecord.setAspectRatio(TXRecordCommon.VIDEO_ASPECT_RATIO_1_1);
            }

            setSelectAspect();
        }
    }

    private void hideAspectSelectAnim() {
        ObjectAnimator showAnimator = ObjectAnimator.ofFloat(mRlAspectSelect, "translationX", 0f,
                2 * (getResources().getDimension(R.dimen.ugc_aspect_divider) + getResources().getDimension(R.dimen.ugc_aspect_width)));
        showAnimator.setDuration(80);
        showAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mRlAspectSelect.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        showAnimator.start();
    }

    private void showAspectSelectAnim() {
        ObjectAnimator showAnimator = ObjectAnimator.ofFloat(mRlAspectSelect, "translationX",
                2 * (getResources().getDimension(R.dimen.ugc_aspect_divider) + getResources().getDimension(R.dimen.ugc_aspect_width)), 0f);
        showAnimator.setDuration(80);
        showAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mRlAspectSelect.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        showAnimator.start();
    }

    private void resumeRecord() {
        mTXCameraRecord.setRecordSpeed(mRecordSpeed);
        mIvDeleteLastPart.setImageResource(R.drawable.ugc_delete_last_part_disable);
        mIvDeleteLastPart.setEnabled(false);
        mIvScaleMask.setVisibility(View.VISIBLE);

        mPause = false;
        isSelected = false;
        if (mTXCameraRecord != null) {
            mTXCameraRecord.resumeRecord();
            if (!TextUtils.isEmpty(mBGMPath)) {
                if (mBGMPlayingPath == null || !mBGMPath.equals(mBGMPlayingPath)) {
                    mTXCameraRecord.playBGMFromTime(0, mBGMDuration);
                    mBGMPlayingPath = mBGMPath;
                } else {
                    mTXCameraRecord.resumeBGM();
                }
            }
        }
        requestAudioFocus();

        mLayoutLeftPanel.setVisibility(View.GONE);
        mLayoutRightPanel.setVisibility(View.GONE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
        } else {
            mLayoutRightUtils.setVisibility(View.GONE);
            mRadioGroup.setVisibility(View.GONE);
        }

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            int recordPostion = mTXCameraRecord.getPartsManager().getDuration();
            mTXVideoEditer.startPlayFromTime(recordPostion, mFollowShotVideoDuration);
    }
    }

    private void pauseRecord() {
        mPause = true;
        mIvDeleteLastPart.setImageResource(R.drawable.selector_delete_last_part);
        mIvDeleteLastPart.setEnabled(true);

        if (mTXCameraRecord != null) {
            if (!TextUtils.isEmpty(mBGMPlayingPath)) {
                mTXCameraRecord.pauseBGM();
            }
            int stopResult = mTXCameraRecord.pauseRecord();
            TXLog.i(TAG, "pauseRecord, result = " + stopResult);
        }
        abandonAudioFocus();

        mLayoutRightPanel.setVisibility(View.VISIBLE);
        mLayoutLeftPanel.setVisibility(View.VISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.VISIBLE);
        mViewRecordModeInstruction.setVisibility(View.VISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
        } else {
            mLayoutRightUtils.setVisibility(View.VISIBLE);
            mRadioGroup.setVisibility(View.VISIBLE);
        }

        if (mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0) {
            mIvMusicMask.setVisibility(View.GONE);
            mIvMusicMaskForFollowShot.setVisibility(GONE);
            mIvScaleMask.setVisibility(View.GONE);
        }
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mTXVideoEditer.pausePlay();
    }
    }

    private void nextStep() {
        stopRecord();
    }

    private void stopRecord() {
        if (!mRecording && mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0) {
            return;
        }
        mCompleteProgressDialog.show();
        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseBGM();
            mTXCameraRecord.stopRecord();
        }

        mPause = true;
        abandonAudioFocus();

        mLayoutRightPanel.setVisibility(View.VISIBLE);
        mLayoutLeftPanel.setVisibility(View.VISIBLE);

        mLayoutRecordModeSelect.setVisibility(View.VISIBLE);
        mViewRecordModeInstruction.setVisibility(View.VISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.VISIBLE);
        } else {
            mLayoutRightUtils.setVisibility(View.VISIBLE);
            mRadioGroup.setVisibility(View.VISIBLE);
        }
    }

    private void startRecord() {
        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT && isReadyJoin) {
            // 上次已经合拍过了，如果没有删除分片，把录制分片生成录制视频，再次合成合拍视频。
            mTXCameraRecord.stopRecord();
            return;
        }

        mIvMusicMask.setVisibility(View.VISIBLE);
        mIvMusicMaskForFollowShot.setVisibility(View.VISIBLE);
        mIvScaleMask.setVisibility(View.VISIBLE);
        mIvScale.setEnabled(false);
        mIvDeleteLastPart.setImageResource(R.drawable.ugc_delete_last_part_disable);
        mIvDeleteLastPart.setEnabled(false);
        if (mTXCameraRecord == null) {
            mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        }

        String customVideoPath = getCustomVideoOutputPath();
        String customCoverPath = customVideoPath.replace(".mp4", ".jpg");

        int result = mTXCameraRecord.startRecord(customVideoPath, customCoverPath);

        String desc = null;
        switch (result) {
        case TXRecordCommon.START_RECORD_OK:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_ok);
            break;
        case TXRecordCommon.START_RECORD_ERR_IS_IN_RECORDING:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_err_is_in_recording);
            break;
        case TXRecordCommon.START_RECORD_ERR_VIDEO_PATH_IS_EMPTY:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_err_video_path_is_empty);
            break;
        case TXRecordCommon.START_RECORD_ERR_API_IS_LOWER_THAN_18:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_err_api_is_lower_than_18);
            break;
        case TXRecordCommon.START_RECORD_ERR_NOT_INIT:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_err_not_init);
            break;
        case TXRecordCommon.START_RECORD_ERR_LICENCE_VERIFICATION_FAILED:
            desc = getResources().getString(R.string.tc_video_record_activity_start_record_start_record_err_licence_verification_failed);
            break;
        }
        TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_START_RECORD, TCUserMgr.getInstance().getUserId(), result, desc, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                }
            });
        if (result != 0) {
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(),
                    getResources().getString(R.string.tc_video_record_activity_start_record_record_failed_tip)
                            + result, Toast.LENGTH_SHORT).show();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord.stopRecord();
            return;
        }

        // 保存草稿箱参数设置，比如屏比、美颜参数等。这里只提供了屏比设置参考，其他设置您可以自己补充
        if(mRecordDraftMgr != null){
            mRecordDraftMgr.setLastAspectRatio(mCurrentAspectRatio);
        }

        if (!TextUtils.isEmpty(mBGMPath)) {
            mBGMDuration = mTXCameraRecord.setBGM(mBGMPath);
            mTXCameraRecord.playBGMFromTime((int) mBGMStartTime, (int) mBgmEndTime);
            mBGMPlayingPath = mBGMPath;
            TXCLog.i(TAG, "music duration = " + mTXCameraRecord.getMusicDuration(mBGMPath));
        }

        mRecording = true;
        mPause = false;

        requestAudioFocus();

        mLayoutRightPanel.setVisibility(View.GONE);
        mLayoutLeftPanel.setVisibility(View.GONE);

        mLayoutRecordModeSelect.setVisibility(View.INVISIBLE);
        mViewRecordModeInstruction.setVisibility(View.INVISIBLE);

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mLayoutRightUtilsForFollowShot.setVisibility(View.GONE);
        } else {
            mLayoutRightUtils.setVisibility(GONE);
            mRadioGroup.setVisibility(View.GONE);
        }

        if (mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mTXVideoEditer.stopPlay();
            mTXVideoEditer.startPlayFromTime(0, mFollowShotVideoDuration);
    }
    }

    private String getCustomVideoOutputPath() {
        return getCustomVideoOutputPath(null);
    }

    private String getCustomVideoOutputPath(String fileNamePrefix) {
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
        String time = sdf.format(new Date(currentTime));
        String outputDir = Environment.getExternalStorageDirectory() + File.separator + TCConstants.OUTPUT_DIR_NAME;
        File outputFolder = new File(outputDir);
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }
        String tempOutputPath;
        if(TextUtils.isEmpty(fileNamePrefix)){
            tempOutputPath = outputDir + File.separator + "TXUGC_" + time + ".mp4";
        }else{
            tempOutputPath = outputDir + File.separator + "TXUGC_" + fileNamePrefix + time + ".mp4";
        }
        return tempOutputPath;
    }

    private void startPreprocess(String videoPath) {
        Intent intent = new Intent(this, TCVideoPreprocessActivity.class);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, videoPath);
        startActivity(intent);
    }

    @Override
    public void onBeautyParamsChange(BeautySettingPannel.BeautyParams params, int key) {
        switch (key) {
            case BeautySettingPannel.BEAUTYPARAM_BEAUTY:
                mBeautyParams.mBeautyLevel = params.mBeautyLevel;
                mBeautyParams.mBeautyStyle = params.mBeautyStyle;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyLevel, mBeautyParams.mWhiteLevel, mBeautyParams.mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_WHITE:
                mBeautyParams.mWhiteLevel = params.mWhiteLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyLevel, mBeautyParams.mWhiteLevel, mBeautyParams.mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACE_LIFT:
                mBeautyParams.mFaceSlimLevel = params.mFaceSlimLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setFaceScaleLevel(params.mFaceSlimLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BIG_EYE:
                mBeautyParams.mBigEyeLevel = params.mBigEyeLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setEyeScaleLevel(params.mBigEyeLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FILTER:
                mBeautyParams.mFilterBmp = params.mFilterBmp;
                mCurrentIndex = params.filterIndex;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setSpecialRatio(mBeautyPannelView.getFilterProgress(mCurrentIndex) / 10.f);
                    mTXCameraRecord.setFilter(params.mFilterBmp);
                }
                doTextAnimator();
                break;
            case BeautySettingPannel.BEAUTYPARAM_MOTION_TMPL:
                mBeautyParams.mMotionTmplPath = params.mMotionTmplPath;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setMotionTmpl(params.mMotionTmplPath);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_GREEN:
                mBeautyParams.mGreenFile = params.mGreenFile;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setGreenScreenFile(params.mGreenFile, true);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_RUDDY:
                mBeautyParams.mRuddyLevel = params.mRuddyLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyLevel, mBeautyParams.mWhiteLevel, mBeautyParams.mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACEV:
                mBeautyParams.mFaceVLevel = params.mFaceVLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setFaceVLevel(params.mFaceVLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACESHORT:
                mBeautyParams.mFaceShortLevel = params.mFaceShortLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setFaceShortLevel(params.mFaceShortLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_CHINSLIME:
                mBeautyParams.mChinSlimLevel = params.mChinSlimLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setChinLevel(params.mChinSlimLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_NOSESCALE:
                mBeautyParams.mNoseScaleLevel = params.mNoseScaleLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setNoseSlimLevel(params.mNoseScaleLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FILTER_MIX_LEVEL:
                mBeautyParams.mFilterMixLevel = params.mFilterMixLevel;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setSpecialRatio(params.mFilterMixLevel / 10.f);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRecordEvent(int event, Bundle param) {
        TXCLog.d(TAG, "onRecordEvent event id = " + event);
        if (event == TXRecordCommon.EVT_ID_PAUSE) {
            mRecordProgressView.clipComplete();
            if(mTXCameraRecord != null && mRecordDraftMgr != null){
                List<String> pathList = mTXCameraRecord.getPartsManager().getPartsPathList();
                String lastPath = pathList.get(pathList.size() - 1);
                mRecordDraftMgr.saveLastPart(lastPath);
            }
        } else if (event == TXRecordCommon.EVT_CAMERA_CANNOT_USE) {
            Toast.makeText(this, getResources().getString(R.string.tc_video_record_activity_on_record_event_evt_camera_cannot_use), Toast.LENGTH_SHORT).show();
        } else if (event == TXRecordCommon.EVT_MIC_CANNOT_USE) {
            Toast.makeText(this, getResources().getString(R.string.tc_video_record_activity_on_record_event_evt_mic_cannot_use), Toast.LENGTH_SHORT).show();
        } else if (event == TXRecordCommon.EVT_ID_RESUME) {

        }
    }

    @Override
    public void onRecordProgress(long milliSecond) {
        updateProgressView((int) milliSecond);
        processProgressTime(milliSecond);
    }

    private void processProgressTime(long milliSecond) {
        float timeSecondFloat = milliSecond / 1000f;
        mProgressTime.setText(String.format(Locale.CHINA, "%.1f", timeSecondFloat) + getResources().getString(R.string.unit_second));
        if (timeSecondFloat < mMinDuration / 1000 || mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT) {
            mTvNextStep.setVisibility(View.GONE);
        } else {
            mTvNextStep.setVisibility(View.VISIBLE);
        }
    }

    private boolean updateProgressView(int milliSecond) {
        if (mRecordProgressView == null) {
            return true;
        }
        mRecordProgressView.setProgress(milliSecond);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /** attention to this below ,must add this**/
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
        if (requestCode != TCConstants.ACTIVITY_BGM_REQUEST_CODE) {
            return;
        }
        if (data == null) {
            return;
        }
        mBGMPath = data.getStringExtra(TCConstants.BGM_PATH);
        mBGMName = data.getStringExtra(TCConstants.BGM_NAME);
        mBgmPosition = data.getIntExtra(TCConstants.BGM_POSITION, -1);
        // 录制添加BGM后是录制不了人声的，而音效是针对人声有效的，因此屏蔽音效选择
        mIvSoundEffectMask.setVisibility(View.VISIBLE);

        mTCBGMPannel.setMusicName(mBGMName);
        mBGMDuration = mTXCameraRecord.setBGM(mBGMPath);
        mBGMStartTime = 0;
        mBgmEndTime = mBGMDuration;
        mTCBGMPannel.setBgmDuration(mBGMDuration);
        mTCBGMPannel.setCutRange(0, mBgmEndTime);
        mTCBGMPannel.resetRangePos();
        showBgmPannel();
        previewBGM(mBGMStartTime, mBgmEndTime);
    }

    @Override
    public void onRecordComplete(TXRecordCommon.TXRecordResult result) {
        String desc = null;
        if (result.retCode < 0) {
            desc = "onRecordComplete录制失败:" + result.descMsg;
        } else {
            desc = "视频录制成功";
        }
        TCUserMgr.getInstance().uploadLogs(TCConstants.ELK_ACTION_VIDEO_RECORD, TCUserMgr.getInstance().getUserId(), result.retCode, desc, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
        TXCLog.i(TAG, "onRecordComplete, result retCode = " + result.retCode + ", descMsg = " + result.descMsg + ", videoPath = " + result.videoPath + ", coverPath = " + result.coverPath);
        if (result.retCode < 0) {
            mCompleteProgressDialog.dismiss();
            mPause = true;
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(),
                    getResources().getString(R.string.tc_video_record_activity_on_record_complete_fail_tip)
                    + result.descMsg, Toast.LENGTH_SHORT).show();
        } else {
            pauseRecord();
            mPause = true;
            mComposeRecordBtn.pauseRecord();
            if(mRecordType == TCConstants.VIDEO_RECORD_TYPE_FOLLOW_SHOT){
                mCompleteProgressDialog.setMessage(getResources().getString(R.string.tc_video_record_activity_on_record_complete_synthesizing));
                mCompleteProgressDialog.show();
                mRecordVideoPath = result.videoPath;
                mBgHandler.sendEmptyMessage(MSG_LOAD_VIDEO_INFO);
            } else {
                mCompleteProgressDialog.dismiss();
                startPreprocess(result.videoPath);
            }
        }
    }

    class BackGroundHandler extends Handler {

        public BackGroundHandler(Looper looper) {
            super(looper);
        }

    @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_VIDEO_INFO:
                    recordVideoInfo = TXVideoInfoReader.getInstance().getVideoFileInfo(mRecordVideoPath);
                    followVideoInfo = TXVideoInfoReader.getInstance().getVideoFileInfo(mFollowShotVideoPath);
                    prepareToJoiner();
                    break;
            }
        }
    }

    private void prepareToJoiner(){
        List<String> videoSourceList = new ArrayList<>();
        videoSourceList.add(mRecordVideoPath);
        videoSourceList.add(mFollowShotVideoPath);
        mTXVideoJoiner.setVideoPathList(videoSourceList);
        mFollowShotVideoOutputPath = getCustomVideoOutputPath("Follow_Shot_");
        // 以左边录制的视频宽高为基准，右边视频等比例缩放
        int followVideoWidth;
        int followVideoHeight;
        if ((float) followVideoInfo.width / followVideoInfo.height >= (float)recordVideoInfo.width / recordVideoInfo.height) {
            followVideoWidth = recordVideoInfo.width;
            followVideoHeight = (int) ((float)recordVideoInfo.width * followVideoInfo.height / followVideoInfo.width);
        } else {
            followVideoWidth = (int) ((float)recordVideoInfo.height * followVideoInfo.width / followVideoInfo.height);
            followVideoHeight = recordVideoInfo.height;
        }

        TXVideoEditConstants.TXAbsoluteRect rect1 = new TXVideoEditConstants.TXAbsoluteRect();
        rect1.x = 0;                     //第一个视频的左上角位置
        rect1.y = 0;
        rect1.width = recordVideoInfo.width;   //第一个视频的宽高
        rect1.height = recordVideoInfo.height;

        TXVideoEditConstants.TXAbsoluteRect rect2 = new TXVideoEditConstants.TXAbsoluteRect();
        rect2.x = rect1.x + rect1.width; //第2个视频的左上角位置
        rect2.y = (recordVideoInfo.height - followVideoHeight) / 2;
        rect2.width = followVideoWidth;  //第2个视频的宽高
        rect2.height = followVideoHeight;

        List<TXVideoEditConstants.TXAbsoluteRect> list = new ArrayList<>();
        list.add(rect1);
        list.add(rect2);
        mTXVideoJoiner.setSplitScreenList(list, recordVideoInfo.width + followVideoWidth, recordVideoInfo.height); //第2，3个param：两个视频合成画布的宽高
        mTXVideoJoiner.splitJoinVideo(TXVideoEditConstants.VIDEO_COMPRESSED_540P, mFollowShotVideoOutputPath);
    }

    @Override
    public void onJoinProgress(final float progress) {
        TXCLog.i(TAG, "onJoinProgress, progress = " + progress);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int progressInt = (int) (progress * 100);
                mCompleteProgressDialog.setMessage(getResources().getString(R.string.tc_video_record_activity_on_join_progress_synthesizing)
                        + progressInt + "%");
            }
        });
    }

    @Override
    public void onJoinComplete(TXVideoEditConstants.TXJoinerResult result) {
        mCompleteProgressDialog.dismiss();
        if(result.retCode == TXVideoEditConstants.JOIN_RESULT_OK){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isReadyJoin = true;
                    startPreprocess(mFollowShotVideoOutputPath);
                    if(mTXVideoEditer != null){
                        mTXVideoEditer.release();
                        mTXVideoEditer = null;
                    }
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TCVideoRecordActivity.this,
                            getResources().getString(R.string.tc_video_record_activity_on_join_complete_synthesis_failed),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                for (int ret : grantResults) {
                    if (ret != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.tc_video_record_activity_on_request_permissions_result_failed_to_get_permission),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                startCameraPreview();
                break;
            default:
                break;
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 100);
                return false;
            }
        }
        return true;
    }

    private void requestAudioFocus() {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        }

        if (null == mOnAudioFocusListener) {
            mOnAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

                @Override
                public void onAudioFocusChange(int focusChange) {
                    try {
                        TXCLog.i(TAG, "requestAudioFocus, onAudioFocusChange focusChange = " + focusChange);

                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            pauseRecord();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            pauseRecord();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                        } else {
                            pauseRecord();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        try {
            mAudioManager.requestAudioFocus(mOnAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abandonAudioFocus() {
        try {
            if (null != mAudioManager && null != mOnAudioFocusListener) {
                mAudioManager.abandonAudioFocus(mOnAudioFocusListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == mMaskLayout) {
            if (motionEvent.getPointerCount() >= 2) {
                mScaleGestureDetector.onTouchEvent(motionEvent);
            } else if (motionEvent.getPointerCount() == 1) {
                mGestureDetector.onTouchEvent(motionEvent);
                // 说明是滤镜滑动后结束
                if (mStartScroll && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    doFilterAnimator();
                }
            }
        }
        return true;
    }

    private void doFilterAnimator() {
        if (mMoveRatio >= 0.2f) { //当滑动距离达到0.2比例的时候，则说明要切换
            mIsNeedChange = true;
            if (mMoveRight) { //说明是右滑动
                mCurrentIndex--;
                mFilterAnimator = generateValueAnimator(mLeftBitmapRatio, 1);
            } else { //左滑动
                mCurrentIndex++;
                mFilterAnimator = generateValueAnimator(mLeftBitmapRatio, 0);
            }
        } else {
            if (mCurrentIndex == mLeftIndex) {//说明用户向左侧滑动
                mFilterAnimator = generateValueAnimator(mLeftBitmapRatio, 1);
            } else {
                mFilterAnimator = generateValueAnimator(mLeftBitmapRatio, 0);
            }
        }
        mFilterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mIsDoingAnimator = true;
                if (mTXCameraRecord == null) return;
                float leftRatio = (float) valueAnimator.getAnimatedValue();
                // 动画结束
                if (leftRatio == 0 || leftRatio == 1) {
                    mLeftBitmapRatio = leftRatio;
                    if (mIsNeedChange) {
                        mIsNeedChange = false;
                        doTextAnimator();
                    } else {
                        mIsDoingAnimator = false;
                    }
                    mBeautyPannelView.setCurrentFilterIndex(mCurrentIndex);

                    // 保存到params 以便程序切换后恢复滤镜
                    if (mCurrentIndex == mLeftIndex) {
                        mBeautyParams.mFilterBmp = mLeftBitmap;
                    } else {
                        mBeautyParams.mFilterBmp = mRightBitmap;
                    }
                    mBeautyParams.mFilterMixLevel = mBeautyPannelView.getFilterProgress(mCurrentIndex);
                }
                float leftSpecialRatio = mBeautyPannelView.getFilterProgress(mLeftIndex) / 10.f;
                float rightSpecialRatio = mBeautyPannelView.getFilterProgress(mRightIndex) / 10.f;
                mTXCameraRecord.setFilter(
                        mLeftBitmap,
                        leftSpecialRatio,
                        mRightBitmap,
                        rightSpecialRatio,leftRatio
                );
            }


        });
        mFilterAnimator.start();
    }

    private ValueAnimator generateValueAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(400);
        return animator;
    }

    private void doTextAnimator() {
        // 设置当前滤镜的名字
        mTvFilter.setText(mBeautyPannelView.getBeautyFilterArr()[mCurrentIndex]);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(400);
        alphaAnimation.setInterpolator(new LinearInterpolator());
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mTvFilter.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTvFilter.setVisibility(View.GONE);
                mIsDoingAnimator = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mTvFilter.startAnimation(alphaAnimation);
    }

    // OnGestureListener回调start
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        mStartScroll = false;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (mBeautyPannelView.isShown()) {
            hideBeautyPannel();
        }

        if(mSoundEffectsSettingPannel.getVisibility() == View.VISIBLE){
            hideSoundEffectPannel();
        }

        if(mTCBGMPannel.getVisibility() == View.VISIBLE){
            hideBgmPannel();
            stopPreviewBGM();
        }
        if (mTXCameraRecord != null && mTouchFocus) {
            mTXCameraRecord.setFocusPosition(motionEvent.getX(), motionEvent.getY());
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent downEvent, MotionEvent moveEvent, float distanceX, float distanceY) {
        if (mIsDoingAnimator) {
            return true;
        }
        boolean moveRight = moveEvent.getX() > downEvent.getX();
        if (moveRight && mCurrentIndex == 0) {
            //  Toast.makeText(TCVideoRecordActivity.this, "已经是第一个啦~", Toast.LENGTH_SHORT).show();
            return true;
        } else if (!moveRight && mCurrentIndex == mBeautyPannelView.getBeautyFilterArr().length - 1) {
            //Toast.makeText(TCVideoRecordActivity.this, "已经是最后一个啦~", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            mStartScroll = true;
            if (moveRight) {//往右滑动
                mLeftIndex = mCurrentIndex - 1;
                mRightIndex = mCurrentIndex;
            } else {// 往左滑动
                mLeftIndex = mCurrentIndex;
                mRightIndex = mCurrentIndex + 1;
            }

            if (mLastLeftIndex != mLeftIndex) { //如果不一样，才加载bitmap出来；避免滑动过程中重复加载
                mLeftBitmap = mBeautyPannelView.getFilterBitmapByIndex(mLeftIndex);
                mLastLeftIndex = mLeftIndex;
            }

            if (mLastRightIndex != mRightIndex) {//如果不一样，才加载bitmap出来；避免滑动过程中重复加载
                mRightBitmap = mBeautyPannelView.getFilterBitmapByIndex(mRightIndex);
                mLastRightIndex = mRightIndex;
            }

            int width = mMaskLayout.getWidth();
            float dis = moveEvent.getX() - downEvent.getX();
            float leftBitmapRatio = Math.abs(dis) / (width * 1.0f);

            float leftSpecialRatio = mBeautyPannelView.getFilterProgress(mLeftIndex) / 10.0f;
            float rightSpecialRatio = mBeautyPannelView.getFilterProgress(mRightIndex) / 10.0f;
            mMoveRatio = leftBitmapRatio;
            if (moveRight) {
                leftBitmapRatio = leftBitmapRatio;
            } else {
                leftBitmapRatio = 1 - leftBitmapRatio;
            }
            this.mMoveRight = moveRight;
            mLeftBitmapRatio = leftBitmapRatio;
            if (mTXCameraRecord != null)
                mTXCameraRecord.setFilter( mLeftBitmap, leftSpecialRatio, mRightBitmap, rightSpecialRatio,leftBitmapRatio);
            return true;
        }
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return true;
    }
    // OnGestureListener回调end

    // OnScaleGestureListener回调start
    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        int maxZoom = mTXCameraRecord.getMaxZoom();
        if (maxZoom == 0) {
            TXCLog.i(TAG, "camera not support zoom");
            return false;
        }

        float factorOffset = scaleGestureDetector.getScaleFactor() - mLastScaleFactor;

        mScaleFactor += factorOffset;
        mLastScaleFactor = scaleGestureDetector.getScaleFactor();
        if (mScaleFactor < 0) {
            mScaleFactor = 0;
        }
        if (mScaleFactor > 1) {
            mScaleFactor = 1;
        }

        int zoomValue = Math.round(mScaleFactor * maxZoom);
        mTXCameraRecord.setZoom(zoomValue);
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        mLastScaleFactor = scaleGestureDetector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }
    // OnScaleGestureListener回调end

    private void back() {
        if (!mRecording) {
            if(mTXVideoEditer != null){
                mTXVideoEditer.stopPlay();
                mTXVideoEditer.release();
            }
            finish();
            return;
        }

        if (!mPause) {
            pauseRecord();
        }

        if (mTXCameraRecord.getPartsManager().getPartsPathList().size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog alertDialog = builder.setTitle(getString(R.string.cancel_record)).setCancelable(false).setMessage(R.string.confirm_cancel_record_content)
                    .setPositiveButton(R.string.give_up, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (mTXCameraRecord != null) {
                                mTXCameraRecord.getPartsManager().deleteAllParts();
                            }
                            // 草稿箱也相应删除
                            if(mRecordDraftMgr != null){
                                mRecordDraftMgr.deleteLastRecordDraft();
                            }
                            finish();
                        }
                    })
                    .setNegativeButton(getString(R.string.wrong_click), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            alertDialog.show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }
}
