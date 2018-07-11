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
    //key from Flickr
    private static final String API_KEY = "5b96b1e29e4eff1b67a1c3f5d6d77cf0";
    private static final String ALL_IMAGE_METHOD ="flickr.photos.getRecent";
    private static final String SEARCH_METHOD="flickr.photos.search";

    //Uri.Builder use with queryParameter
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    //This method receives low-level data by URL, and return data in byte[]
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream byteArrayOutputStream  = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            // Otherwise everything is working, handle input stream
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byteArrayOutputStream.close();
            return byteArrayOutputStream .toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    // transformation low-level data in String
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<ImageItem> fetchRecentImage(int pageNumber) {
        String url = buildUrl(ALL_IMAGE_METHOD, null, pageNumber);
        return downloadImageItems(url);
    }

    public List<ImageItem> searchImage(String query, int pageNumber) {
        String url = buildUrl(SEARCH_METHOD, query, pageNumber);
        return downloadImageItems(url);
    }

    private String buildUrl(String method, String query, int pageNumber) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page", String.valueOf(pageNumber));
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private List<ImageItem> downloadImageItems(String url) {
        List<ImageItem> items = new ArrayList<>();

        try {
            Log.i(TAG, "download image items: URL = " + url);

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            items = parseItems(jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException jse) {
            Log.e(TAG, "Failed to parse JSON", jse);
        }
        return items;
    }

    // Parses the JSON object into a list of ImageItem
    private List<ImageItem> parseItems(JSONObject jsonBody)
            throws IOException, JSONException {
        List<ImageItem> items = new ArrayList<>();

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject imageJsonObject = photoJsonArray.getJSONObject(i);
            ImageItem item = new ImageItem();
            item.setId(imageJsonObject.getString("id"));
            item.setCaption(imageJsonObject.getString("title"));
            //use only url_s
            if (!imageJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(imageJsonObject.getString("url_s"));
            item.setOwner(imageJsonObject.getString("owner"));
            items.add(item);

        }return items;
    }
}

