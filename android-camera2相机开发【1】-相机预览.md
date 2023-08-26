# anroid-camera2相机开发【1】-相机预览

### 1. 配置权限

* AndroidManifext.xml文件：`<uses-permission android:name="android.permission.CAMERA" />`
* android6.0以后的动态权限：网上很多教程，这里不再赘述。

### 2. 布局文件

很简单的布局，只有一个 `TextureView` 。

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".fragments.PreviewFragment">

    <TextureView
        android:id="@+id/ttv_camera"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

</RelativeLayout>
```

### 3. 实现预览

在具体实现编码之前，先简单介绍下相关的几个重要的类。

* CameraManager：相机管理类，可以用来遍历相机列表，获取相机配置类，打开相机等。
* CameraCharacteristics：相机配置类，用来获取相机的朝向/输出尺寸/格式等具体配置。
* CameraDevice：相机设备类，有很多参数（预览/拍照等等），主要用来创建相机会话（session）和请求（request）。
* CameraCaptureSession：相机捕获会话类，用于预览和拍照等。
* CaptureRequest：相机捕获请求类，用来定义输出缓冲区和显示界面。

**3.1 初始化界面和 CameraManager**

在 fragment 的 `onViewCreated` 声明周期方法中进行。

```java
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        previewView = view.findViewById(R.id.ttv_camera);
        //初始化 CameraManager
        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    }
```

**3.2 设置预览TextureView状态监听**

在 fragment 的 `onResume` 声明周期方法中对 TextureView 的状态进行监听，当其可用是会调用监听器的 `onSurfaceTextureAvailable` 方法，即可在该方法中启动相机。 

```java
    @Override
    public void onResume() {
        super.onResume();

        //设置 TextureView 的状态监听
        previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            //TextureView 可用时调用改回调方法
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //TextureView 可用，启动相机
                setupCamera();
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
        });
    }
```

**3.3 启动相机**

启动相机可以分为两个步骤： 配置相机相关参数，打开相机。

```java
    private void setupCamera() {
        //配置相机参数（cameraId，previewSize）
        configCamera();
        //打开相机
        openCamera();
    }
```

**配置相机相关参数（待打开的相机id，相机输出尺寸）。**

1. 获取相机列表，选择要打开的相机id。
2. 获取选择相机的输出尺寸，配合预览窗口的尺寸，选择最合适的预览尺寸。

```java
    private void configCamera() {
        try {
            //遍历相机列表，使用前置相机
            for (String cid : cameraManager.getCameraIdList()) {
                //获取相机配置
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cid);
                //使用后置相机
                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);//获取相机朝向
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //获取相机输出格式/尺寸参数
                StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //打印相关参数（相机id，相机输出尺寸、设备屏幕尺寸、previewView尺寸）
                printSizes(cid, configs);
                //设定最佳预览尺寸
                previewSize = setOptimalPreviewSize(configs.getOutputSizes(SurfaceTexture.class),
                        previewView.getMeasuredWidth(),
                        previewView.getMeasuredHeight());
                //打印最佳预览尺寸
                Log.d(TAG, "最佳预览尺寸（w-h）：" + previewSize.getWidth() + "-" + previewSize.getHeight());

                cameraId = cid;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size setOptimalPreviewSize(Size[] sizes, int previewViewWidth, int previewViewHeight) {
        List<Size> bigEnoughSizes = new ArrayList<>();
        List<Size> notBigEnoughSizes = new ArrayList<>();

        for (Size size : sizes) {
            if (size.getWidth() >= previewViewWidth && size.getHeight() >= previewViewHeight) {
                bigEnoughSizes.add(size);
            } else {
                notBigEnoughSizes.add(size);
            }
        }

        if (bigEnoughSizes.size() > 0) {
            return Collections.min(bigEnoughSizes, new CompareSizesByArea());
        } else if (notBigEnoughSizes.size() > 0) {
            return Collections.max(notBigEnoughSizes, new CompareSizesByArea());
        } else {
            Log.d(TAG, "未找到合适的预览尺寸");
            return sizes[0];
        }
    }
```

**打开相机**

1. 调用 `CameraManager.openCamera()` 方法打开相机。
2. 第二个参数是相机设备状态监听，相机打开时会调用 `onOpened()` 回调方法。
3. 在 `onOpened()` 回调方法中创建预览会话。（很重要）
    1. 根据TextureView 和 选定的 previewSize 创建用于显示预览数据的Surface
    2. 调用 `CameraDevice.createCaptureSession()` 方法创建捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行。
    3. 创建预览捕获请求，并设置会话进行重复请求，以获取连续的预览数据。
4. 相机打开异常和断开连接后，需要释放相机资源。

```java
    private void openCamera() {
        try {
            //打开相机
            cameraManager.openCamera(cameraId,
                    new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice camera) {
                            cameraDevice = camera;
                            //创建相机预览 session
                            createPreviewSession();
                        }

                        @Override
                        public void onDisconnected(CameraDevice camera) {
                            //释放相机资源
                            releseCamera();
                        }

                        @Override
                        public void onError(CameraDevice camera, int error) {
                            //释放相机资源
                            releseCamera();
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
     private void createPreviewSession() {
         //根据TextureView 和 选定的 previewSize 创建用于显示预览数据的Surface
         SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
         surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());//设置SurfaceTexture缓冲区大小
         final Surface previewSurface = new Surface(surfaceTexture);
 
         try {
             //创建预览session
             cameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                     new CameraCaptureSession.StateCallback() {
                         @Override
                         public void onConfigured(CameraCaptureSession session) {
                             try {
                                 //构建预览捕获请求
                                 CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                 builder.addTarget(previewSurface);//设置 previewSurface 作为预览数据的显示界面
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
    
    private void releseCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
```

### 4. 效果图

从下面效果图可以看到，相机预览的画面有变形，需要进一步处理。

相机竖向的效果图

![相机竖向的效果图](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E7%9B%B8%E6%9C%BA%E9%A2%84%E8%A7%88/002.png)

相机横向的效果图

![相机横向的效果图](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E7%9B%B8%E6%9C%BA%E9%A2%84%E8%A7%88/001.png)

### 5. 总结

本文实现了最基础的相机预览功能，重点放在梳理相机预览的关键步骤和相关类的使用。

针对画面拉伸变形的纠正以及横竖向布局切换的问题，后续文章进行实现。

