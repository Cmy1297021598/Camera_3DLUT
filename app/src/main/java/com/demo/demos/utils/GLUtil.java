package com.demo.demos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES30.*;
import static android.opengl.GLUtils.texImage2D;

/**
 * Created by wangyt on 2019/5/9
 */
public class GLUtil {
    private static final String TAG = "opengl-demos";

    private static Context context;

    public static void init(Context ctx){
        context = ctx;
    }

    /*********************** 纹理 ************************/
    public static int loadTextureFromRes2D(FloatBuffer fbuf, int lut_size){

        Log.i("dm2022"," loadTextureFromRes2D:fbuf=" + fbuf.capacity() + " lut_size=" + lut_size);

        // 重新组织lut_table数据，构成一个狭长位图
        //ByteBuffer buffer = ByteBuffer.allocate(lut_size * lut_size * lut_size * 3 * 4); // sizeof(float)==4
        //buffer.order(ByteOrder.nativeOrder());
        FloatBuffer fBuffer = FloatBuffer.allocate(lut_size * lut_size * lut_size * 3);
        float bitmap[] = fBuffer.array();
        fbuf.position(0);
        float lut_table[] = fbuf.array();
        for (int y = 0; y < lut_size; y++)
        {
            for (int x = 0; x < lut_size * lut_size; x++)
            {
                int nx = x / lut_size;
                int vx = x % lut_size;
                //新二维图上(x,y)的点在原数组中的位置:(nx * lut_size + vx,y)
                int pz = nx * lut_size * lut_size + y * lut_size + vx;
                float r = lut_table[3 * pz + 0];
                float g = lut_table[3 * pz + 1];
                float b = lut_table[3 * pz + 2];

                int nd = y * lut_size * lut_size + x;
                bitmap[3 * nd + 0] = r;
                bitmap[3 * nd + 1] = g;
                bitmap[3 * nd + 2] = b;
            }
        }

        int[] textureId = new int[1];
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        glGenTextures(1, textureId, 0);
        Log.i("dm2022"," texture2D: texID=" + textureId[0]);
        if (textureId[0] == 0) {
            Log.e("dm2022", "创建纹理对象失败");
            return -1;
        }
        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, lut_size*lut_size, lut_size, 0, GL_RGB, GL_FLOAT, fBuffer);

