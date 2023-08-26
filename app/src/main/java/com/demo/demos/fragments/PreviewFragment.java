package com.demo.demos.fragments;


import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreviewFragment extends BaseFragment {

    private static final long PREVIEW_SIZE_MIN = 720 * 480;

    Button btnChangePreviewSize;
    Button btnImageMode;
    Button btnVideoMode;
    //    TextureView previewView;//相机预览view
    AutoFitTextureView previewView;//自适应相机预览view

    CameraManager cameraManager;//相机管理类
    CameraDevice cameraDevice;//相机设备类
    CameraCaptureSession cameraCaptureSession;//相机会话类

    String cameraId;//相机id

    List<Size> outputSizes;//相机输出尺寸
    int sizeIndex = 0;

    Size previewSize;//预览尺寸

    ImageReader previewReader;

    public PreviewFragment() {
        // Required empty public constructor
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        //TextureView 可用时调用改回调方法
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //TextureView 可用，打开相机
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //初始化相机
        initCamera();
        //初始化界面
        initViews(view);
    }

    private void initCamera() {
        cameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getCameraId(false);//默认使用后置相机
        //获取指定相机的输出尺寸列表，降序排序
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        //初始化预览尺寸
        previewSize = outputSizes.get(0);
    }

    private void initViews(View view) {
        btnChangePreviewSize = view.findViewById(R.id.btn_change_preview_size);
        btnChangePreviewSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //切换预览分辨率
                updateCameraPreview();
                setButtonText();
                Log.e(TAG, "onClick: " + previewView.getWidth()+ ';' + previewView.getHeight() );

            }
        });
        setButtonText();

        btnImageMode = view.findViewById(R.id.btn_image_mode);
        btnImageMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //拍照模式，选择最大输出尺寸
                updateCameraPreviewWithImageMode();
            }
        });

        btnVideoMode = view.findViewById(R.id.btn_video_mode);
        btnVideoMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //录像模式，选择宽高比和预览窗口高宽比一致或最接近且的输出尺寸
                //如果该输出尺寸过小，则选择和预览窗口面积最接近的输出尺寸
                updateCameraPreviewWithVideoMode();
            }
        });

        previewView = view.findViewById(R.id.afttv_camera);
        previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());

        //设置 TextureView 的状态监听
        previewView.setSurfaceTextureListener(surfaceTextureListener);
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
        super.onPause();
        releaseCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            //打开相机
            cameraManager.openCamera(cameraId,
                    new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice camera) {
                            if (camera == null) {
                                return;
                            }
                            cameraDevice = camera;
                            //创建相机预览 session
                            createPreviewSession();
                        }

                        @Override
                        public void onDisconnected(CameraDevice camera) {
                            //释放相机资源
                            releaseCamera();
                        }

                        @Override
                        public void onError(CameraDevice camera, int error) {
                            //释放相机资源
                            releaseCamera();
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreviewSession() {
        //关闭之前的会话
        CameraUtils.getInstance().releaseImageReader(previewReader);
        CameraUtils.getInstance().releaseCameraSession(cameraCaptureSession);
        //根据TextureView 和 选定的 previewSize 创建用于显示预览数据的Surface
        SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());//设置SurfaceTexture缓冲区大小
        final Surface previewSurface = new Surface(surfaceTexture);
        //获取 ImageReader 和 surface
        previewReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 2);
        previewReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] data = new byte[buffer.remaining()];
                            Log.d(TAG, "data-size=" + data.length);
                            buffer.get(data);
                            image.close();
                        }
                    }
                },
                null);
        final Surface readerSurface = previewReader.getSurface();

        try {
            //创建预览session
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {

                            cameraCaptureSession = session;

                            try {
                                //构建预览捕获请求
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                builder.addTarget(previewSurface);//设置 previewSurface 作为预览数据的显示界面
                                builder.addTarget(readerSurface);
                                CaptureRequest captureRequest = builder.build();
                                //设置重复请求，以获取连续预览数据
                                session.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                                            @Override
                                            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                                                super.onCaptureProgressed(session, request, partialResult);
                                            }

                                            @Override
                                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                                super.onCaptureCompleted(session, request, result);
                                            }
                                        },
                                        null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void updateCameraPreview() {
        if (sizeIndex + 1 < outputSizes.size()) {
            sizeIndex++;
        } else {
            sizeIndex = 0;
        }
        previewSize = outputSizes.get(sizeIndex);
        previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
        //重新创建会话
        createPreviewSession();
    }

    private void updateCameraPreviewWithImageMode() {
        previewSize = outputSizes.get(0);
        previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
        createPreviewSession();
    }

    private void updateCameraPreviewWithVideoMode() {
        List<Size> sizes = new ArrayList<>();
        //计算预览窗口高宽比，高宽比，高宽比
        float ratio = ((float) previewView.getHeight() / previewView.getWidth());
        //首先选取宽高比与预览窗口高宽比一致且最大的输出尺寸
        for (int i = 0; i < outputSizes.size(); i++) {
            if (((float) outputSizes.get(i).getWidth()) / outputSizes.get(i).getHeight() == ratio) {
                sizes.add(outputSizes.get(i));
            }
        }
        if (sizes.size() > 0) {
            previewSize = Collections.max(sizes, new CameraUtils.CompareSizesByArea());
            previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
            createPreviewSession();
            return;
        }
        //如果不存在宽高比与预览窗口高宽比一致的输出尺寸，则选择与其高宽比最接近的输出尺寸
        sizes.clear();
        float detRatioMin = Float.MAX_VALUE;
        for (int i = 0; i < outputSizes.size(); i++) {
            Size size = outputSizes.get(i);
            float curRatio = ((float) size.getWidth()) / size.getHeight();
            if (Math.abs(curRatio - ratio) < detRatioMin) {
                detRatioMin = curRatio;
                previewSize = size;
            }
        }
        if (previewSize.getWidth() * previewSize.getHeight() > PREVIEW_SIZE_MIN) {
            previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
            createPreviewSession();
        }
        //如果宽高比最接近的输出尺寸太小，则选择与预览窗口面积最接近的输出尺寸
        long area = previewView.getWidth() * previewView.getHeight();
        long detAreaMin = Long.MAX_VALUE;
        for (int i = 0; i < outputSizes.size(); i++) {
            Size size = outputSizes.get(i);
            long curArea = size.getWidth() * size.getHeight();
            if (Math.abs(curArea - area) < detAreaMin) {
                detAreaMin = curArea;
                previewSize = size;
            }
        }
        previewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
        createPreviewSession();
    }

    private void releaseCamera() {
        CameraUtils.getInstance().releaseImageReader(previewReader);
        CameraUtils.getInstance().releaseCameraSession(cameraCaptureSession);
        CameraUtils.getInstance().releaseCameraDevice(cameraDevice);
    }

    private void setButtonText() {
        btnChangePreviewSize.setText(previewSize.getWidth() + "-" + previewSize.getHeight());
    }
}
