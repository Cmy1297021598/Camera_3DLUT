# android-camera2相机开发【7】-使用opengles进行相机预览

前面几篇文章梳理了 android 相机的基本使用流程及相关的 api，完成了预览拍照等功能。

就预览而言，后续想做实时滤镜等功能的话，如果按照之前的方法用 ImageReader 拿到数据之后做处理再显示，一是繁琐，而是效率太低，卡顿严重。所以需要使用 opengles 对相机的预览数据进行渲染，可以很大的提高效率，防止卡顿。

这篇文章先不做滤镜，而是先实现 opengles 渲染相机数据的功能，为后续实时滤镜等功能打基础。

### opengles 基本使用

opengles 在 android 平台上的简单使用可以参考之前写的 opengles 相关的文章。

主要流程：

1. 布局中使用 GLSurfacView 作为预览窗口。
2. 准备相关的顶点属性数据和着色器文件。
2. 实现 GLSurfaceView.Render 接口，编写具体的渲染代码。 

具体的操作在之前 opengles 相关的文章中都有，完整的代码放在 [android-opengl-demo项目](https://github.com/WangYantao/android-opengles-demos)

### opengles 相机预览

android 相机的预览数据可以输出到 SurfaceTexture 上，所以用 opengles 做相机预览的主要思路是

1. 在 GLSurfaceView.Render 中创建一个纹理，再使用该纹理创建一个 SurfaceTexture
2. 使用该 SurfaceTexture 创建一个 Surface 传给相机，相机预览数据就输出到一个纹理上了
3. 使用 GLSurfaceView.Render 将该纹理渲染到 GLSurfaceView 窗口上
4. 使用 SurfaceTexture 的 setOnFrameAvailableListener 方法 给 SurfaceTexture  添加一个数据帧数据可用的监听器，在监听器中 调用 GLSurfaceView 的 requestRender 方法渲染该帧数据，这样相机每次输出一帧数据就可以渲染一次，在GLSurfaceView窗口中就可以看到相机的预览数据了

**准备着色器**

顶点着色器

```
#version 300 es

layout (location = 0) in vec4 a_Position;
layout (location = 1) in vec2 a_texCoord;

out vec2 v_texCoord;

void main()
{
    gl_Position = a_Position;
    v_texCoord = a_texCoord;
}
``` 

片段着色器，需要注意的是，做相机预览的话，纹理的类型需要使用 samplerExternalOES ，而不是之前渲染图片的 sampler2D。

```
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 v_texCoord;
out vec4 outColor;
uniform samplerExternalOES s_texture;

void main(){
    outColor = texture(s_texture, v_texCoord);
}
```

**实现Render**

流程和之前文章里基本一样，需要注意的是需要使用纹理创建一个 SurfaceTexture，并提供 SurfaceTexture 实例的获取方法，以便后续相机获取使用。

另外注意渲染的时候先调用 SurfaceTexture 的 updateTexImage 方法，更新纹理。

```java
public class GLRender implements GLSurfaceView.Renderer{
    private static final String VERTEX_ATTRIB_POSITION = "a_Position";
    private static final int VERTEX_ATTRIB_POSITION_SIZE = 3;
    private static final String VERTEX_ATTRIB_TEXTURE_POSITION = "a_texCoord";
    private static final int VERTEX_ATTRIB_TEXTURE_POSITION_SIZE = 2;
    private static final String UNIFORM_TEXTURE = "s_texture";

    private  float[] vertex ={
            -1f,1f,0.0f,//左上
            -1f,-1f,0.0f,//左下
            1f,-1f,0.0f,//右下
            1f,1f,0.0f//右上
    };

    //纹理坐标，（s,t），t坐标方向和顶点y坐标反着
    public float[] textureCoord = {
            0.0f,1.0f,
            1.0f,1.0f,
            1.0f,0.0f,
            0.0f,0.0f
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureCoordBuffer;

    private int program;

    //接收相机数据的纹理
    private int[] textureId = new int[1];
    //接收相机数据的 SurfaceTexture
    public SurfaceTexture surfaceTexture;

    public GLRender() {
        //初始化顶点数据
        initVertexAttrib();
    }

    private void initVertexAttrib() {
        textureCoordBuffer = GLUtil.getFloatBuffer(textureCoord);
        vertexBuffer = GLUtil.getFloatBuffer(vertex);
    }

    //向外提供 surfaceTexture 实例
    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //创建纹理对象
        glGenTextures(textureId.length, textureId, 0);
        //将纹理对象绑定到srufaceTexture
        surfaceTexture = new SurfaceTexture(textureId[0]);
        //创建并连接程序
        program = GLUtil.createAndLinkProgram(R.raw.texture_vertex_shader, R.raw.texture_fragtment_shader);
        //设置清除渲染时的颜色
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0,0,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //srufaceTexture 获取新的纹理数据
        surfaceTexture.updateTexImage();
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(program);

        int vertexLoc = glGetAttribLocation(program, VERTEX_ATTRIB_POSITION);
        int textureLoc = glGetAttribLocation(program, VERTEX_ATTRIB_TEXTURE_POSITION);

        glEnableVertexAttribArray(vertexLoc);
        glEnableVertexAttribArray(textureLoc);

        glVertexAttribPointer(vertexLoc,
                VERTEX_ATTRIB_POSITION_SIZE,
                GL_FLOAT,
                false,
                0,
                vertexBuffer);

        glVertexAttribPointer(textureLoc,
                VERTEX_ATTRIB_TEXTURE_POSITION_SIZE,
                GL_FLOAT,
                false,
                0,
                textureCoordBuffer);

        //绑定0号纹理单元纹理
        glActiveTexture(GL_TEXTURE0);
        //将纹理放到当前单元的 GL_TEXTURE_BINDING_EXTERNAL_OES 目标对象中
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        //设置纹理过滤参数
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        //将片段着色器的纹理属性值（s_texture）设置为 0 号单元
        int uTextureLoc = glGetUniformLocation(program, UNIFORM_TEXTURE);
        glUniform1i(uTextureLoc,0);

        glDrawArrays(GL_TRIANGLE_FAN,0,vertex.length / 3);

        glDisableVertexAttribArray(vertexLoc);
        glDisableVertexAttribArray(textureLoc);
    }
}
```

**相机使用前面创建的SurfaceTexture实例**

通过 SurfaceTexture 实例创建一个 Surface ，传给相机即可。

```java
            //获取Render中的 SurfaceTexture 实例
            surfaceTexture = glRender.getSurfaceTexture();
            if (surfaceTexture == null) {
                return;
            }
            surfaceTexture.setDefaultBufferSize(photoSize.getWidth(), photoSize.getHeight());
            //添加帧可用监听，让GLSurfaceView 进行渲染
            surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                    glSurfaceView.requestRender();
                }
            });
            //通过 SurfaceTexture 实例创建一个 Surface
            surface = new Surface(surfaceTexture);

            try {
                cameraDevice = camera;
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //将 surface 传给相机
                previewRequestBuilder.addTarget(surface);
                previewRequest = previewRequestBuilder.build();

                cameraDevice.createCaptureSession(Arrays.asList(surface), sessionsStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
```

### 总结

本文梳理了使用 opengles 将相机预览数据渲染到 GLSurfaceView 的基本流程，后续以此为基础，结合 opengles 的离屏渲染机制实现实时滤镜功能。 


[项目github地址](https://github.com/WangYantao/android-camera-demos)