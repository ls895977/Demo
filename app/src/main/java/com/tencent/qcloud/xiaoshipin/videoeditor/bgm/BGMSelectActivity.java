package com.tencent.qcloud.xiaoshipin.videoeditor.bgm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.qcloud.xiaoshipin.R;
import com.tencent.qcloud.xiaoshipin.common.utils.TCConstants;
import com.tencent.qcloud.xiaoshipin.videoeditor.bgm.utils.TCBGMInfo;
import com.tencent.qcloud.xiaoshipin.videoeditor.common.widget.progress.SampleProgressButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinsonswang on 2017/12/8.
 */

public class BGMSelectActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private final String TAG = "BGMSelectActivity";
    private LinearLayout mBackLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mBGMRecyclerView;
    private View mEmptyView;
    private TCMusicAdapter mTCMusicAdapter;
    private TCBGMManager.LoadBgmListener mLoadBgmListener;
    private List<TCBGMInfo> mTCBgmInfoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bgm_select);

        initData();

        initView();

        initListener();

        prepareToRefresh();
    }

    private void prepareToRefresh() {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        this.onRefresh();
    }

    private void initData() {
        mTCBgmInfoList = new ArrayList<>();
    }

    private void initListener() {
        mLoadBgmListener = new TCBGMManager.LoadBgmListener() {
            @Override
            public void onBgmList(final ArrayList<TCBGMInfo> tcBgmInfoList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTCBgmInfoList.clear();
                        if (tcBgmInfoList != null) {
                            mTCBgmInfoList.addAll(tcBgmInfoList);
                        }
                        mTCMusicAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);

                        mEmptyView.setVisibility(mTCMusicAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onBgmDownloadSuccess(final int position, final String filePath) {

                try {
                    Thread.sleep(200);
                }catch (Exception e){
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TCBGMInfo info = mTCBgmInfoList.get(position);
                        info.status = TCBGMInfo.STATE_DOWNLOADED;
                        info.localPath = filePath;
                        mTCMusicAdapter.updateItem(position, info);
                    }
                });
            }

            @Override
            public void onDownloadFail(final int position, final String errorMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TCBGMInfo info = mTCBgmInfoList.get(position);
                        info.status = TCBGMInfo.STATE_UNDOWNLOAD;
                        info.progress = 0;
                        mTCMusicAdapter.updateItem(position, info);
                        Toast.makeText(BGMSelectActivity.this
                                , getResources().getString(R.string.bgm_select_activity_download_failed)
                                ,Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDownloadProgress(final int position, final int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TCBGMInfo info = mTCBgmInfoList.get(position);
                        info.status = TCBGMInfo.STATE_DOWNLOADING;
                        info.progress = progress;
                        mTCMusicAdapter.updateItem(position, info);
                    }
                });

            }
        };
        TCBGMManager.getInstance().setOnLoadBgmListener(mLoadBgmListener);
    }

    private void initView() {
        mBackLayout = (LinearLayout) findViewById(R.id.back_ll);
        mBackLayout.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.bgm_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false);
        mBGMRecyclerView = (RecyclerView) findViewById(R.id.bgm_recycler_view);
        mEmptyView = findViewById(R.id.tv_bgm_empty);

        mTCMusicAdapter = new TCMusicAdapter(this, mTCBgmInfoList);
        mTCMusicAdapter.setOnClickSubItemListener(new TCMusicAdapter.OnClickSubItemListener() {
            @Override
            public void onClickUseBtn(SampleProgressButton button, int position) {
                TCBGMInfo tcBgmInfo = mTCBgmInfoList.get(position);
                if(tcBgmInfo.status == TCBGMInfo.STATE_UNDOWNLOAD){
                    tcBgmInfo.status = TCBGMInfo.STATE_DOWNLOADING;
                    mTCMusicAdapter.updateItem(position, tcBgmInfo);
                    downloadMusic(position);
                }else if(tcBgmInfo.status == TCBGMInfo.STATE_DOWNLOADED){
                    backToEditActivity(position, tcBgmInfo.localPath);
                }

            }
        });

        mBGMRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBGMRecyclerView.addItemDecoration(new SpaceItemDecoration(5));
        mBGMRecyclerView.setAdapter(mTCMusicAdapter);
    }

    private void downloadMusic(int position){
        TCBGMInfo tcBgmInfo = mTCBgmInfoList.get(position);
        TXCLog.i(TAG, "tcBgmInfo name = " + tcBgmInfo.name + ", url = " + tcBgmInfo.url);
        if (TextUtils.isEmpty(tcBgmInfo.localPath)) {
            downloadBgmInfo(position, tcBgmInfo);
            tcBgmInfo.status = TCBGMInfo.STATE_DOWNLOADING;
            tcBgmInfo.progress = 0;
            mTCMusicAdapter.updateItem(position, tcBgmInfo);
            return;
        }
        File file = new File(tcBgmInfo.localPath);
        if(!file.isFile()){
            tcBgmInfo.localPath = "";
            tcBgmInfo.status = TCBGMInfo.STATE_DOWNLOADING;
            tcBgmInfo.progress = 0;
            mTCMusicAdapter.updateItem(position, tcBgmInfo);
            downloadBgmInfo(position, tcBgmInfo);
            return;
        }
    }

    private void backToEditActivity(int position, String path) {
        TCBGMManager.getInstance().setOnLoadBgmListener(null);
        Intent intent = new Intent();
        intent.putExtra(TCConstants.BGM_POSITION, position);
        intent.putExtra(TCConstants.BGM_PATH, path);
        intent.putExtra(TCConstants.BGM_NAME, mTCBgmInfoList.get(position).name);
        setResult(TCConstants.ACTIVITY_BGM_REQUEST_CODE, intent);
        finish();
    }

    @Override
    public void onRefresh() {
        TXCLog.i(TAG, "onRefresh");
        reloadBGMList();
    }

    private void downloadBgmInfo(int position, TCBGMInfo tcbgmInfo) {
        TCBGMManager.getInstance().downloadBgmInfo(tcbgmInfo.name, position, tcbgmInfo.url);
    }

    private void reloadBGMList() {
        TCBGMManager.getInstance().loadBgmList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TCBGMManager.getInstance().setOnLoadBgmListener(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_ll:
                finish();
                break;
        }
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int mSpace;

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.left = 0;
            outRect.right = 0;
            outRect.bottom = mSpace;
            outRect.top = 0;

        }

        public SpaceItemDecoration(int space) {
            this.mSpace = space;
        }
    }

}
