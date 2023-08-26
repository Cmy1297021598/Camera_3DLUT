# android-camera2相机开发【2】-动态切换相机预览尺寸

上一篇文章中介绍了相机预览的基本实现，只固定的显示了一个输出尺寸， android 设备相机实际可以支持输出很多尺寸的图像，不同的设备具体尺寸数值不同，通过系统提供的方法可以获取设备支持的输出尺寸，具体的操作可以参考上一篇文章中的相关内容。

本文在上一篇文章的基础上进行调整，实现的效果是，添加一个按钮，点击的时候切换相机输出尺寸，更新预览界面。

### 1. 简单的工具类

将一些简单的相机的操作封装程一个工具类，能获取指定的相机，相机输出尺寸列表，释放相机资源，具体代码如下。

```java
public class CameraUtils {
    private static CameraUtils ourInstance = new CameraUtils();

    private static Context appContext;
    private static CameraManager cameraManager;

    public static void init(Context context){
        if (appContext == null) {
            appContext = context.getApplicationContext();
            cameraManager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
        }
    }

    public static CameraUtils getInstance() {
        return ourInstance;
    }

    private CameraUtils() {

    }

    public CameraManager getCameraManager(){
        return cameraManager;
    }

    public String getFrontCameraId(){
        return getCameraId(true);
    }

    public String getBackCameraId(){
        return getCameraId(false);
    }

    /**
     * 获取相机id
     * @param useFront 是否使用前置相机
     * @return
     */
    public String getCameraId(boolean useFront){
        try {
            for (String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (useFront){
                    if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT){
                        return cameraId;
                    }
                }else {
                    if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK){
                        return cameraId;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 根据输出类获取指定相机的输出尺寸列表
     * @param cameraId 相机id
     * @param clz 输出类
     * @return
     */
    public List<Size> getCameraOutputSizes(String cameraId, Class clz){
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(configs.getOutputSizes(clz));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 根据输出格式获取指定相机的输出尺寸列表
     * @param cameraId 相机id
     * @param format 输出格式
     * @return
     */
    public List<Size> getCameraOutputSizes(String cameraId, int format){
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(configs.getOutputSizes(format));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 释放相机资源
     * @param cameraDevice
     */
    public void releaseCamera(CameraDevice cameraDevice){
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
```

### 2. 初始化相机

使用工具类，初始化一些相机相关的参数，后面打开相机的时候直接使用即可，省去了上一篇文章 `configCamera()` 配置相机的步骤。

```java
    private void initCamera(){
        cameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getCameraId(false);//默认使用后置相机
        //获取指定相机的输出尺寸列表
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        //初始化预览尺寸
        previewSize = outputSizes.get(0);
    }
```

### 3. 点击按钮切换输出尺寸，更新预览界面

点击按钮时，在相机输出尺寸列表中进行循环，依次使用选定的尺寸构造预览界面。

```java
        btnChangePreviewSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //切换预览分辨率
                updateCameraPreview();
                setButtonText();
            }
        });

    private void updateCameraPreview(){
        if (sizeIndex + 1 < outputSizes.size()){
            sizeIndex++;
        }else {
            sizeIndex = 0;
        }
        previewSize = outputSizes.get(sizeIndex);
        //重新创建会话
        createPreviewSession();
    }

    private void setButtonText(){
        btnChangePreviewSize.setText(previewSize.getWidth() + "-" + previewSize.getHeight());
    }
```

### 总结

经过上面的步骤，点击按钮时就可以预览不同输出尺寸的图像。

可以发现，不同的预览尺寸由于和预览窗口宽高比不同，造成不同程度的图像拉伸，需要进一步优化，后续文章进行实现。

