package com.example.vasyl.imageloader.flickr;

import android.net.Uri;
import android.util.Log;
import com.example.vasyl.imageloader.model.ImageItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrHelper {
    private static final String TAG = "FlickrHelper";
    private static final String API_KEY = "5b96b1e29e4eff1b67a1c3f5d6d77cf0";
    private static final String FETCH_RECENTS_METHOD="flickr.photos.getRecent";
    private static final String SEARCH_METHOD="flickr.photos.search";

   private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    //this method receives low-level data by URL, and return data in byte[]
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    // transformation low-level data in String
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<ImageItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }
    public List<ImageItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }


    private List<ImageItem> downloadGalleryItems(String url) {

        List<ImageItem> items = new ArrayList<>();

        try {
//            String url = Uri.parse("https://api.flickr.com/services/rest/")
//                    .buildUpon()
//                    //returns a list of the latest public photos uploaded to flickr
//                    .appendQueryParameter("method", "flickr.photos.getRecent")
//                    .appendQueryParameter("api_key", API_KEY)
//                    //format callback
//                    .appendQueryParameter("format", "json")
//                    //1 - delete name method and () from callback
//                    .appendQueryParameter("nojsoncallback", "1")
//                    //use small images
//                    .appendQueryParameter("extras", "url_s")
//                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            //parse image
            parseItems(items,jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return items;
    }

    private String buildUrl(String method,String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if (method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private void parseItems(List<ImageItem> items, JSONObject jsonBody)
            throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            ImageItem item = new ImageItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            //use only url_s
            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}
