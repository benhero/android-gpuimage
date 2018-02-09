package jp.co.cyberagent.android.gpuimage.sample.data;

import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

import jp.co.cyberagent.android.gpuimage.sample.utils.NumberUtils;

import static android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH;

/**
 * 视频原始信息类
 *
 * @author Benhero
 */
public class VideoInfo {
    /**
     * 路径
     */
    private String mPath;
    /**
     * 视频时长
     */
    private int mDuration;
    /**
     * 原始视频附带的旋转角度
     */
    private int mDegrees;
    /**
     * 比特率
     */
    private int mBitRate;
    /**
     * 视频宽 - 数据存储：可能由于旋转角度导致和展示上看到的不一致
     */
    private int mWidth;
    /**
     * 视频高 - 数据存储：可能由于旋转角度导致和展示上看到的不一致
     */
    private int mHeight;
    /**
     * 视频宽 - 展示播放时
     */
    private int mDisplayWidth;
    /**
     * 视频高 - 展示播放时
     */
    private int mDisplayHeight;

    public VideoInfo() {
    }

    public VideoInfo(String path) {
        mPath = path;
        initMetadata();
    }

    /**
     * 初始化视频信息
     */
    private void initMetadata() {
        if (TextUtils.isEmpty(mPath)) {
            return;
        }

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mPath);
            mDegrees = NumberUtils.getInteger(retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION));
            mDuration = NumberUtils.getInteger(retriever.extractMetadata(METADATA_KEY_DURATION));
            mBitRate = NumberUtils.getInteger(retriever.extractMetadata(METADATA_KEY_BITRATE));
            mWidth = NumberUtils.getInteger(retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH));
            mHeight = NumberUtils.getInteger(retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT));
            mDisplayWidth = isDisplayRotate() ? mHeight : mWidth;
            mDisplayHeight = isDisplayRotate() ? mWidth : mHeight;
            retriever.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("JKL", toString());
    }


    @Override
    public String toString() {
        return "Path:" + mPath
                + "\n mBitRate:" + mBitRate
                + "\n Width:" + mWidth + ", Height:" + mHeight
                + "\n Degrees:" + mDegrees + ", Duration:" + mDuration;
    }

    public String getPath() {
        return mPath;
    }

    public int getDuration() {
        return mDuration;
    }

    public int getDegrees() {
        return mDegrees;
    }

    public int getBitRate() {
        return mBitRate;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    /**
     * 视觉上和数据存储宽高是否需要转置
     */
    public boolean isDisplayRotate() {
        return mDegrees == 90 || mDegrees == 270;
    }

}
