package fr.caensup.licsts.meteovilles;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

        // Charger les villes sauvegardées
        loadCitiesFromFile();

        // Effectuer un refresh automatique des données météo au démarrage
        refreshWeatherData();

        // Ajouter une ville
        btnAddCity.setOnClickListener(v -> {
            String cityName = etNewCity.getText().toString().trim();
            if (!cityName.isEmpty()) {
                City newCity = new City(cityName);
                cities.add(newCity);
                adapter.notifyDataSetChanged();
                saveCitiesToFile(); // Sauvegarde après ajout

                // Récupérer les données météo immédiatement après ajout
                fetchWeatherForCity(newCity);

                etNewCity.setText("");
            } else {
                Toast.makeText(this, "Entrez un nom de ville", Toast.LENGTH_SHORT).show();
            }
        });

        // Bouton Refresh
        btnRefresh.setOnClickListener(v -> refreshWeatherData());

        // Suppression d'une ville par clic simple
        lvCities.setOnItemClickListener((parent, view, position, id) -> {
            cities.remove(position);
            adapter.notifyDataSetChanged();
            saveCitiesToFile();
            Toast.makeText(this, "Ville supprimée", Toast.LENGTH_SHORT).show();
        });

        // Modification d'une ville par clic long
        lvCities.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = cities.get(position);
            showEditDialog(city);
            return true;
        });
    }

    private void refreshWeatherData() {
        for (City city : cities) {
            fetchWeatherForCity(city);
        }
    }

    private void fetchWeatherForCity(City city) {
        String cityName = city.getName();
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName
                + "&units=metric&lang=fr&appid=" + API_KEY;

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
                        String icon = weather.getString("icon"); // Code de l'icône météo

                        // Mettez à jour les données météo de la ville
                        city.setTemperature(temperature);
                        city.setWeatherDescription(description);
                        city.setWeatherIcon(icon); // Enregistrez le code de l'icône

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
                saveCitiesToFile(); // Sauvegarde après modification
            } else {
                Toast.makeText(this, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveCitiesToFile() {
        StringBuilder data = new StringBuilder();
        for (City city : cities) {
            data.append(city.getName()).append("\n");
        }

        try {
            FileOutputStream fos = openFileOutput("cities.txt", MODE_PRIVATE);
            fos.write(data.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de l'enregistrement des villes", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCitiesFromFile() {
        cities.clear(); // Nettoie la liste avant de charger depuis le fichier

        try {
            FileInputStream fis = openFileInput("cities.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                cities.add(new City(line));
            }

            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            // Si le fichier n'existe pas encore, ce n'est pas une erreur critique
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du chargement des villes", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged(); // Rafraîchit l'affichage de la liste
    }

}