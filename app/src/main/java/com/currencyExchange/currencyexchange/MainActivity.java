package com.currencyExchange.currencyexchange;

import androidx.appcompat.app.AppCompatActivity;

// Related to design and xml files
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

// Related to api requests and Json handling
import com.osamah.currencyexchange.R;
import org.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private String baseCurrency;
    private String data = "";

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        baseCurrency = "usd";
        EditText searchBar = findViewById(R.id.searchView);
        EditText amountEt = findViewById(R.id.amountEt);
        TextWatcher onChange = new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isConnected())
                    filterContent(searchBar.getText().toString(), amountEt.getText().toString());
                else
                    loadContent(baseCurrency);
            }
        };
        searchBar.addTextChangedListener(onChange);
        amountEt.addTextChangedListener(onChange);
        loadContent(baseCurrency);
    }

    @Override
    protected void onResume() {
        loadContent(baseCurrency);
        super.onResume();
    }
    @Override
    protected void onStart() {
        loadContent(baseCurrency);
        super.onStart();
    }
    @Override
    protected void onRestart() {
        loadContent(baseCurrency);
        super.onRestart();
    }
    @Override
    protected void onPause() {
        EditText searchBar = findViewById(R.id.searchView);
        EditText amountEt = findViewById(R.id.amountEt);
        searchBar.clearFocus();
        amountEt.clearFocus();
        super.onPause();
    }
    @Override
    protected void onStop() {
        EditText searchBar = findViewById(R.id.searchView);
        EditText amountEt = findViewById(R.id.amountEt);
        searchBar.clearFocus();
        amountEt.clearFocus();
        super.onStop();
    }

    public void showCurrencies(View v){
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.inflate(R.menu.currencies_menu);
            popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        try {
            String currency = item.getTitle().toString();
            baseCurrency = currency;
            ((LinearLayout)findViewById(R.id.CurrenciesLayout)).removeAllViews();
            loadContent(currency);
            return true;
        } catch (Exception e){
            TextView t = findViewById(R.id.titleTV);
            t.setText(e.toString());
        }
        return false;
    }

    private void loadContent(String baseCurrency){
        String apiUrl = "http://www.floatrates.com/daily/" + baseCurrency +".json";
        Button baseCurrencyBtn = findViewById(R.id.baseCurrency);
        ((LinearLayout)findViewById(R.id.CurrenciesLayout)).removeAllViews();
        baseCurrencyBtn.setText("Base Currency: " + baseCurrency.toUpperCase());
        if(isConnected()){
            ProgressBar loading = new ProgressBar(this);
            ((LinearLayout)findViewById(R.id.CurrenciesLayout)).addView(loading);
            new ApiCallAsyncTask().execute(apiUrl);
        } else{
            data = "";
            TextView textView = new TextView(this);
            textView.setText("Your device is not connected to internet");
            textView.setTextColor(Color.WHITE);
            Button button = new Button(this);
            button.setText("Reload");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadContent(baseCurrency);
                }
            });
            ((LinearLayout)findViewById(R.id.CurrenciesLayout)).addView(textView);
            ((LinearLayout)findViewById(R.id.CurrenciesLayout)).addView(button);
        }
    }

    private void filterContent(String filterFactor, String amountString){
        filterFactor = filterFactor.toLowerCase();
        LinearLayout countriesLayout = findViewById(R.id.CurrenciesLayout);
        countriesLayout.removeAllViews();

        try {
            // Parse and handle the JSON response
            JSONObject countries = new JSONObject(data);
            // Example: Extract data from JSON
            for (Iterator<String> it = countries.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject country = countries.getJSONObject(key);
                if (country.getString("code").toLowerCase().contains("ils"))
                    continue;
                if(country.get("name").toString().toLowerCase().contains(filterFactor) ||
                        country.get("code").toString().toLowerCase().contains(filterFactor)){
                    //check if amount is valid number
                    String baseString;
                    String rateString;
                    double baseAmount;
                    double rateAmount;
                    Double amount;
                    try {
                        if(amountString.contains("f"))
                            throw new RuntimeException();
                        amount = Double.valueOf(amountString);
                    } catch (Exception e){
                        amount = (double) 1;
                    }
                    baseAmount = (double) ((Math.round(amount * 100)) / 100);
                    rateAmount = (double) ((Math.round(( country.getDouble("rate")) * 100) * amount) / 100) ;
                    baseString = String.valueOf(baseAmount) + " " + baseCurrency.toUpperCase();
                    rateString = String.valueOf(rateAmount) + " " + country.getString("code").toUpperCase();
                    makeCurrencyView(country.get("name").toString(), baseString,
                            rateString);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void about(View v){
        Toast toast = new Toast(this);
        toast.setText("We use 3rd party api to fetch an approximate currency prices");
        toast.show();
    }
    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private class ApiCallAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            String result = "";
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try {
                    InputStream inputStream = connection.getInputStream();
                    result = convertInputStreamToString(inputStream);
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                TextView titleTV = findViewById(R.id.titleTV);
                titleTV.setText(e.toString());
            }
            return result;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            data = result;
            TextView titleTV = findViewById(R.id.titleTV);
            Button baseCurrencyBtn = findViewById(R.id.baseCurrency);
            ProgressBar progressBar = findViewById(R.id.loadingBar);
            try {
                // Process the API response on the main thread
                handleApiResponse(result);
                if(progressBar != null)
                    progressBar.setVisibility(View.GONE);
            } catch (Exception e){
                titleTV.setText("Obs, something went wrong!");
            }
        }
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        reader.close();
        return stringBuilder.toString();
    }

    private void handleApiResponse(String result) {
        EditText searchBar = findViewById(R.id.searchView);
        EditText amountEt = findViewById(R.id.amountEt);
        filterContent(searchBar.getText().toString(), amountEt.getText().toString());
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    private void makeCurrencyView(String name, String baseString, String rateString){

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackground(getResources().getDrawable(R.drawable.currency_container));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, 20, 20, 20);
        linearLayout.setLayoutParams(layoutParams);

        // Create the TextView
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setText(name + "\n" + baseString + " = " + rateString);
        textView.setTextColor(getResources().getColor(R.color.primaryCurrencyTextColor));
        LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textLayoutParams.setMargins(30, 30, 30, 30);
        textView.setLayoutParams(textLayoutParams);

        // Add the TextView to the LinearLayout
        linearLayout.addView(textView);

        // Add the layout to our activity
        LinearLayout CurrenciesLayout = (LinearLayout) findViewById(R.id.CurrenciesLayout);
        CurrenciesLayout.addView(linearLayout);
    }

}