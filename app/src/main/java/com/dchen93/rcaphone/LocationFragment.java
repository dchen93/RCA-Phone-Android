package com.dchen93.rcaphone;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class LocationFragment extends Fragment {
    private LocationAdapter mLocationAdapter;
    public View rootView;
    private CenterLocation[] centerLocations;

    public LocationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_location, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateLocation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ArrayList<CenterLocation> centers = new ArrayList<>();

        mLocationAdapter = new LocationAdapter(getActivity(), centers);
//        mLocationAdapter =
//                new ArrayAdapter<>(
//                        getActivity(),
//                        R.layout.list_item_location,
//                        R.id.list_item_location_textview,
//                        new ArrayList<CenterLocation>()
//                );

        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_location);
        listView.setAdapter(mLocationAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CenterLocation location = mLocationAdapter.getItem(position);
                mLocationAdapter.clear();
                for (CenterLocation currCenter : centerLocations) {
                    if (location == currCenter) {
                        currCenter.location = true;
                    } else {
                        currCenter.location = false;
                    }
                    mLocationAdapter.add(currCenter);
                }
                UpdateLocationTask updateServer = new UpdateLocationTask();
                updateServer.execute();
            }
        });


        return rootView;
    }

    private void updateLocation() {
        FetchLocationTask fetchLocation = new FetchLocationTask();
        fetchLocation.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateLocation();
    }

    public class CenterLocation {
        String center;
        boolean location;

        public CenterLocation(String center, boolean location) {
            this.center = center;
            this.location = location;
        }
    }

    public class UpdateLocationTask extends AsyncTask<Void, Void, Void> {
        private final String LOG_TAG = UpdateLocationTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            rootView.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            TextView name = (TextView) rootView.findViewById(R.id.progress);
            name.setText(R.string.sending);
        }

        private String writeJSON() throws JSONException {
            JSONObject json = new JSONObject();
            JSONArray centers = new JSONArray();

            for (int i = 0; i < 3; i++) {
                JSONObject center = new JSONObject();
                try {
                    center.put("current_location", centerLocations[i].location);
                    center.put("center_name", centerLocations[i].center);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                centers.put(center);
            }

            try {
                json.put("center_set", centers);
                json.put("phone_name", "RCA Phone");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return json.toString();
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            int phone = 1; //for the RCA Phone

            String json = null;
            try {
                json = writeJSON();
            } catch (JSONException e) {
                e.printStackTrace();
                return params[0];
            }

            try {
                final String BASE_URL = "http://rcaphone.herokuapp.com/api/phones";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(Integer.toString(phone))
                        .build();

                String withSlash = builtUri.toString() + '/';
                URL url = new URL(withSlash);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.connect();

                OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());
                outputStream.write(json.getBytes());
                outputStream.flush();
                outputStream.close();

//                Log.e(LOG_TAG, builtUri.toString());
//                Log.e(LOG_TAG, json);

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    return null;
                }


                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateView();
        }
    }

    public class LocationAdapter extends ArrayAdapter<CenterLocation> {
        public LocationAdapter(Context context, ArrayList<CenterLocation> centers) {
            super(context, 0, centers);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            CenterLocation center = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_location, parent, false);
            }

            TextView list_item_location_textview = (TextView) convertView.findViewById(R.id.list_item_location_textview);

            list_item_location_textview.setText(center.center);

            if (center.location) {
                list_item_location_textview.setBackgroundColor(Color.WHITE);
                list_item_location_textview.setTextColor(Color.DKGRAY);
            } else {
                list_item_location_textview.setBackgroundColor(Color.DKGRAY);
                list_item_location_textview.setTextColor(Color.WHITE);
            }

            return convertView;
        }
    }

    private void updateView() {
        rootView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        if (centerLocations != null) {
            mLocationAdapter.clear();
            for (CenterLocation centerInfo : centerLocations) {
                mLocationAdapter.add(centerInfo);
            }
        }
    }

    public class FetchLocationTask extends AsyncTask<Void, Void, Void> {
        private final String LOG_TAG = FetchLocationTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            rootView.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            TextView name = (TextView) rootView.findViewById(R.id.progress);
            name.setText(R.string.loading);
        }

        private void getLocationFromJson(String locationJsonStr) throws JSONException {
//            final String PHONE = "phone_name";
//            final String UPDATED = "updated_time";
            final String CENTERS = "center_set";
            final String NAME = "center_name";
            final String CURRENT = "current_location";

            JSONObject locationJson = new JSONObject(locationJsonStr);
            JSONArray locationArray = locationJson.getJSONArray(CENTERS);

            centerLocations = new CenterLocation[locationArray.length()];
            for (int i = 0; i < locationArray.length(); i++) {
                String center;
                boolean location;

                JSONObject centerInfo = locationArray.getJSONObject(i);

                center = centerInfo.getString(NAME);
                location = centerInfo.getBoolean(CURRENT);

                CenterLocation currentCenter = new CenterLocation(center, location);
                centerLocations[i] = currentCenter;
            }
        }

        protected Void doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String locationJsonStr = null;

            int phone = 1; //for the RCA Phone
            String format = "json";

            try {
                final String BASE_URL = "http://rcaphone.herokuapp.com/api/phones";
                final String FORMAT_PARAM = "format";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(Integer.toString(phone))
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .build();

                URL url = new URL(builtUri.toString());

//                Log.e(LOG_TAG, "URL = " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                locationJsonStr = buffer.toString();
//                Log.e(LOG_TAG, "JSON " + locationJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                getLocationFromJson(locationJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            updateView();
        }
    }
}
