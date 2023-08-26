package com.demo.demos;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.demo.demos.base.BaseActivity;
import com.demo.demos.fragments.GLPreviewFragment;
import com.demo.demos.fragments.PhotoFragment;
import com.demo.demos.fragments.PreviewFragment;
import com.demo.demos.utils.CameraUtils;
import com.demo.demos.utils.GLUtil;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    Button btnPreview, btnCapture, btnGLPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CameraUtils.init(this);
        GLUtil.init(this);

        btnPreview = findViewById(R.id.btnPreview);
        btnPreview.setOnClickListener(this);

        btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(this);

        btnGLPreview = findViewById(R.id.btnGLPreview);
        btnGLPreview.setOnClickListener(this);

        requestPermission("请给予相机、存储权限，以便app正常工作", null,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPreview:
                transactFragment(new PreviewFragment());
                break;
            case R.id.btnCapture:
                transactFragment(new PhotoFragment());
                break;
            case R.id.btnGLPreview:
                transactFragment(new GLPreviewFragment());
                break;
        }
    }

    private void transactFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_camera, fragment)
                .commit();
    }
}
