package com.example.vasyl.imageloader;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;


import com.example.vasyl.imageloader.flickr.FlickrHelper;
import com.example.vasyl.imageloader.flickr.QueryPreferences;
import com.example.vasyl.imageloader.flickr.ThumbnailDownloader;
import com.example.vasyl.imageloader.model.ImageItem;

import java.util.ArrayList;
import java.util.List;

// use fragment from v4.app.Fragment
public class ImageLoaderFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<ImageItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static ImageLoaderFragment newInstance() {
        return new ImageLoaderFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //control fragment
        setRetainInstance(true);
        //use menu in fragment
        setHasOptionsMenu(true);
        updateItems();

        Handler responseHandler =new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder,
                                                      Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //need kill stream
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_item, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    @Nullable
    @Override
       public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        //custom RecyclerView
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setupAdapter();
        return v;
    }



    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView ;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<ImageItem> mImageItems;

        public PhotoAdapter(List<ImageItem> imageItems) {
            mImageItems = imageItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            ImageItem imageItem = mImageItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_collections_black_24dp);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, imageItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mImageItems.size();
        }
    }
    private void setupAdapter() {
        //  Return true if the fragment is currently added to its activity
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    //AsyncTask
    // List<ImageItem> - type of result
    private class FetchItemsTask extends AsyncTask<Void,Void,List<ImageItem>>{
        private String mQuery;
        public  FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<ImageItem> doInBackground(Void... voids) {


            if (mQuery==null){
                return new FlickrHelper().fetchRecentPhotos();
            }else {
                return new FlickrHelper().searchPhotos(mQuery);
            }
        }

        //work in main stream
        @Override
        protected void onPostExecute(List<ImageItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
}