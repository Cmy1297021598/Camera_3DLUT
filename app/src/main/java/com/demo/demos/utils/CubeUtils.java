package com.demo.demos.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.demo.demos.filter.ColorFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CubeUtils {
    private static String TAG = "CubeUtils";

    // read 3d lut data as array: float[r][g][b][3]
    public static FloatBuffer encode2(int[] lut_size, String arg) {

        try {
            int lutSize = 0;
            BufferedReader br = new BufferedReader(new FileReader(new File(arg)));
            String line;
            String[] parts;
            float[] data = null;
            int index = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.isEmpty()) {
                    continue;
                }
                parts = line.split(" ");
                if (parts.length == 2) {
                    if (parts[0].equalsIgnoreCase("LUT_3D_SIZE")) {
                        lutSize = Integer.parseInt(parts[1]);
                        ColorFilter.CUBE_SIZE = lutSize;
                        if (data == null) {
                            data = new float[lutSize * lutSize * lutSize * 3];
                            continue;
                        } else {
                            System.out.println("corrupted data");
                            System.exit(0);
                        }
                    }
                }

                if (parts.length != 3) {
                    System.out.println("unknown data: " + line);
                    continue;
                }
                if (data == null) {
                    System.out.println("not ready yet");
                    continue;
                }

                float pRed = Float.parseFloat(parts[0]);
                float pGre = Float.parseFloat(parts[1]);
                float pBlu = Float.parseFloat(parts[2]);
                data[index++] = pRed;
                data[index++] = pGre;
                data[index++] = pBlu;
            }

            System.out.println("dimension lutSize=" + lutSize);

            //ByteBuffer mbb = ByteBuffer.allocate(data.length * 4); // sizeof(float)==4
            //mbb.order(ByteOrder.nativeOrder());
            //FloatBuffer fbuf = mbb.asFloatBuffer();
            FloatBuffer fbuf = FloatBuffer.allocate(data.length);
            fbuf.put(data);
            fbuf.position(0);

            lut_size[0] = lutSize;

            return fbuf;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap encode(String arg) {
        try {
            int lutSize = 0;
            BufferedReader br = new BufferedReader(new FileReader(new File(arg)));
            String line;
            String[] parts;
            float[][] data = null;
            int index = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.isEmpty()) {
                    continue;
                }
                parts = line.split(" ");
                if (parts.length == 2) {
                    if (parts[0].equalsIgnoreCase("LUT_3D_SIZE")) {
                        lutSize = Integer.parseInt(parts[1]);
                        ColorFilter.CUBE_SIZE = lutSize;
                        if (data == null) {
                            data = new float[lutSize * lutSize * lutSize][];
                            continue;
                        } else {
                            System.out.println("corrupted data");
                            System.exit(0);
                        }
                    }
                }

                if (parts.length != 3) {
                    System.out.println("unknown data: " + line);
                    continue;
                }
                if (data == null) {
                    System.out.println("not ready yet");
                    continue;
                }

                float[] pixel = new float[3];
                pixel[0] = Float.parseFloat(parts[0]);
                pixel[1] = Float.parseFloat(parts[1]);
                pixel[2] = Float.parseFloat(parts[2]);
                data[index++] = pixel;
            }

            int dim = lutSize * (int) Math.sqrt(lutSize);
            index = 0;
            System.out.println("dimension dim=" + dim + "x" + dim + " lutSize=" + lutSize);

            Bitmap bitmap = Bitmap.createBitmap(dim,dim, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            for (int x = 0; x < dim / lutSize; x++) {
                for (int y = 0; y < dim / lutSize; y++) {
                    for (int w = 0; w < lutSize; w++) {
                        for (int h = 0; h < lutSize; h++) {
                            int width = y * lutSize + h;
                            int height = x * lutSize + w;

                            float[] raw = data[index++];

                            paint.setColor(Color.rgb(raw[0], raw[1], raw[2]));
                            canvas.drawLine(width, height, width+1, height+1,paint);
                            //canvas.drawPoint(width, height,paint);
                        }
                    }
                }
            }
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
