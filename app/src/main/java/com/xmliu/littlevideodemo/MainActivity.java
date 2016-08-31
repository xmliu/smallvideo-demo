package com.xmliu.littlevideodemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FullVideoView mVideoView = null;
    private RelativeLayout contentLayout;
    private TextView videoUploadTV;
    private ImageView videoThumb;
    private ImageView videoDeleteIV;
    private Button videoPlayBtn;
    private Handler mHandler;
    private String mediaUrl;

    public static final int REQUEST_UPLOAD_VIDEO = 0x005;
    public static final String SD_PATH = Environment
            .getExternalStorageDirectory().getAbsolutePath();
    public static final String BASE_UPLOAD_VIDEO_PATH = SD_PATH+"/Xmliu/Media/Video/";
    private File dir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
//                    case Constants.Tags.HANDLER_PAY_SUCCESS:
//                        Bitmap bmp = createVideoThumbnail(mediaUrl, 300, 100);
//
//                        videoThumb.setImageBitmap(bmp);
//                        break;
                }
            }
        };

        // 创建文件夹存放视频
        dir = new File(BASE_UPLOAD_VIDEO_PATH);
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            Log.e("TAG", "create dir error", e);
        }

        contentLayout = (RelativeLayout) findViewById(R.id.content_layout);
        mVideoView = (FullVideoView) findViewById(R.id.video_view);
        videoPlayBtn = (Button) findViewById(R.id.video_play_btn);

        videoUploadTV = (TextView) findViewById(R.id.video_upload);
        videoThumb = (ImageView) findViewById(R.id.video_thumb);
        videoDeleteIV = (ImageView) findViewById(R.id.video_delete);


        videoPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentLayout.setVisibility(View.GONE);
                //使用videoView播放器
                mVideoView.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(mediaUrl)) {
                    Uri uri = Uri.parse(mediaUrl);
                    mVideoView.setVideoURI(uri);
                } else {
                    mVideoView.setVideoPath(BASE_UPLOAD_VIDEO_PATH
                            + "upload.mp4");
                }
                mVideoView.setMediaController(new MediaController(
                        MainActivity.this));// 显示视频播放控制栏
                mVideoView.requestFocus();
                // mVideoView.setSystemUiVisibility(View.INVISIBLE);//隐藏系统导航栏，sdk最低为11
                mVideoView.start();
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mVideoView.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        });
        videoDeleteIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPlayBtn.setVisibility(View.GONE);

                videoDeleteIV.setVisibility(View.GONE);
                videoUploadTV.setVisibility(View.VISIBLE);
                videoThumb.setImageResource(R.drawable.common_image_loading);//默认图

            }
        });

        videoUploadTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivityForResult(new Intent(MainActivity.this,
                        RecordVideoActivity.class), REQUEST_UPLOAD_VIDEO);

            }
        });
    }

    private Bitmap createVideoThumbnail(String url, int width, int height) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int kind = MediaStore.Video.Thumbnails.MINI_KIND;
        try {
            if (Build.VERSION.SDK_INT >= 14) {
                retriever.setDataSource(url, new HashMap<String, String>());
            } else {
                retriever.setDataSource(url);
            }
            bitmap = retriever.getFrameAtTime();
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        if (kind == MediaStore.Images.Thumbnails.MICRO_KIND && bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_UPLOAD_VIDEO:
                if (resultCode == 0x006) {
                    videoUploadTV.setVisibility(View.GONE);
                    videoDeleteIV.setVisibility(View.VISIBLE);
                    videoPlayBtn.setVisibility(View.VISIBLE);
//                    addVideo(BASE_UPLOAD_VIDEO_PATH + "upload.mp4");
                    // 获取视频第一帧
                    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(
                            BASE_UPLOAD_VIDEO_PATH + "upload.mp4",
                            MediaStore.Video.Thumbnails.MINI_KIND);
                    videoThumb.setImageBitmap(bitmap);
                }

                break;
        }
    }
}
