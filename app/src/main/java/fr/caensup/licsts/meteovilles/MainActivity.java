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

import org.json.JSONArray;
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
                searchCitiesByName(cityName); // Recherche les villes correspondantes
                etNewCity.setText("");
            } else {
                Toast.makeText(this, "Entrez un nom de ville", Toast.LENGTH_SHORT).show();
            }
        });

        // Bouton Refresh
        btnRefresh.setOnClickListener(v -> refreshWeatherData());

        // Suppression d'une ville par clic simple
        lvCities.setOnItemClickListener((parent, view, position, id) -> {
            // Récupérez la ville à supprimer
            City cityToRemove = cities.get(position);

            // Boîte de dialogue de confirmation
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirmation de suppression")
                    .setMessage("Voulez-vous supprimer la ville : " + cityToRemove.getName() + " ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        // Supprimez la ville si l'utilisateur confirme
                        cities.remove(position);
                        adapter.notifyDataSetChanged(); // Rafraîchit la liste
                        saveCitiesToFile(); // Enregistre la nouvelle liste
                        Toast.makeText(MainActivity.this, "Ville supprimée : " + cityToRemove.getName(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Non", (dialog, which) -> {
                        // Ne rien faire si l'utilisateur annule
                        dialog.dismiss();
                    })
                    .show();
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
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + city.getLat()
                + "&lon=" + city.getLon()
                + "&units=metric&lang=fr&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur réseau pour " + city.getName(), Toast.LENGTH_SHORT).show());
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
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ville non trouvée : " + city.getName(), Toast.LENGTH_SHORT).show());
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

    private void searchCitiesByName(String cityName) {
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=5&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONArray json = new JSONArray(responseData);
                        if (json.length() == 0) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Aucune ville trouvée", Toast.LENGTH_SHORT).show());
                        } else {
                            // Liste temporaire pour stocker les villes avec météo
                            List<City> cityList = new ArrayList<>();
                            for (int i = 0; i < json.length(); i++) {
                                JSONObject cityObject = json.getJSONObject(i);
                                String name = cityObject.getString("name");
                                String country = cityObject.getString("country");
                                double lat = cityObject.getDouble("lat");
                                double lon = cityObject.getDouble("lon");

                                City city = new City(name, country, lat, lon);
                                cityList.add(city);

                                // Récupérer la météo pour chaque ville
                                fetchTemperatureForCity(city, cityList);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur dans la réponse", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchTemperatureForCity(City city, List<City> cityList) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + city.getLat()
                + "&lon=" + city.getLon()
                + "&units=metric&lang=fr&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Pas de température récupérée, on continue sans
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur réseau pour " + city.getName(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        JSONObject main = json.getJSONObject("main");
                        String temperature = main.getString("temp") + "°C";

                        // Ajouter la température à la ville
                        city.setTemperature(temperature);

                        // Vérifier si toutes les villes ont été mises à jour
                        if (allCitiesHaveTemperature(cityList)) {
                            runOnUiThread(() -> showCitySelectionDialog(cityList));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private boolean allCitiesHaveTemperature(List<City> cityList) {
        for (City city : cityList) {
            if (city.getTemperature().equals("N/A")) {
                return false;
            }
        }
        return true;
    }


    private void showCitySelectionDialog(List<City> cityList) {
        // Préparer les noms des villes avec la température
        String[] cityNames = new String[cityList.size()];
        for (int i = 0; i < cityList.size(); i++) {
            City city = cityList.get(i);
            cityNames[i] = city.getName() + ", " + city.getCountry() + " (" + city.getTemperature() + ")";
        }

        // Créer la boîte de dialogue
        new AlertDialog.Builder(this)
                .setTitle("Choisissez une ville")
                .setItems(cityNames, (dialog, which) -> {
                    // L'utilisateur a sélectionné une ville
                    City selectedCity = cityList.get(which);
                    addCityToList(selectedCity.getName(), selectedCity.getCountry(), selectedCity.getLat(), selectedCity.getLon());
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void addCityToList(String name, String country, double lat, double lon) {
        if (isCityAlreadyInList(name, country, lat, lon)) {
            runOnUiThread(() -> Toast.makeText(this, "La ville est déjà dans la liste", Toast.LENGTH_SHORT).show());
            return;
        }

        City newCity = new City(name);
        newCity.setCountry(country);
        newCity.setLat(lat);
        newCity.setLon(lon);

        cities.add(newCity);
        adapter.notifyDataSetChanged();
        saveCitiesToFile(); // Sauvegarder les villes
        fetchWeatherForCity(newCity); // Récupérer les données météo
    }


    private boolean isCityAlreadyInList(String name, String country, double lat, double lon) {
        for (City city : cities) {
            if (city.getName().equalsIgnoreCase(name) &&
                    city.getCountry().equalsIgnoreCase(country) &&
                    city.getLat() == lat &&
                    city.getLon() == lon) {
                return true; // La ville existe déjà
            }
        }
        return false; // La ville n'est pas dans la liste
    }
}