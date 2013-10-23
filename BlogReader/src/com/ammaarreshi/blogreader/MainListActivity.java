package com.ammaarreshi.blogreader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainListActivity extends ListActivity {

	//Create member variables
	public static final int NUMBER_OF_POSTS = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();
	protected JSONObject mBlogData;
	protected ProgressBar mProgressBar;
	private final static String KEY_TITLE = "title";
	private final static String KEY_CONTENT = "content";
	private static String mSearchTerm = "android";
	private EditText searchTerm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_list);

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

		if(isNetworkAvailable()){
			GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
			getBlogPostsTask.execute();
			mProgressBar.setVisibility(View.VISIBLE);
		}

		else{
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
		}

		Button searchButton = (Button) findViewById(R.id.searchButton);
		searchTerm = (EditText) findViewById(R.id.searchTextField);

		searchButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mSearchTerm = searchTerm.getText().toString();
				GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
				getBlogPostsTask.execute();
				
			}
		});

	}


	/*
	 * Checking if network is available
	 */
	private boolean isNetworkAvailable() {

		ConnectivityManager manager = (ConnectivityManager) 
				getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();

		boolean isAvailable = false;

		if(networkInfo != null && networkInfo.isConnected()){
			isAvailable = true;
		}

		return isAvailable;

	}

	/*
	 * (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 * Sending data to new activity so that a webView opens up with blog post - see BlogWebViewActivity class file
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		JSONArray jsonPosts;
		try {
			JSONObject jsonObject = mBlogData.getJSONObject("responseData");
			jsonPosts = jsonObject.getJSONArray("results");
			JSONObject jsonPost = jsonPosts.getJSONObject(position);
			String blogUrl = jsonPost.getString("url");
			Intent intent = new Intent(this, BlogWebViewActivity.class);
			intent.setData(Uri.parse(blogUrl));
			startActivity(intent);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logException(e);
		}


	}

	/*
	 * Logging any necessary errors
	 */
	private void logException(Exception e) {
		Log.e(TAG, "Exception Caught: ", e);
	}

	/*
	 * Handling the response
	 */
	public void handleBlogResponse() {

		//Set visibility of progress bar to invisible once data is loaded
		mProgressBar.setVisibility(View.INVISIBLE);

		if(mBlogData == null){
			updateDisplay();
		}

		else{
			try {

				//Parse an array in the JSON file with tag "posts"
				JSONObject jsonObject = mBlogData.getJSONObject("responseData");
				JSONArray jsonPosts = jsonObject.getJSONArray("results");
				Log.v("The data looks like thsi" , jsonPosts.toString());
				ArrayList<HashMap<String, String>> blogPosts = 
						new ArrayList<HashMap<String,String>>();

				//Run loop through array of posts and get all the title and author tags together
				for(int i = 0; i<jsonPosts.length(); i++){
					JSONObject post = jsonPosts.getJSONObject(i);
					String title = post.getString(KEY_TITLE);
					title = Html.fromHtml(title).toString();

					String author = post.getString(KEY_CONTENT);
					author = Html.fromHtml(author).toString();

					//Insert the blog title and author into a hashmap
					HashMap<String, String> blogPost = new HashMap<String, String>();
					blogPost.put(KEY_TITLE, title);
					blogPost.put(KEY_CONTENT, author);

					blogPosts.add(blogPost);

				}

				//Setting these keys to the list view 
				String[] keys = { KEY_TITLE, KEY_CONTENT };
				//change the two text fields on the list with the corresponding data
				int[] ids = { android.R.id.text1, android.R.id.text2 };
				SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
				setListAdapter(adapter);

			} catch (JSONException e) {
				Log.e(TAG, "Exception caught: ", e);
			}
		}
	}

	/*
	 * Alert user if no data can be pulled
	 */
	private void updateDisplay() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.error_title));
		builder.setMessage(getString(R.string.error_message));
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();
		TextView emptyTextView = (TextView) getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.no_items));
		//		URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);

	}

	/**
	 * Create the HTTP request and async task, get response from blog and JSON
	 * @author Ammaar
	 *
	 */
	private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject>{

		@Override
		protected JSONObject doInBackground(Object... params) {
			int responseCode = -1;
			JSONObject jsonResponse = null;
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			//		    HttpGet httpget = new HttpGet("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
			HttpGet httpget = new HttpGet("https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=" 
					+ mSearchTerm + "&rsz=8");
			System.out.println(mSearchTerm);
			try {
				HttpResponse response = client.execute(httpget);
				StatusLine statusLine = response.getStatusLine();
				responseCode = statusLine.getStatusCode();

				if (responseCode == HttpURLConnection.HTTP_OK) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while((line = reader.readLine()) != null){
						builder.append(line);
					}

					jsonResponse = new JSONObject(builder.toString());
				}
				else {
					Log.i(TAG, String.format("Unsuccessful HTTP response code: %d", responseCode));
				}
			}
			catch (JSONException e) {
				logException(e);
			}
			catch (Exception e) {
				logException(e);
			}           

			return jsonResponse;
		}   

		@Override
		protected void onPostExecute(JSONObject result){

			mBlogData = result;
			handleBlogResponse();


		}


	}



}