        //fbuf.clear();
        // 解绑当前纹理，防止其他地方以外改变该纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        //返回纹理对象
        return textureId[0];

    }

    public static int loadTextureFromRes2D2(int lut_qsz[], FloatBuffer fbuf, int lut_size) {

        int qSZ = 2;
        while (qSZ * qSZ < lut_size) {
            qSZ += 1;
        }
        lut_qsz[0] = qSZ;
        Log.i("dm2022", " loadTextureFromRes2D2:fbuf=" + fbuf.capacity() + " lut_size=" + lut_size + " qSZ=" + qSZ);


        // 重新组织lut_table数据，构成一个方形位图
        FloatBuffer fBuffer = FloatBuffer.allocate((lut_size * qSZ) * (lut_size * qSZ) * 3);
        float bitmap[] = fBuffer.array();
        fbuf.position(0);
        float lut_table[] = fbuf.array();

        for (int qj = 0; qj < qSZ; qj++) {
            for (int qi = 0; qi < qSZ; qi++) {

                // 位置(qi, qj)上放置一个lut_size * lut_size的图
                int offx = lut_size * qi;
                int offy = lut_size * qj;
                int blu  = qj * qSZ + qi;
                // 总数不超过lut_size
                if (blu < lut_size)
                {
                    for (int y = 0; y < lut_size; y++) {
                        for (int x = 0; x < lut_size; x++) {

                            int red = x;
                            int gre = y;

                            //新二维图上(x,y)的点在原数组中的位置:(nx * lut_size + vx,y)
                            int pz  = blu * lut_size * lut_size + gre * lut_size + red;
                            float r = lut_table[3 * pz + 0];
                            float g = lut_table[3 * pz + 1];
                            float b = lut_table[3 * pz + 2];

                            int nd = (offy + y) * lut_size * qSZ + (offx + x);

                            bitmap[3 * nd + 0] = r;
                            bitmap[3 * nd + 1] = g;
                            bitmap[3 * nd + 2] = b;
                        }
                    }
                }
            }
        }

        int[] textureId = new int[1];
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        glGenTextures(1, textureId, 0);
        Log.i("dm2022"," texture2D: texID=" + textureId[0]);
        if (textureId[0] == 0) {
            Log.e("dm2022", "创建纹理对象失败");
            return -1;
        }
        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, lut_size * qSZ, lut_size * qSZ, 0, GL_RGB, GL_FLOAT, fBuffer);

        //fbuf.clear();
        // 解绑当前纹理，防止其他地方以外改变该纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        //返回纹理对象
        return textureId[0];

    }
    public static int loadTextureFromRes3D(FloatBuffer fbuf, int lut_size){

        Log.i("dm2022"," loadTextureFromRes3D:fbuf=" + fbuf.capacity() + " lut_size=" + lut_size);

        int[] textureId = new int[1];
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        glGenTextures(1, textureId, 0);
        Log.i("dm2022"," texture3D: texID=" + textureId[0]);
        if (textureId[0] == 0) {
            Log.e("dm2022", "创建纹理对象失败");
            return -1;
        }
        glBindTexture(GL_TEXTURE_3D, textureId[0]);
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // GL_LINEAR
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB, lut_size, lut_size, lut_size, 0, GL_RGB, GL_FLOAT, fbuf);

        //fbuf.clear();
        // 解绑当前纹理，防止其他地方以外改变该纹理
        glBindTexture(GL_TEXTURE_3D, 0);
        //返回纹理对象
        return textureId[0];

    }

    public static int loadTextureFromRes(Bitmap bitmap) {
        //创建纹理对象
        int[] textureId = new int[1];
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        glGenTextures(1, textureId, 0);
        if (textureId[0] == 0) {
            Log.e(TAG, "创建纹理对象失败");
        }
        //原尺寸加载位图资源（禁止缩放）
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false;
//        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
        if (bitmap == null) {
            //删除纹理对象
            glDeleteTextures(1, textureId, 0);
            Log.e(TAG, "加载位图失败");
        }
        //绑定纹理到opengl
        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // GL_NEAREST
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // GL_NEAREST
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        //释放bitmap资源（上面已经把bitmap的数据复制到纹理上了）
        bitmap.recycle();
        //解绑当前纹理，防止其他地方以外改变该纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        //返回纹理对象
        return textureId[0];
    }



    /*********************** 着色器、程序 ************************/
    public static String loadShaderSource(int resId){
        StringBuilder res = new StringBuilder();

        InputStream is = context.getResources().openRawResource(resId);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String nextLine;
        try {
            while ((nextLine = br.readLine()) != null) {
                res.append(nextLine);
                res.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res.toString();
    }

    /**
     * 加载着色器源，并编译
     *
     * @param type         顶点着色器（GL_VERTEX_SHADER）/片段着色器（GL_FRAGMENT_SHADER）
     * @param shaderSource 着色器源
     * @return 着色器
     */
    public static int loadShader(int type, String shaderSource){
        //创建着色器对象
        int shader = glCreateShader(type);
        if (shader == 0) return 0;//创建失败
        //加载着色器源
        glShaderSource(shader, shaderSource);
        //编译着色器
        glCompileShader(shader);
        //检查编译状态
        int[] compiled = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, glGetShaderInfoLog(shader));
            glDeleteShader(shader);
            return 0;//编译失败
        }

        return shader;
    }

    public static int createAndLinkProgram(int vertextShaderResId, int fragmentShaderResId){
        //获取顶点着色器
        int vertexShader = GLUtil.loadShader(GL_VERTEX_SHADER, loadShaderSource(vertextShaderResId));
        if (0 == vertexShader){
            Log.e(TAG, "failed to load vertexShader");
            return 0;
        }
        //获取片段着色器
        int fragmentShader = GLUtil.loadShader(GL_FRAGMENT_SHADER, loadShaderSource(fragmentShaderResId));
        if (0 == fragmentShader){
            Log.e(TAG, "failed to load fragmentShader");
            return 0;
        }
        int program = glCreateProgram();
        if (program == 0){
            Log.e(TAG, "failed to create program");
        }
        //绑定着色器到程序
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        //连接程序
        glLinkProgram(program);
        //检查连接状态
        int[] linked = new int[1];
        glGetProgramiv(program,GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0){
            glDeleteProgram(program);
            Log.e(TAG, "failed to link program");
            return 0;
        }
        return program;
    }
}

