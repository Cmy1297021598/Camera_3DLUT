package com.demo.demos.render;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import com.demo.demos.filter.ColorFilter;
import com.demo.demos.filter.CameraFilter;
import com.demo.demos.utils.MatrixUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.glReadPixels;
import static android.opengl.GLES30.*;
import static com.demo.demos.utils.CommonUtil.TAG;

/**
 * Created by wangyt on 2019/5/21
 */
public class CameraPreviewRender implements GLSurfaceView.Renderer {

    boolean useFront = false;
    float[] matrix = new float[16];

    boolean takingPhoto = false;
    boolean recordingVideo = false;

    SurfaceTexture surfaceTexture;
    int[] cameraTexture = new int[1];

    CameraFilter cameraFilter;
    ColorFilter colorFilter;
    int width, height;

    int[] exportFrame = new int[1];
    int[] exportTexture = new int[1];

    public CameraPreviewRender() {
        cameraFilter = new CameraFilter();
        colorFilter = new ColorFilter();
    }

    public void setUseFront(boolean useFront) {
        if (this.useFront != useFront) {
            this.useFront = useFront;
            cameraFilter.setUseFront(useFront);
            matrix = MatrixUtil.flip(matrix, true, false);
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public boolean isTakingPhoto() {
        return takingPhoto;
    }

    public void setTakingPhoto(boolean takingPhoto) {
        this.takingPhoto = takingPhoto;
    }

    public boolean isRecordingVideo() {
        return recordingVideo;
    }

    public void setRecordingVideo(boolean recordingVideo) {
        this.recordingVideo = recordingVideo;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        createTexture();
        surfaceTexture = new SurfaceTexture(cameraTexture[0]);

        cameraFilter.onSurfaceCreated();
        colorFilter.onSurfaceCreated();
        matrix = MatrixUtil.flip(colorFilter.getMatrix(), false, true);
        colorFilter.setMatrix(matrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            cameraFilter.onSurfaceChanged(width, height);
            colorFilter.onSurfaceChanged(width, height);

            delFrameBufferAndTexture();
            genFrameBufferAndTexture();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }

        // 测试计算时间
        //long s1 = System.currentTimeMillis();

        cameraFilter.setTextureId(cameraTexture);
        cameraFilter.onDraw();

        colorFilter.setTextureId(cameraFilter.getOutputTextureId());

        //long s2 = System.currentTimeMillis();

        //Log.e("==>> onDrawFrame:","time=" + (s2-s1) + "ms");

        if (isTakingPhoto()) {
            ByteBuffer exportBuffer = ByteBuffer.allocate(width * height * 4);

            bindFrameBufferAndTexture();
            colorFilter.setMatrix(MatrixUtil.flip(matrix, false, true));
            colorFilter.onDraw();
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, exportBuffer);
            savePhoto(exportBuffer);
            unBindFrameBuffer();

            setTakingPhoto(false);
            colorFilter.setMatrix(MatrixUtil.flip(matrix, false, true));
        } else {
            colorFilter.onDraw();
        }
    }

    private void createTexture() {
        glGenTextures(cameraTexture.length, cameraTexture, 0);
    }

    public void delFrameBufferAndTexture() {
        glDeleteFramebuffers(exportFrame.length, exportFrame, 0);
        glDeleteTextures(exportTexture.length, exportTexture, 0);
    }

    public void genFrameBufferAndTexture() {
        glGenFramebuffers(exportFrame.length, exportFrame, 0);

        glGenTextures(exportTexture.length, exportTexture, 0);
        glBindTexture(GL_TEXTURE_2D, exportTexture[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        setTextureParameters();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void setTextureParameters() {
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    public void bindFrameBufferAndTexture() {
        glBindFramebuffer(GL_FRAMEBUFFER, exportFrame[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, exportTexture[0], 0);
    }

    public void unBindFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

//    public ByteBuffer getPixelBuffer(){
//        final ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
//        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
//        return buffer;
//    }

    public void savePhoto(final ByteBuffer buffer) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                String folderPath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
                File folder = new File(folderPath);
                if (!folder.exists() && !folder.mkdirs()) {
                    Log.e("demos", "图片目录异常");
                    return;
                }
                String filePath = folderPath + System.currentTimeMillis() + ".jpg";
                BufferedOutputStream bos = null;
                try {
                    FileOutputStream fos = new FileOutputStream(filePath);
                    bos = new BufferedOutputStream(fos);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null) {
                        try {
                            bos.flush();
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
            }
        }).start();
    }
}
