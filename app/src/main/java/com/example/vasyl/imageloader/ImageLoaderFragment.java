package com.example.vasyl.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.Toast;


import com.example.vasyl.imageloader.flickr.FlickrHelper;
import com.example.vasyl.imageloader.flickr.ImageLruCache;
import com.example.vasyl.imageloader.flickr.QueryPreferences;
import com.example.vasyl.imageloader.flickr.ThumbnailDownloader;
import com.example.vasyl.imageloader.flickr.ThumbnailPreloader;
import com.example.vasyl.imageloader.model.ImageItem;

import java.util.ArrayList;
import java.util.List;

// use fragment from v4.app.Fragment
public class ImageLoaderFragment extends Fragment {

    private static final String TAG = "ImageLoaderFragment";

    private RecyclerView mPhotoRecyclerView;
    private ProgressBar mProgressBar;
    private List<ImageItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<ImageHolder> mThumbnailDownloader;
    private ThumbnailPreloader<Integer> mThumbnailPreloader;

    private PhotoAdapter mAdapter;

    private int mPageNumber = 1;
    private int mNumColumns = 2;

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
        mThumbnailPreloader = new ThumbnailPreloader<>(responseHandler);
        // Place image in cache and set photoHolder image
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<ImageHolder>() {
            @Override
            public void onThumbnailDownloaded(ImageHolder imageHolder, Bitmap thumbnail, String url) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                ImageLruCache imageLruCache = ImageLruCache.get(getContext());
                imageLruCache.addBitmapToMemoryCache(url, thumbnail);
                imageHolder.bindDrawable(drawable);
            }
        });

        // Store preloaded image in cache
        mThumbnailPreloader.setThumbnailDownloadListener(new ThumbnailPreloader.ThumbnailDownloadListener<Integer>() {
            @Override
            public void onThumbnailDownloaded(Integer target, Bitmap thumbnail, String url) {
                // Only store in cache since we are preloading
                ImageLruCache imageLruCache = ImageLruCache.get(getContext());
                imageLruCache.addBitmapToMemoryCache(url, thumbnail);
            }
        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");

        mThumbnailPreloader.setPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
        mThumbnailPreloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Preloader background thread started");
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
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                hideKeyboard();
                mProgressBar.setVisibility(View.VISIBLE);
                mItems = new ArrayList<>();
                setupAdapter();
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
    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = (View) getActivity().getCurrentFocus();
        // Make sure no error is thrown if keyboard is already closed
        IBinder windowToken = currentFocus == null ? null : currentFocus.getWindowToken();
        inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                mItems = new ArrayList<>();
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
        View v = inflater.inflate(R.layout.fragment_image_loader,container,false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.circle_loader);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.image_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mNumColumns));
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();
                int totalItems = lm.getItemCount();
                int lastVisibleItem = lm.findLastVisibleItemPosition();
                preloadImages();

                if ((lastVisibleItem + 10) >= totalItems && mPageNumber < 10) {
                    // Call api and append items (Temporarily replace)
                    mPageNumber++;
                    updateItems();
                }
            }
        });

        // Layout listener for updating column count based on width at runtime
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                GridLayoutManager manager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                float currentWidth = manager.getWidth();
                mNumColumns = (int) currentWidth / 400;
                manager.setSpanCount(mNumColumns);
            }
        });
        registerForContextMenu(mPhotoRecyclerView);

        setupAdapter();
        return v;
    }

    private class ImageHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView ;
        private ImageItem mImageItem;

        public ImageHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);


        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindImageItem(ImageItem imageItem){
            mImageItem = imageItem;
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        Toast.makeText(getActivity(),
                getString(R.string.context_menu_download)
                        + " "
                        + String.valueOf(mAdapter.getPosition())
                , Toast.LENGTH_SHORT).show();

        return super.onContextItemSelected(item);
    }

    private class PhotoAdapter extends RecyclerView.Adapter<ImageHolder> {

        private List<ImageItem> mImageItems;

        private int position;

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public PhotoAdapter(List<ImageItem> imageItems) {
            mImageItems = imageItems;
        }


        @Override
        public ImageHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_image, viewGroup, false);
            return new ImageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ImageHolder holder, final int position) {
            ImageItem imageItem = mImageItems.get(position);
            holder.bindImageItem(imageItem);

            // current image
            Drawable currentImage = getResources().getDrawable(R.drawable.ic_collections_black_24dp);
            ImageLruCache imageLruCache = ImageLruCache.get(getContext());

            if (imageLruCache.getBitmapFromMemCache(imageItem.getUrl()) != null) {
                currentImage = new BitmapDrawable(getResources(), imageLruCache.getBitmapFromMemCache(imageItem.getUrl()));
                Log.i(TAG, "Found in cache, no need to download image");
            } else {
                mThumbnailDownloader.queueThumbnail(holder, imageItem.getUrl());
            }
            holder.bindDrawable(currentImage);
            holder.mItemImageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(Menu.NONE,v.getId(),0,R.string.context_menu_download)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    setPosition(position);
                                    return false;
                                }
                            });
                }
            });
        }

        @Override
        public int getItemCount() {
            return mImageItems.size();
        }
    }

    private void setupAdapter() {
        //  Return true if the fragment is currently added to its activity
        if (isAdded()) {
            mAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mAdapter);
        }
    }

    private void preloadImages() {
        GridLayoutManager lm = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
        int firstVisiblePosition = lm.findFirstVisibleItemPosition();
        int lastVisiblePosition = lm.findLastVisibleItemPosition();
        ImageLruCache imageLruCache = ImageLruCache.get(getContext());

        if (firstVisiblePosition - 10 >= 0) {
            // load previous 10 images into cache
            for (int position = firstVisiblePosition - 10; position < firstVisiblePosition; position++) {
                String url = mItems.get(position).getUrl();

                if (imageLruCache.getBitmapFromMemCache(url) != null) {
                    // No need to re-download this
                    continue;
                }
                mThumbnailPreloader.queueThumbnail(position, url);
            }
            // load next 10 images
            if (lastVisiblePosition + 10 <= mItems.size()) {
                for (int position = lastVisiblePosition + 1; position < lastVisiblePosition + 10; position++) {
                    String url = mItems.get(position).getUrl();

                    if (imageLruCache.getBitmapFromMemCache(url) != null) {
                        // no need to re-download this
                        Log.i(TAG, "Already cached");
                        continue;
                    }
                    mThumbnailPreloader.queueThumbnail(position, url);
                }
            }
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

            if (mQuery == null) {
                return new FlickrHelper().fetchRecentImage(mPageNumber);
            } else {
                return new FlickrHelper().searchImage(mQuery, mPageNumber);
            }
        }

        //work in main stream
        @Override
        protected void onPostExecute(List<ImageItem> items) {
            if (mProgressBar.getVisibility() == View.VISIBLE) {
                mProgressBar.setVisibility(View.GONE);
            }

            if (items.size() == 0) {
                return;
            }

            if (mItems.size() == 0) {
                mItems = items;
                QueryPreferences.setLastResultId(getActivity(), mItems.get(0).getId());
                setupAdapter();
            } else {
                int oldSize = mItems.size();
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyItemRangeInserted(oldSize, items.size());
            }
        }
    }
}