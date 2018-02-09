package jp.co.cyberagent.android.gpuimage.sample.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageLookupFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageOESFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.IRenderCallback;
import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools;
import jp.co.cyberagent.android.gpuimage.sample.R;
import jp.co.cyberagent.android.gpuimage.sample.data.VideoInfo;
import jp.co.cyberagent.android.gpuimage.sample.view.AspectFrameLayout;

/**
 * 视频处理页
 *
 * @author Benhero
 */
public class ActivityVideo extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, GPUImageView.OnPictureSavedListener {

    private static final int REQUEST_PICK_VIDEO = 1;
    private GPUImageFilter mFilter;
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;
    private GLSurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mHolder;
    private String mPath;
    private GPUImageRenderer mRenderer;
    private GPUImage mGPUImage;
    private AspectFrameLayout mAspectFrameLayout;
    private VideoInfo mVideoInfo;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
        findViewById(R.id.video_previous_filter).setOnClickListener(this);
        findViewById(R.id.video_next_filter).setOnClickListener(this);
        findViewById(R.id.button_save).setOnClickListener(this);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.video_surface);
        mAspectFrameLayout = (AspectFrameLayout) findViewById(R.id.video_surface_wrapper);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("video/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_VIDEO);
    }

    private void initPlayer() {
        double aspectRatio = mVideoInfo.getWidth() / (double) mVideoInfo.getHeight();
        if (mVideoInfo.isDisplayRotate()) {
            aspectRatio = mVideoInfo.getHeight() / (double) mVideoInfo.getWidth();
        }
        mAspectFrameLayout.setAspectRatio(aspectRatio);

        mMediaPlayer = new MediaPlayer();
        mGPUImage = new GPUImage(this, new IRenderCallback() {
            @Override
            public void onSurfaceTextureCreated(SurfaceTexture texture) {
                try {
                    Surface surface = new Surface(texture);
                    mMediaPlayer.reset();
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setSurface(surface);
                    mMediaPlayer.setDataSource(mPath);
                    mMediaPlayer.prepareAsync();
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.seekTo(0);
                            mMediaPlayer.start();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFrameAvailable(long frameTimeNanos) {
                mSurfaceView.requestRender();
            }
        });
        mGPUImage.setGLSurfaceView(mSurfaceView);
        mGPUImage.setRotation(Rotation.fromInt(mVideoInfo.getDegrees()));
    }

    private void play() {

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_VIDEO:
                if (resultCode == RESULT_OK) {
                    handleImage(data.getData());
                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.video_previous_filter:
                GPUImageFilterTools.showPreviousFilter(this, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {
                    @Override
                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                        switchFilterTo(filter);
                        mGPUImage.requestRender();
                    }
                });
                break;
            case R.id.video_next_filter:
                GPUImageFilterTools.showNextFilter(this, new GPUImageFilterTools.OnGpuImageFilterChosenListener() {
                    @Override
                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                        switchFilterTo(filter);
                        mGPUImage.requestRender();
                    }
                });
                break;
            case R.id.button_save:
                saveImage();
                break;

            default:
                break;
        }

    }

    @Override
    public void onPictureSaved(final Uri uri) {
        Toast.makeText(this, "Saved: " + uri.toString(), Toast.LENGTH_SHORT).show();
    }

    private void saveImage() {
        String fileName = System.currentTimeMillis() + ".jpg";
//        mGPUImageView.saveToPictures("GPUImage", fileName, this);
//        mGPUImageView.saveToPictures("GPUImage", fileName, 1600, 1600, this);
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            GPUImageFilterGroup gpuImageFilterGroup = new GPUImageFilterGroup();
            mFilter = gpuImageFilterGroup;
//            gpuImageFilterGroup.addFilter(filter);
            GPUImageLookupFilter amatorka = new GPUImageLookupFilter();
            amatorka.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.lookup_amatorka));
            gpuImageFilterGroup.addFilter(new GPUImageOESFilter());
            gpuImageFilterGroup.addFilter(filter);
            mGPUImage.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }
        mGPUImage.requestRender();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    private void handleImage(final Uri uri) {
        mPath = getRealPathFromURI(uri);
        mVideoInfo = new VideoInfo(mPath);
        initPlayer();
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
