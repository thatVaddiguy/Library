package com.example.android.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<Book> books = new ArrayList<Book>();
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button search = (Button) findViewById(R.id.search_button);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText searchText = (EditText) findViewById(R.id.search_text);
                String[]searchString = new String[1];
                searchString[0]=searchText.getText().toString().replace(" ","+");
                ApiRequest request = (ApiRequest) new ApiRequest();
                request.execute(searchString);
                if(searchText.length()>0){
                    searchText =null;
                }
            }
        });

    }

    public class ApiRequest extends AsyncTask<String, Object, JSONObject> {

        @Override
        protected void onPreExecute() {

            if (!isNetworkConnected()){
                Log.e(LOG_TAG,"Not Connected to the internet");
                cancel(true);
                return;
            }
        }

        @Override
        protected JSONObject doInBackground(String... search) {

            if (isCancelled()) {
                return null;
            }

            String apiUrlString = "https://www.googleapis.com/books/v1/volumes?q="+search[0]+"&maxResults=10";
            try {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(apiUrlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Log.e(LOG_TAG, "The response code is" + responseCode);
                    connection.disconnect();
                    return null;
                }

                StringBuilder builder = new StringBuilder();
                InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader responseReader = new BufferedReader(inputStreamReader);
                String line = responseReader.readLine();
                while (line != null) {
                    builder.append(line);
                    line = responseReader.readLine();
                }
                String responseString = builder.toString();
                JSONObject responseJson = new JSONObject(responseString);

                connection.disconnect();
                return responseJson;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSONExcepton");
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException");
                e.printStackTrace();
                return null;
            }

        }



        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            books.removeAll(books);
            try {
                JSONArray results = jsonObject.getJSONArray("items");
                int length = results.length();
                for (int i = 0; i < length; i++) {
                    JSONObject obj = results.getJSONObject(i).getJSONObject("volumeInfo");
                    String title = obj.optString("title");
                    String author = obj.optString("authors").replace("[","").replace("]","").replace("\"","").replace(",",", ");
                    String publisher = obj.optString("publisher");
                    books.add(new Book(title, author, publisher));
                }

                BookAdapter adapter = new BookAdapter(MainActivity.this, books);
                ListView listView = (ListView) findViewById(R.id.list_books);
                TextView empty = (TextView) findViewById(R.id.empty);
                empty.setVisibility(View.GONE);
                listView.setAdapter(adapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    protected boolean isNetworkConnected(){
        if (connectivityManager==null){
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo!=null&&networkInfo.isConnected()){
            return true;
        }else {
            return false;
        }
    }
}
