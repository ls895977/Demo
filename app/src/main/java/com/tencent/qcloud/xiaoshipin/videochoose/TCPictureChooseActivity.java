package com.tencent.qcloud.xiaoshipin.videochoose;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.videojoiner.ItemView;
import com.tencent.qcloud.xiaoshipin.videojoiner.MenuAdapter;
import com.tencent.qcloud.xiaoshipin.videojoiner.TCPictureJoinActivity;
import com.tencent.qcloud.xiaoshipin.videojoiner.widget.swipemenu.SwipeMenuRecyclerView;
import com.tencent.qcloud.xiaoshipin.videojoiner.widget.swipemenu.touch.OnItemMoveListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class TCPictureChooseActivity extends Activity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "TCPictureChooseActivity";

    public static final int TYPE_SINGLE_CHOOSE = 0;
    public static final int TYPE_MULTI_CHOOSE = 1;
    private static final int VIDEO_SPAN_COUNT = 4;

    private SwipeMenuRecyclerView mSwipeMenuRecyclerView;
    private MenuAdapter mMenuAdapter;
    private RecyclerView mRecyclerView;
    private Button mBtnNext;
    private TextView mTxTitle;

    private int mType;

    private TCVideoEditerListAdapter mAdapter;
    private TCVideoEditerMgr mTCVideoEditerMgr;
    private ArrayList<TCVideoFileInfo> mTCVideoFileInfoList;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ArrayList<TCVideoFileInfo> fileInfoArrayList = (ArrayList<TCVideoFileInfo>) msg.obj;
            mAdapter.addAll(fileInfoArrayList);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_ugc_video_list);

        mTxTitle = (TextView)findViewById(R.id.title_tv);
        mTxTitle.setText(getResources().getString(R.string.picture_choose));

        mTCVideoEditerMgr = TCVideoEditerMgr.getInstance(this);
        mHandlerThread = new HandlerThread("LoadList");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mType = getIntent().getIntExtra("CHOOSE_TYPE", TYPE_MULTI_CHOOSE);
        mTCVideoFileInfoList = new ArrayList<>();
        init();
        loadVideoList();
    }

    @Override
    protected void onDestroy() {
        mHandlerThread.getLooper().quit();
        mHandlerThread.quit();
        super.onDestroy();
    }

    private void loadVideoList() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<TCVideoFileInfo> fileInfoArrayList = mTCVideoEditerMgr.getAllPictrue();

                    Message msg = new Message();
                    msg.obj = fileInfoArrayList;
                    mMainHandler.sendMessage(msg);
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideoList();
        }
    }

    private void init() {
        LinearLayout backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(this);

        mBtnNext = (Button) findViewById(R.id.btn_next);
        mBtnNext.setEnabled(false);
        mBtnNext.setOnClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, VIDEO_SPAN_COUNT));

        mAdapter = new TCVideoEditerListAdapter(this);
        mAdapter.setOnItemAddListener(onItemAddListener);
        mRecyclerView.setAdapter(mAdapter);

        if (mType == TYPE_SINGLE_CHOOSE) {
            mAdapter.setMultiplePick(false);
        } else {
            mAdapter.setMultiplePick(true);
        }

        mSwipeMenuRecyclerView = (SwipeMenuRecyclerView) findViewById(R.id.swipe_menu_recycler_view);
        mSwipeMenuRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mMenuAdapter = new MenuAdapter(this, mTCVideoFileInfoList);
        mMenuAdapter.setOnItemDeleteListener(onItemDeleteListener);
        mSwipeMenuRecyclerView.setAdapter(mMenuAdapter);
        mSwipeMenuRecyclerView.setLongPressDragEnabled(true);
        mSwipeMenuRecyclerView.setOnItemMoveListener(onItemMoveListener);
    }

    private ItemView.OnDeleteListener onItemDeleteListener = new ItemView.OnDeleteListener() {
        @Override
        public void onDelete(int position) {
            mMenuAdapter.removeIndex(position);
            if (mMenuAdapter.getItemCount() == 0) {
                mBtnNext.setEnabled(false);
            }
        }
    };

    private ItemView.OnAddListener onItemAddListener = new ItemView.OnAddListener() {

        @Override
        public void onAdd(TCVideoFileInfo fileInfo) {
            mMenuAdapter.addItem(fileInfo);
            if (mMenuAdapter.getItemCount() > 0) {
                mBtnNext.setEnabled(true);
            }
        }
    };

    private OnItemMoveListener onItemMoveListener = new OnItemMoveListener() {
        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            // 当Item被拖拽的时候。
            Collections.swap(mTCVideoFileInfoList, fromPosition, toPosition);
            mMenuAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_next:
                doSelect();
                break;
            case R.id.back_ll:
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Glide.with(this).pauseRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(this).resumeRequests();
    }

    private void doSelect() {
        int count = mMenuAdapter.getItemCount();
        ArrayList picturePathList = new ArrayList();
        for (int i = 0; i < count; i++) {
            TCVideoFileInfo fileInfo = mMenuAdapter.getItem(i);
            File file = new File(fileInfo.getFilePath());
            if (!file.exists()) {
                showErrorDialog(getResources().getString(R.string.tc_picture_choose_activity_the_selected_file_does_not_exist));
                return;
            }
            picturePathList.add(fileInfo.getFilePath());
        }
        if (count < 2) {
            Toast.makeText(this,
                    getResources().getString(R.string.tc_picture_choose_activity_please_select_multiple_images),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TCPictureJoinActivity.class);
        intent.putStringArrayListExtra(TCConstants.INTENT_KEY_MULTI_PIC_LIST, picturePathList);
        startActivity(intent);
        finish();
    }

    private void showErrorDialog(String msg) {
        AlertDialog.Builder normalDialog = new AlertDialog.Builder(this, R.style.ConfirmDialogStyle);
        normalDialog.setMessage(msg);
        normalDialog.setCancelable(false);
        normalDialog.setPositiveButton(getResources().getString(R.string.tc_picture_choose_activity_got_it),
                null);
        normalDialog.show();
    }
}
