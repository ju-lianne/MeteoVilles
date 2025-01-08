package fr.caensup.licsts.meteovilles;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etNewCity;
    private Button btnAddCity, btnRefresh;
    private ListView lvCities;

    private List<City> cities;
    private CityAdapter adapter;

    private final String API_KEY = "f476e0b3c03f1d0a2a074ffe2ce51e5b";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNewCity = findViewById(R.id.etNewCity);
        btnAddCity = findViewById(R.id.btnAddCity);
        btnRefresh = findViewById(R.id.btnRefresh);
        lvCities = findViewById(R.id.lvCities);

        cities = new ArrayList<>();
        adapter = new CityAdapter(this, cities);
        lvCities.setAdapter(adapter);

        btnAddCity.setOnClickListener(v -> {
            String cityName = etNewCity.getText().toString().trim();
            if (!cityName.isEmpty()) {
                City newCity = new City(cityName);
                cities.add(newCity);
                adapter.notifyDataSetChanged();
                etNewCity.setText("");

                // Récupérez immédiatement les données météo pour la nouvelle ville
                fetchWeatherForCity(newCity);
            } else {
                Toast.makeText(this, "Entrez un nom de ville", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefresh.setOnClickListener(v -> refreshWeatherData());

        lvCities.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = cities.get(position);
            showEditDialog(city);
            return true;
        });

        lvCities.setOnItemClickListener((parent, view, position, id) -> {
            cities.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Ville supprimée", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshWeatherData() {
        for (City city : cities) {
            fetchWeatherForCity(city);
        }
    }

    private void fetchWeatherForCity(City city) {
        String cityName = city.getName();
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&units=metric&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur réseau pour " + cityName, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        JSONObject main = json.getJSONObject("main");
                        JSONObject weather = json.getJSONArray("weather").getJSONObject(0);

                        String temperature = main.getString("temp") + "°C";
                        String description = weather.getString("description");

                        // Mettez à jour les données météo de la ville
                        city.setTemperature(temperature);
                        city.setWeatherDescription(description);

                        // Mettez à jour la liste (adapter)
                        runOnUiThread(() -> adapter.notifyDataSetChanged());

                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur dans les données reçues", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ville non trouvée : " + cityName, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showEditDialog(City city) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier le nom de la ville");

        final EditText input = new EditText(this);
        input.setText(city.getName());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                city.setName(newName);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}