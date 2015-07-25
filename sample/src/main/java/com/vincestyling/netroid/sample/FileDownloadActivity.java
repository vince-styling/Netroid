package com.vincestyling.netroid.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.vincestyling.netroid.Listener;
import com.vincestyling.netroid.NetroidError;
import com.vincestyling.netroid.NetroidLog;
import com.vincestyling.netroid.RequestQueue;
import com.vincestyling.netroid.request.FileDownloadRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.toolbox.FileDownloader;

import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;

public class FileDownloadActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    public final static DecimalFormat DECIMAL_POINT = new DecimalFormat("0.0");

    private LinkedList<DownloadTask> mTaskList;
    private LinkedList<DownloadTask> mDownloadList;
    private FileDownloader mDownloder;
    private File mSaveDir;

    private View lotBtnPanel;
    private Button btnAddTask;
    private BaseAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_downloader);

        RequestQueue queue = Netroid.newRequestQueue(getApplicationContext(), null);
        mDownloder = new FileDownloader(queue, 1) {
            @Override
            public FileDownloadRequest buildRequest(File storeFile, String url) {
                return new FileDownloadRequest(storeFile, url) {
                    @Override
                    public void prepare() {
                        addHeader("Accept-Encoding", "identity");
                        super.prepare();
                    }
                };
            }
        };

        mSaveDir = new File(Environment.getExternalStorageDirectory(), "netroid-sample");
        if (!mSaveDir.exists()) mSaveDir.mkdir();

        mTaskList = new LinkedList<DownloadTask>();
        mTaskList.add(new DownloadTask("Duowan20140427.apk", "http://download.game.yy.com/duowanapp/m/Duowan20140427.apk"));
        mTaskList.add(new DownloadTask("Evernote_402491.dmg", "http://cdn1.evernote.com/mac/release/Evernote_402491.dmg"));
        mTaskList.add(new DownloadTask("netroid-sample.apk", "http://netroid.cn/attach/netroid-sample.apk"));
        mTaskList.add(new DownloadTask("netroid-sample-wrong.apk", "http://netroid.cn/attach/netroid-sample-wrong.apk"));
        mTaskList.add(new DownloadTask("netroid_request_handling_flowchart.png", "http://netroid.cn/netroid_request_handling_flowchart.png"));
        mTaskList.add(new DownloadTask("91assistant_3.9_295.apk", "http://dl.sj.91.com/msoft/91assistant_3.9_295.apk"));
        mTaskList.add(new DownloadTask("BaiduRoot_2001.apk", "http://bs.baidu.com/easyroot/BaiduRoot_2001.apk"));
        mTaskList.add(new DownloadTask("Baidumusic_yinyuehexzfc.apk", "http://music.baidu.com/cms/mobile/static/apk/Baidumusic_yinyuehexzfc.apk"));
        mTaskList.add(new DownloadTask("MacWeChat-zh_CN.dmg", "http://dldir1.qq.com/foxmail/Mac/WeChat-zh_CN.dmg"));
        mTaskList.add(new DownloadTask("XamarinInstaller.dmg", "http://download.xamarin.com/Installer/Mac/XamarinInstaller.dmg"));

        lotBtnPanel = findViewById(R.id.lotBtnPanel);

        btnAddTask = (Button) findViewById(R.id.btnAddTask);
        btnAddTask.setText("添加任务(" + mTaskList.size() + ")");
        btnAddTask.setOnClickListener(this);

        mDownloadList = new LinkedList<DownloadTask>();

        ListView lsvTaskCollector = (ListView) findViewById(R.id.lsvTaskCollector);
        mAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mDownloadList.size();
            }

            @Override
            public DownloadTask getItem(int position) {
                return mDownloadList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.download_task_item, null);
                }

                DownloadTask task = getItem(position);
                TextView txvTaskName = (TextView) convertView.findViewById(R.id.txvTaskName);
                txvTaskName.setText(task.storeFileName);

                task.txvFileSize = (TextView) convertView.findViewById(R.id.txvFileSize);
                task.txvDownloadedSize = (TextView) convertView.findViewById(R.id.txvDownloadedSize);

                task.btnStatus = (Button) convertView.findViewById(R.id.btnStatus);
                task.btnStatus.setTag(task.storeFileName);

                task.invalidate();

                return convertView;
            }
        };
        lsvTaskCollector.setAdapter(mAdapter);
        lsvTaskCollector.setOnItemClickListener(this);
        lsvTaskCollector.setOnItemLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mTaskList.size() == 1) {
            lotBtnPanel.setVisibility(View.GONE);
        }

        final DownloadTask task = mTaskList.poll();
        final File storeFile = new File(mSaveDir, task.storeFileName);

        task.controller = mDownloder.add(storeFile, task.url, new Listener<Void>() {
            @Override
            public void onPreExecute() {
                task.invalidate();
            }

            @Override
            public void onSuccess(Void response) {
                showToast(task.storeFileName + " Success!");
            }

            @Override
            public void onError(NetroidError error) {
                NetroidLog.e(error.getMessage());
            }

            @Override
            public void onFinish() {
                NetroidLog.e("onFinish size : %s", Formatter.formatFileSize(
                        FileDownloadActivity.this, storeFile.length()));
                task.invalidate();
            }

            @Override
            public void onProgressChange(long fileSize, long downloadedSize) {
                task.onProgressChange(fileSize, downloadedSize);
//				NetroidLog.e("---- fileSize : " + fileSize + " downloadedSize : " + downloadedSize);
            }
        });
        mDownloadList.add(task);
        mAdapter.notifyDataSetChanged();
        btnAddTask.setText("添加任务(" + mTaskList.size() + ")");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DownloadTask task = mDownloadList.get(position);
        switch (task.controller.getStatus()) {
            case FileDownloader.DownloadController.STATUS_DOWNLOADING:
                task.controller.pause();
                task.invalidate();
                break;
            case FileDownloader.DownloadController.STATUS_PAUSE:
                task.controller.resume();
                task.invalidate();
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final DownloadTask task = mDownloadList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("是否删除”" + task.storeFileName + "“的下载任务？");
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.controller.discard();
                mDownloadList.remove(task);
                mAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
        return true;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class DownloadTask {
        FileDownloader.DownloadController controller;
        String storeFileName;
        String url;

        Button btnStatus;
        TextView txvFileSize;
        TextView txvDownloadedSize;

        long fileSize;
        long downloadedSize;

        private void onProgressChange(long fileSize, long downloadedSize) {
            this.fileSize = fileSize;
            this.downloadedSize = downloadedSize;
            invalidate();
        }

        private void invalidate() {
            if (btnStatus == null) return;
            if (!TextUtils.equals((CharSequence) btnStatus.getTag(), storeFileName)) return;

            switch (controller.getStatus()) {
                case FileDownloader.DownloadController.STATUS_DOWNLOADING:
                    if (fileSize > 0 && downloadedSize > 0) {
                        btnStatus.setText(DECIMAL_POINT.format(downloadedSize * 1.0f / fileSize * 100) + '%');
                    } else {
                        btnStatus.setText("0%");
                    }
                    break;
                case FileDownloader.DownloadController.STATUS_WAITING:
                    btnStatus.setText("waiting");
                    break;
                case FileDownloader.DownloadController.STATUS_PAUSE:
                    btnStatus.setText("paused");
                    break;
                case FileDownloader.DownloadController.STATUS_SUCCESS:
                    btnStatus.setText("done");
                    break;
            }

            txvDownloadedSize.setText(Formatter.formatFileSize(FileDownloadActivity.this, downloadedSize));
            txvFileSize.setText(Formatter.formatFileSize(FileDownloadActivity.this, fileSize));
        }

        private DownloadTask(String storeFileName, String url) {
            this.storeFileName = storeFileName;
            this.url = url;
        }
    }

    @Override
    public void finish() {
        mDownloder.clearAll();
        super.finish();
    }
}