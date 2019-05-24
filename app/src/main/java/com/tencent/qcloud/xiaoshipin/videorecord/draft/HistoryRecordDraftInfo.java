package com.tencent.qcloud.xiaoshipin.videorecord.draft;

import java.util.List;

/**
 * Created by vinsonswang on 2018/9/4.
 *
 * @TODO 历史草稿箱
 */

public class HistoryRecordDraftInfo {
    private List<RecordDraftInfo> historyDraftInfo;

    public List<RecordDraftInfo> getRecordDraftInfoList() {
        return historyDraftInfo;
    }

    public void setRecordDraftInfoList(List<RecordDraftInfo> recordDraftInfoList) {
        this.historyDraftInfo = recordDraftInfoList;
    }
}
