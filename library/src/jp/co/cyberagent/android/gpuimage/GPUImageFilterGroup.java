/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.GPUImageRenderer.CUBE;
import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
public class GPUImageFilterGroup extends GPUImageFilter {
    /**
     * 多层级的滤镜列表，包含了group
     */
    protected List<GPUImageFilter> mFilters = new ArrayList<>();
    /**
     * 单层级的滤镜列表，不包含group，拆分自mFilters
     */
    protected List<GPUImageFilter> mMergedFilters = new ArrayList<>();
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    /**
     * 位置坐标
     */
    private final FloatBuffer mGLCubeBuffer;
    /**
     * 纹理坐标
     */
    private final FloatBuffer mGLTextureBuffer;
    /**
     * TODO : 处理纹理翻转
     */
    private final FloatBuffer mGLTextureFlipBuffer;

    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    public GPUImageFilterGroup() {
        this(null);
    }

    /**
     * Instantiates a new GPUImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    public GPUImageFilterGroup(List<GPUImageFilter> filters) {
        mFilters.addAll(filters);
        updateMergedFilters();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
        mGLTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureFlipBuffer.put(flipTexture).position(0);
    }

    public void addFilter(GPUImageFilter filter) {
        if (filter == null) {
            return;
        }
        mFilters.add(filter);
        updateMergedFilters();
    }

    @Override
    public void onInit() {
        super.onInit();
        for (GPUImageFilter filter : mFilters) {
            filter.init();
        }
    }

    @Override
    public void onDestroy() {
        destroyFrameBuffers();
        for (GPUImageFilter filter : mFilters) {
            filter.destroy();
        }
        super.onDestroy();
    }

    private void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        if (mFrameBuffers != null) {
            destroyFrameBuffers();
        }

        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            mFilters.get(i).onOutputSizeChanged(width, height);
        }

        if (mMergedFilters != null && mMergedFilters.size() > 0) {
            size = mMergedFilters.size();
            mFrameBuffers = new int[size - 1];
            mFrameBufferTextures = new int[size - 1];

            // 创建FrameBuffer
            for (int i = 0; i < size - 1; i++) {
                // 1. 创建FrameBuffer
                GLES20.glGenFramebuffers(1, mFrameBuffers, i);
                // 2. 生成纹理对象
                GLES20.glGenTextures(1, mFrameBufferTextures, i);
                // 3. 绑定纹理对象
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
                // 4. 设置纹理对象的相关信息：颜色模式、大小
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // 纹理过滤参数设置
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                // 绑定FrameBuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                // 将纹理对象挂载到FrameBuffer上，存储颜色信息
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);

                // 解绑纹理
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                // 解绑FrameBuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    @SuppressLint("WrongCall")
    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return;
        }
        int size = mMergedFilters.size();
        int previousTexture = textureId;
        for (int i = 0; i < size; i++) {
            GPUImageFilter filter = mMergedFilters.get(i);
            boolean isNotLast = i < size - 1;
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                GLES20.glClearColor(0, 0, 0, 0);
            }

            if (i == 0) {
                filter.onDraw(previousTexture, cubeBuffer, textureBuffer);
            } else if (i == size - 1) {
                filter.onDraw(previousTexture, mGLCubeBuffer, (size % 2 == 0) ? mGLTextureFlipBuffer : mGLTextureBuffer);
            } else {
                filter.onDraw(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
            }

            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                previousTexture = mFrameBufferTextures[i];
            }
        }
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<GPUImageFilter> getFilters() {
        return mFilters;
    }

    public List<GPUImageFilter> getMergedFilters() {
        return mMergedFilters;
    }

    public void updateMergedFilters() {
        if (mFilters.isEmpty()) {
            return;
        }
        mMergedFilters.clear();
        List<GPUImageFilter> filters;
        for (GPUImageFilter filter : mFilters) {
            if (filter instanceof GPUImageFilterGroup) {
                ((GPUImageFilterGroup) filter).updateMergedFilters();
                filters = ((GPUImageFilterGroup) filter).getMergedFilters();
                if (filters == null || filters.isEmpty())
                    continue;
                mMergedFilters.addAll(filters);
                continue;
            }
            mMergedFilters.add(filter);
        }
    }
}
