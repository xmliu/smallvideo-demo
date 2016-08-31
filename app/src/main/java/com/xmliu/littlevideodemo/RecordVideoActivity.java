package com.xmliu.littlevideodemo;

import android.app.Activity;
import android.app.Dialog;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class RecordVideoActivity extends Activity implements
        OnClickListener, SurfaceHolder.Callback {
    private static final String TAG = RecordVideoActivity.class.getSimpleName();

    private File videoFile;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ImageView switchCameraIV;
    private Button recordBtn;
    private ImageView zoomBtn;
    private ImageView cancelIV;
    private MediaRecorder recorder;
    private Camera myCamera = null;
    private int cameraFront = 1;// 1前置；0后置

    private int recordState = 0; // 拍摄状态
    private static final int RECORD_OFF = 0; // 不在拍摄
    private static final int RECORD_ON = 1; // 正在拍摄

    private Chronometer mChronometer;
    private long timeLeftInS = 0;

    public static final int RESULT_UPLOAD_VIDEO = 0x006;

    private ProgressBar progressBar;
    private int curPro;
    private int maxPro;

    private int RECORD_SECOND = 5;//录制的秒数
    private float downY;

    private Dialog mRecordDialog;
    private TextView recordCancel1TV;
    private TextView recordCancel2TV;
    private boolean moveState = false; // 手指是否移动
    private boolean isScaleBig = true;
    private float recodeTime = 0.0f; // 录音时长

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_recoder_video);

        mSurfaceView = (SurfaceView) findViewById(R.id.videoView);
        switchCameraIV = (ImageView) findViewById(R.id.switch_camera);
        progressBar = (ProgressBar) findViewById(R.id.pb_progress);
        recordBtn = (Button) findViewById(R.id.start);
        zoomBtn = (ImageView) findViewById(R.id.zoom);
        cancelIV = (ImageView) findViewById(R.id.cancel);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mChronometer
                .setOnChronometerTickListener(new OnChronometerTickListener() {

                    @Override
                    public void onChronometerTick(Chronometer chronometer) {
                        // TODO Auto-generated method stub
                        if (SystemClock.elapsedRealtime()
                                - mChronometer.getBase() > RECORD_SECOND * 1000) {

                            mChronometer.stop();
                            mChronometer.setVisibility(View.GONE);

                            releaseRecoder();
                            if (!moveState) {
                                releaseCamera();
                            }

                            mRecordDialog.dismiss();
                            setResult(RESULT_UPLOAD_VIDEO);
                            RecordVideoActivity.this.finish();
                        }
                    }
                });


        initProgress();

        zoomBtn.setOnClickListener(this);
        cancelIV.setOnClickListener(this);
        switchCameraIV.setOnClickListener(this);

        recordBtn.setOnTouchListener(new View.OnTouchListener() {

            long time = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {


                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN: // 按下按钮

                        if (recordState != RECORD_ON) {
                            time = System.currentTimeMillis();
                            downY = event.getY();
                            recordState = RECORD_ON;
                            switchCameraIV.setVisibility(View.GONE);

//							mChronometer.setBase(SystemClock.elapsedRealtime());

                            recorder();
                            initTimer(RECORD_SECOND + 1);
                            mChronometer.setVisibility(View.VISIBLE);
                            mChronometer.start();
                        }


                        break;
                    case MotionEvent.ACTION_MOVE: // 滑动手指


                        float moveY = event.getY();
                        if (downY - moveY > 50) {
                            moveState = true;
                            showStatusDialog(1);
                        }
                        if (downY - moveY < 20) {
                            moveState = false;
                            showStatusDialog(0);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:

                        time = System.currentTimeMillis() - time;
                        if (recordState == RECORD_ON) {
                            recordState = RECORD_OFF;
                            if (mRecordDialog.isShowing()) {
                                mRecordDialog.dismiss();
                            }

                            mChronometer.stop();
                            mChronometer.setVisibility(View.GONE);
                            releaseRecoder();
                            switchCameraIV.setVisibility(View.VISIBLE);

                            if (!moveState) {
                                time = time / 1000;
                                if (time < 1) {
                                    Toast.makeText(RecordVideoActivity.this, "时间太短，请重新录制", Toast.LENGTH_SHORT).show();
                                    initProgress();
                                } else {
                                    setResult(RESULT_UPLOAD_VIDEO);
                                    RecordVideoActivity.this.finish();
                                }
                            } else {
                                // 如果取消录制，则......
                                initProgress();
                            }
                            moveState = false;
                        }


                        break;
                }
                return false;
            }
        });

    }

    private void initProgress() {
        //进度条
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(progressBar.getMax());
        //保存第一进度和第二进度的数据
        curPro = progressBar.getProgress();
        maxPro = progressBar.getMax();
    }


    private void showStatusDialog(int type) {

        if (mRecordDialog == null) {
            mRecordDialog = new Dialog(RecordVideoActivity.this,
                    R.style.DialogStyle);
            mRecordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mRecordDialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mRecordDialog.setContentView(R.layout.dialog_record_status);
            recordCancel1TV = (TextView) mRecordDialog
                    .findViewById(R.id.record_cancel1);
            recordCancel2TV = (TextView) mRecordDialog
                    .findViewById(R.id.record_cancel2);
        }
        switch (type) {
            case 1:
                recordCancel1TV.setVisibility(View.GONE);
                recordCancel2TV.setVisibility(View.VISIBLE);
                break;

            default:
                recordCancel1TV.setVisibility(View.VISIBLE);
                recordCancel2TV.setVisibility(View.GONE);
                break;
        }
        mRecordDialog.show();

    }

    public void setZoom(int zoomKey) {
        //判断是否支持变焦
        if (myCamera.getParameters().isZoomSupported()) {
            try {
                Camera.Parameters parameters = myCamera.getParameters();
                int zoomValue = parameters.getZoom();
                Log.i("ZOOM", "zoomValue=" + zoomValue);
                switch (zoomKey) {
                    case 1:
                        if (zoomValue <= 14)
                            zoomValue += 4;

                        break;
                    case 2:
                        if (zoomValue >= 4)
                            zoomValue -= 4;
                        break;
                    default:
                        break;
                }
                parameters.setZoom(zoomValue);
                myCamera.setParameters(parameters);
            } catch (Exception e) {
                Log.i("ZOOM", "exception zoom");
                e.printStackTrace();
            }
        } else {
            Log.i("ZOOM", "NOT support zoom");
        }
    }

    public void recorder() {
        try {
            // 初始化recoder对象
            recorder = new MediaRecorder();
            recorder.reset();

            myCamera.stopPreview();// 加上这行，在部分手机上就不会出现停止预览的情况，
            // 以下两行代码加上会出现保存的视频可能为花屏
            myCamera.unlock();// 从Android4.0开始，这个方法由系统自动调用
            recorder.setCamera(myCamera);

            videoFile = new File(MainActivity.BASE_UPLOAD_VIDEO_PATH
                    + "upload.mp4");
            if (videoFile.exists()) {
                videoFile.delete();
            }
            videoFile.createNewFile();

            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);// 视频源
//			recorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 录音源为麦克风

            CamcorderProfile profile = CamcorderProfile
                    .get(CamcorderProfile.QUALITY_HIGH);
            recorder.setVideoEncodingBitRate(profile.videoBitRate);

            recorder.setAudioChannels(profile.audioChannels);
            recorder.setAudioEncodingBitRate(profile.audioBitRate);
            recorder.setAudioSamplingRate(profile.audioSampleRate);

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//			recorder.setAudioEncoder(profile.audioCodec);
            recorder.setVideoEncoder(profile.videoCodec);

            recorder.setVideoSize(640, 480);// 视频尺寸
            recorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.GINGERBREAD) {
                Toast.makeText(RecordVideoActivity.this,
                        "由于系统版本过低，拍摄的视频会旋转90度", Toast.LENGTH_SHORT).show();
            } else {
                if (cameraFront == 0) {
                    recorder.setOrientationHint(90);// 设置保存后的视频文件旋转90度
                } else {
                    recorder.setOrientationHint(270);// 设置保存后的视频文件旋转90度
                }
            }
            recorder.setPreviewDisplay(mSurfaceHolder.getSurface());// 预览
            recorder.setOutputFile(videoFile.getAbsolutePath());// 保存路径
            recorder.setMaxDuration(RECORD_SECOND * 1000);// 最大期限
            recorder.setOnInfoListener(new OnInfoListener() {
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.i("TAG", "已经达到最长录制时间");
                        mChronometer.stop();
                        mChronometer.setVisibility(View.GONE);
                        releaseRecoder();
                        if (!moveState) {
                            releaseCamera();
                        }
                    }
                }
            });
            recorder.setOnErrorListener(new OnErrorListener() {

                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    releaseRecoder();
                    releaseCamera();
                    Log.i("TAG", "MediaRecorder:" + mr.toString() + ";what:"
                            + what + ";extra:" + extra);
                }
            });
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            myCamera = Camera.open(Camera.getNumberOfCameras() - 1);
            System.out.println("open()方法有问题");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.zoom:

                if (isScaleBig) {
                    // 放大
                    setZoom(1);
                    isScaleBig = false;
                } else {
                    // 缩小
                    setZoom(2);
                    isScaleBig = true;
                }
                break;
            case R.id.cancel:

                releaseRecoder();
                releaseCamera();

                RecordVideoActivity.this.finish();
                break;
            case R.id.switch_camera:

                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

                for (int i = 0; i < cameraCount; i++) {

                    Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
                    if (cameraFront == 0) {
                        //现在是后置，变更为前置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置

                            myCamera.stopPreview();//停掉原来摄像头的预览
                            myCamera.release();//释放资源
                            myCamera = null;//取消原来摄像头
                            myCamera = Camera.open(i);//打开当前选中的摄像头
                            try {
//							deal(myCamera);
                                myCamera.setDisplayOrientation(90);
                                myCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            myCamera.startPreview();//开始预览
                            cameraFront = 1;
                            break;
                        }
                    } else {
                        //现在是前置， 变更为后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                            myCamera.stopPreview();//停掉原来摄像头的预览
                            myCamera.release();//释放资源
                            myCamera = null;//取消原来摄像头
                            myCamera = Camera.open(i);//打开当前选中的摄像头
                            try {
//							deal(myCamera);
                                myCamera.setDisplayOrientation(90);
                                myCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            myCamera.startPreview();//开始预览
                            cameraFront = 0;
                            break;
                        }
                    }

                }


                break;
            default:
                break;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mSurfaceHolder = holder;
        try {
            // 设置参数
//			myCamera = Camera.open();
            if (cameraFront == 0) {
                myCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else if (cameraFront == 1) {
                myCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            myCamera.setDisplayOrientation(90);// 设置预览时的视频文件旋转90度
            // 摄像头画面显示在Surface上
            myCamera.setPreviewDisplay(mSurfaceHolder);

        } catch (IOException e) {
            if (myCamera != null)
                myCamera.release();
            myCamera = null;
        } catch (RuntimeException e) {
            myCamera = Camera.open(Camera.getNumberOfCameras() - 1);
            Log.i(TAG, "open()方法有问题！");
        }

        if (myCamera == null) {
            finish();
            return;
        }
        myCamera.startPreview();


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.i(TAG, "surfaceChanged" + width + "*****" + height);
        mSurfaceHolder = holder;
//		Camera.Parameters mParameters = myCamera.getParameters();
//		Camera.Size bestSize = null;
//
//		List<Camera.Size> sizeList = myCamera.getParameters().getSupportedPreviewSizes();
//		bestSize = sizeList.get(0);
//
//		for(int i = 1; i < sizeList.size(); i++){
//			if((sizeList.get(i).width * sizeList.get(i).height) >
//					(bestSize.width * bestSize.height)){
//				bestSize = sizeList.get(i);
//			}
//		}
//
//		mParameters.setPreviewSize(bestSize.width, bestSize.height);
//		myCamera.setParameters(mParameters);
//		myCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mSurfaceView = null;

        mSurfaceHolder = null;

        releaseRecoder();
        releaseCamera();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        releaseRecoder();
        releaseCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            releaseRecoder();
            releaseCamera();
            finish(); // finish当前activity
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void releaseRecoder() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.reset();
            } catch (RuntimeException e) {
                videoFile.delete(); // you must delete the outputfile when
                // the recorder stop failed.
            } finally {
                recorder.release();
                recorder = null;
            }
        }
    }

    private void releaseCamera() {
        if (myCamera != null) {
            myCamera.stopPreview();
            myCamera.release();
            myCamera = null;
        }
    }

    /**
     * 初始化计时器，计时器是通过widget.Chronometer来实现的
     *
     * @param total 一共多少秒
     */
    private void initTimer(long total) {
        this.timeLeftInS = total;
        mChronometer
                .setOnChronometerTickListener(new OnChronometerTickListener() {

                    @Override
                    public void onChronometerTick(Chronometer chronometer) {
                        if (timeLeftInS <= 0) {
                            mChronometer.stop();
                            mChronometer.setVisibility(View.GONE);
//							recordTime = RECORD_SECOND - timeLeftInS;
                            return;
                        }

                        timeLeftInS--;
                        refreshTimeLeft();
                    }
                });
    }

    /**
     * 将倒计时显示在屏幕上 初步决定放在右下角
     */
    private void refreshTimeLeft() {
        progressBar.setProgress(curPro += 1);
        progressBar.setSecondaryProgress(maxPro -= 1);

        if (String.valueOf(timeLeftInS).length() == 1) {
            this.mChronometer.setText("剩余：00:0" + timeLeftInS);
        } else {
            this.mChronometer.setText("剩余：00:" + timeLeftInS);
        }
    }

}
