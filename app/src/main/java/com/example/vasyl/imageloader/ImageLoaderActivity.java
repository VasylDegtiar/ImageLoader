package com.example.vasyl.imageloader;

import android.support.v4.app.Fragment;


public class ImageLoaderActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return ImageLoaderFragment.newInstance();
    }


}
