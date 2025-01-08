package fr.caensup.licsts.meteovilles;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etCityName;
    private Button btnGetWeather;
    private TextView tvWeatherResult;

    private final String API_KEY = "f476e0b3c03f1d0a2a074ffe2ce51e5b";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCityName = findViewById(R.id.etCityName);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        tvWeatherResult = findViewById(R.id.tvWeatherResult);

        btnGetWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = etCityName.getText().toString().trim();
                if (cityName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Entrez une ville", Toast.LENGTH_SHORT).show();
                } else {
                    getWeatherData(cityName);
                }
            }
        });
    }

    private void getWeatherData(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&units=metric&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ERROR", e.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        JSONObject main = json.getJSONObject("main");
                        JSONObject weather = json.getJSONArray("weather").getJSONObject(0);

                        String city = json.getString("name");
                        String temp = main.getString("temp") + "°C";
                        String description = weather.getString("description");

                        String result = "Ville : " + city + "\n"
                                + "Température : " + temp + "\n"
                                + "Description : " + description;

                        runOnUiThread(() -> tvWeatherResult.setText(result));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur dans les données", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ville non trouvée", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}