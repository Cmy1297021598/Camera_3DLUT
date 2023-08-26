# android-camera2相机开发【10】-opengl离屏渲染、拍照、前后相机切换

**离屏渲染**

之前已经将相机的预览数据已经输出到opengl的纹理上，渲染的时候，opengl直接将纹理渲染到了屏幕。

但是，如果想要对该纹理进一步处理，就不能直接渲染到屏幕，而是应该先渲染到屏幕外的缓冲区（FrameBuffer）处理完后再渲染到屏幕。渲染到缓冲区的操作就是离屏渲染。

离屏渲染的目的是更改渲染目标（屏幕->缓冲区），主要步骤如下：

1. 准备离屏渲染所需要的 FrameBuffer 和 纹理对象 
2. 切换渲染目标（屏幕->缓冲区）
3. 执行渲染（和之前一样，执行onDrawFrame方法进行绘制）
4. 重置渲染目标（缓冲区->屏幕） 

关键代码如下：

```java
    //准备离屏渲染所需要的 FrameBuffer 和 纹理对象 
    public void genFrameBufferAndTexture() {
        glGenFramebuffers(frameBuffer.length, frameBuffer, 0);

        glGenTextures(frameTexture.length, frameTexture, 0);
    }
    //切换渲染目标（屏幕->缓冲区）
    public void bindFrameBufferAndTexture() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameTexture[0], 0);
    }
    //重置渲染目标（缓冲区->屏幕）
    public void unBindFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
```

**拍照**

无论中间经过离屏渲染做了多少处理，最后都会切换渲染到屏幕，不渲染到屏幕看不到，也就没法预览。

拍照是再预览的基础上，再点击拍照的一瞬间将最后一个渲染环节切换到FrameBuffer，然后通过 `glReadPixels` 方法将显存中的数据回传到内存中保存到本地，最后再将渲染切换回屏幕继续预览。

需要注意的是在屏幕预览时y轴正方向是向下的，所以保存到本地的时候需要在y轴上做一次反转。

关键代码如下：

```java
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
```

**前后相机切换**

这个比较简单，需要注意的就是前置/后置摄像头默认的旋转角度不同预览的时候需要进行旋转校正。

```java
    //后置相机，顺时针旋转90度
    public static final float[] textureCoordCameraBack = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    //前置相机，逆时针旋转90度
    public static final float[] textureCoordCameraFront = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };
```

另外一个需要注意的就是前置相机的镜像问题，通过矩阵进行设置

```java
        if (this.useFront != useFront) {
            this.useFront = useFront;
            cameraFilter.setUseFront(useFront);
            matrix = MatrixUtil.flip(matrix, true, false);
        }
```

[项目github地址](https://github.com/WangYantao/android-camera-demos)