package com.example.vasyl.imageloader;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ImageLoaderActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return ImageLoaderFragment.newInstance();
    }


}
