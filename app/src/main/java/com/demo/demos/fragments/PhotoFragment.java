package com.demo.demos.fragments;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.demo.demos.R;
import com.demo.demos.base.BaseActivity;
import com.demo.demos.base.BaseFragment;
import com.demo.demos.utils.CameraUtils;
import com.demo.demos.views.AutoFitTextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoFragment extends BaseFragment {

    private static final SparseIntArray PHOTO_ORITATION = new SparseIntArray();

    static {
        PHOTO_ORITATION.append(Surface.ROTATION_0, 90);
        PHOTO_ORITATION.append(Surface.ROTATION_90, 0);
        PHOTO_ORITATION.append(Surface.ROTATION_180, 270);
        PHOTO_ORITATION.append(Surface.ROTATION_270, 180);
    }

    Button btnPhoto;
    AutoFitTextureView previewView;

    String cameraId;
    CameraManager cameraManager;
    List<Size> outputSizes;
    Size photoSize;
    CameraDevice cameraDevice;
    CameraCaptureSession captureSession;
    CaptureRequest.Builder previewRequestBuilder;
    CaptureRequest previewRequest;
    CaptureRequest.Builder photoRequestBuilder;
    CaptureRequest photoRequest;
    ImageReader photoReader;
    Surface previewSurface;
    Surface photoSurface;

    int cameraOritation;
    int displayRotation;

    public PhotoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCamera();

        initViews(view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void initCamera() {
        cameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getBackCameraId();
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        photoSize = outputSizes.get(0);
    }

    private void initViews(View view) {
        btnPhoto = view.findViewById(R.id.btn_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        previewView = view.findViewById(R.id.preview_view);
    }

    private void initReaderAndSurface() {

        //初始化拍照 ImageReader
        photoReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(), ImageFormat.JPEG, 2);
        photoReader.setOnImageAvailableListener(photoReaderImgListener, null);
        photoSurface = photoReader.getSurface();
    }

    @Override
    public void onResume() {
        super.onResume();

        ((BaseActivity) getActivity()).requestPermission("请给予相机、存储权限，以便app正常工作",
                new BaseActivity.Callback() {
                    @Override
                    public void success() {
                        if (previewView.isAvailable()) {
                            openCamera();
                        } else {
                            previewView.setSurfaceTextureListener(surfaceTextureListener);
                        }
                    }

                    @Override
                    public void failed() {
                        Toast.makeText(getContext(), "未授予相机、存储权限！", Toast.LENGTH_SHORT).show();
                    }
                },
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            displayRotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getOrientation();
            if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                previewView.setAspectRation(photoSize.getHeight(), photoSize.getWidth());
            } else {
                previewView.setAspectRation(photoSize.getWidth(), photoSize.getHeight());
            }
            configureTransform(previewView.getWidth(), previewView.getHeight());
            cameraManager.openCamera(cameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "相机访问异常");
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == previewView || null == photoSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, photoSize.getHeight(), photoSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / photoSize.getHeight(),
                    (float) viewWidth / photoSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        previewView.setTransform(matrix);
    }

    private void takePhoto() {
        try {
            photoRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            cameraOritation = PHOTO_ORITATION.get(displayRotation);
            photoRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraOritation);
            photoRequestBuilder.addTarget(photoSurface);
            photoRequest = photoRequestBuilder.build();

            captureSession.stopRepeating();
            captureSession.capture(photoRequest, sessionCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "相机访问异常");
        }
    }

    private void writeImageToFile() {
        String filePath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/001.jpg";
        Image image = photoReader.acquireNextImage();
        if (image == null) {
            return;
        }
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(filePath));
            fos.write(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
                fos = null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                image = null;
            }
        }
    }

    private void releaseCamera() {
        CameraUtils.getInstance().releaseImageReader(photoReader);
        CameraUtils.getInstance().releaseCameraSession(captureSession);
        CameraUtils.getInstance().releaseCameraDevice(cameraDevice);
    }

    /********************************** listener/callback **************************************/
    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //启动相机
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "相机已启动");
            //初始化 ImageReader 和 Surface
            initReaderAndSurface();

            cameraDevice = camera;
            try {
                //初始化预览 Surface
                SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
                if (surfaceTexture == null) {
                    return;
                }

                surfaceTexture.setDefaultBufferSize(photoSize.getWidth(), photoSize.getHeight());//设置SurfaceTexture缓冲区大小
                previewSurface = new Surface(surfaceTexture);

                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(previewSurface);
                previewRequest = previewRequestBuilder.build();

                cameraDevice.createCaptureSession(Arrays.asList(previewSurface, photoSurface), sessionsStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "相机已断开连接");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "相机打开出错");
        }
    };

    CameraCaptureSession.StateCallback sessionsStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (null == cameraDevice) {
                return;
            }

            captureSession = session;
            try {
                captureSession.setRepeatingRequest(previewRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "会话注册失败");
        }
    };

    CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            try {
                captureSession.setRepeatingRequest(previewRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }
    };

    ImageReader.OnImageAvailableListener photoReaderImgListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            writeImageToFile();
        }
    };
}
