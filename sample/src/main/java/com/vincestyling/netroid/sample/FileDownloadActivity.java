package com.vincestyling.netroid.sample;

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
import com.vincestyling.netroid.request.FileDownloadRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.toolbox.FileDownloader;
import com.vincestyling.netroid.toolbox.FileDownloader.DownloadController;

import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;

/**
 * This sample demonstrating the FileDownloader component which just like the ImageLoader.
 * FileDownloader brings us two main functionalities, download task management and continuous
 * transmission on the breakpoint, it's a good choice for multiple tasks purpose.
 */
public class FileDownloadActivity extends BaseActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    public final static DecimalFormat DECIMAL_POINT = new DecimalFormat("0.0");

    private LinkedList<DownloadTask> mTaskList;
    private LinkedList<DownloadTask> mDownloadList;
    private File mSaveDir;

    private View lotBtnPanel;
    private Button btnAddTask;
    private BaseAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_downloader);

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
        btnAddTask.setOnClickListener(this);
        invalidateBtn();

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
                Holder holder;
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.download_task_item, null);
                    holder = new Holder();
                    holder.btnStatus = (Button) convertView.findViewById(R.id.btnStatus);
                    holder.txvTaskName = (TextView) convertView.findViewById(R.id.txvTaskName);
                    holder.txvFileSize = (TextView) convertView.findViewById(R.id.txvFileSize);
                    holder.txvDownloadedSize = (TextView) convertView.findViewById(R.id.txvDownloadedSize);
                    convertView.setTag(holder);
                } else {
                    holder = (Holder) convertView.getTag();
                }

                DownloadTask task = getItem(position);
                holder.txvTaskName.setText(task.storeFileName);

                task.txvFileSize = holder.txvFileSize;
                task.txvDownloadedSize = holder.txvDownloadedSize;

                task.btnStatus = holder.btnStatus;
                task.btnStatus.setTag(task.storeFileName);

                task.invalidate();

                return convertView;
            }

            class Holder {
                TextView txvTaskName;
                TextView txvFileSize;
                TextView txvDownloadedSize;
                Button btnStatus;
            }
        };
        lsvTaskCollector.setAdapter(mAdapter);
        lsvTaskCollector.setOnItemClickListener(this);
        lsvTaskCollector.setOnItemLongClickListener(this);
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        Netroid.init(null);

        Netroid.setFileDownloder(new FileDownloader(Netroid.getRequestQueue(), 1) {
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
        });
    }

    @Override
    public void onClick(View v) {
        if (mTaskList.size() == 1) {
            lotBtnPanel.setVisibility(View.GONE);
        }

        final DownloadTask task = mTaskList.poll();
        final File storeFile = new File(mSaveDir, task.storeFileName);

        task.controller = Netroid.getFileDownloader().add(storeFile, task.url, new Listener<Void>() {
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
                AppLog.e("onError : %s", error.getMessage());
            }

            @Override
            public void onFinish() {
                AppLog.e("onFinish size : %s", Formatter.formatFileSize(
                        FileDownloadActivity.this, storeFile.length()));
                task.invalidate();
            }

            @Override
            public void onProgressChange(long fileSize, long downloadedSize) {
                task.onProgressChange(fileSize, downloadedSize);
//				AppLog.e("---- fileSize : " + fileSize + " downloadedSize : " + downloadedSize);
            }
        });
        mDownloadList.add(task);
        mAdapter.notifyDataSetChanged();
        invalidateBtn();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DownloadTask task = mDownloadList.get(position);
        switch (task.controller.getStatus()) {
            case DownloadController.STATUS_DOWNLOADING:
            case DownloadController.STATUS_WAITING:
                task.controller.pause();
                task.invalidate();
                break;
            case DownloadController.STATUS_PAUSE:
                task.controller.resume();
                task.invalidate();
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final DownloadTask task = mDownloadList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("do you want to remove \"" + task.storeFileName + "\" task？");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.controller.discard();
                mDownloadList.remove(task);
                mAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

    private void invalidateBtn() {
        btnAddTask.setText("Add Task(" + mTaskList.size() + ")");
    }

    private class DownloadTask {
        DownloadController controller;
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
                case DownloadController.STATUS_DOWNLOADING:
                    if (fileSize > 0 && downloadedSize > 0) {
                        btnStatus.setText(DECIMAL_POINT.format(downloadedSize * 1.0f / fileSize * 100) + '%');
                    } else {
                        btnStatus.setText("0%");
                    }
                    break;
                case DownloadController.STATUS_WAITING:
                    btnStatus.setText("waiting");
                    break;
                case DownloadController.STATUS_PAUSE:
                    btnStatus.setText("paused");
                    break;
                case DownloadController.STATUS_SUCCESS:
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
}
