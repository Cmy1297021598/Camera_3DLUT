package com.demo.demos.filter;

import com.demo.demos.R;
import com.demo.demos.utils.CommonUtil;
import com.demo.demos.utils.GLUtil;

import java.nio.FloatBuffer;

import static android.opengl.GLES30.*;

/**
 * Created by wangyt on 2019/5/24
 */
public class BaseFilter {
    public static final String VERTEX_ATTRIB_POSITION = "a_Position";
    public static final int VERTEX_ATTRIB_POSITION_SIZE = 3;
    public static final String VERTEX_ATTRIB_TEXTURE_POSITION = "a_texCoord";
    public static final int VERTEX_ATTRIB_TEXTURE_POSITION_SIZE = 2;
    public static final String UNIFORM_TEXTURE = "s_texture";
    public static final String UNIFORM_MATRIX = "u_matrix";

    public static final float[] vertex ={
            -1f,1f,0.0f,//左上
            -1f,-1f,0.0f,//左下
            1f,-1f,0.0f,//右下
            1f,1f,0.0f//右上
    };

    public static final float[] textureCoord = {
            0.0f,1.0f,
            0.0f,0.0f,
            1.0f,0.0f,
            1.0f,1.0f
    };

    public float[] matrix = {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
    };

    public FloatBuffer vertexBuffer;
    public FloatBuffer textureCoordBuffer;

    public int[] textureId;
    public int program;
    public int hVertex, hMatrix, hTextureCoord, hTexture;

    public int width, height;

    public float[] getMatrix() {
        return matrix;
    }

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    public BaseFilter() {
        initBuffer();
    }

    public void initBuffer(){
        vertexBuffer = CommonUtil.getFloatBuffer(vertex);
        textureCoordBuffer = CommonUtil.getFloatBuffer(textureCoord);
    }

    public int[] getTextureId() {
        return textureId;
    }

    public void setTextureId(int[] textureId) {
        this.textureId = textureId;
    }

    public int[] getOutputTextureId(){
        return null;
    }

    public void onSurfaceCreated(){
        program = initProgram();
        initAttribLocations();
    }

    public void onSurfaceChanged(int width, int height){
        this.width = width;
        this.height = height;
    }

    public void onDraw(){
        setViewPort();
        useProgram();
        setExtend();
        bindTexture();
        enableVertexAttribs();
        clear();
        draw();
        disableVertexAttribs();
    }

    public int initProgram(){
        return GLUtil.createAndLinkProgram(R.raw.texture_vertex_shader, R.raw.texture_fragtment_shader);
    }

    public void initAttribLocations(){
        hVertex = glGetAttribLocation(program, VERTEX_ATTRIB_POSITION);
        hMatrix = glGetUniformLocation(program, UNIFORM_MATRIX);
        hTextureCoord = glGetAttribLocation(program, VERTEX_ATTRIB_TEXTURE_POSITION);
        hTexture = glGetUniformLocation(program, UNIFORM_TEXTURE);
    }

    public void setViewPort(){
        glViewport(0,0,width,height);
    }

    public void clear(){
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void useProgram(){
        glUseProgram(program);
    }

    public void setExtend(){
        glUniformMatrix4fv(hMatrix, 1, false, getMatrix(),0);
    }

    public void bindTexture(){
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, getTextureId()[0]);
        glUniform1i(hTexture, 0);
    }

    public void enableVertexAttribs(){
        glEnableVertexAttribArray(hVertex);
        glEnableVertexAttribArray(hTextureCoord);
        glVertexAttribPointer(hVertex,
                VERTEX_ATTRIB_POSITION_SIZE,
                GL_FLOAT,
                false,
                0,
                vertexBuffer);

        glVertexAttribPointer(hTextureCoord,
                VERTEX_ATTRIB_TEXTURE_POSITION_SIZE,
                GL_FLOAT,
                false,
                0,
                textureCoordBuffer);
    }

    public void draw(){
        glDrawArrays(GL_TRIANGLE_FAN,0,vertex.length / 3);
    }

    public void disableVertexAttribs(){
        glDisableVertexAttribArray(hVertex);
        glDisableVertexAttribArray(hTextureCoord);
    }
}
