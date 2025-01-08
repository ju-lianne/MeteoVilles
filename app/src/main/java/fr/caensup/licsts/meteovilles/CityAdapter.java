package fr.caensup.licsts.meteovilles;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.bumptech.glide.Glide;

public class CityAdapter extends ArrayAdapter<City> {
    private final Context context;
    private final List<City> cities;

    public CityAdapter(Context context, List<City> cities) {
        super(context, R.layout.city_item, cities);
        this.context = context;
        this.cities = cities;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.city_item, parent, false);
        }

        City city = cities.get(position);

        TextView tvCityName = convertView.findViewById(R.id.tvCityName);
        TextView tvCityWeather = convertView.findViewById(R.id.tvCityWeather);
        ImageView ivWeatherIcon = convertView.findViewById(R.id.ivWeatherIcon);

        tvCityName.setText(city.getName());
        tvCityWeather.setText(city.getTemperature() + " | " + city.getWeatherDescription());

        // Chargez l'icône météo
        String iconCode = city.getWeatherIcon();
        if (iconCode != null && !iconCode.isEmpty()) {
            String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
            Glide.with(context).load(iconUrl).into(ivWeatherIcon);
        } else {
            ivWeatherIcon.setImageResource(R.drawable.placeholder_weather); // Icône par défaut si aucune donnée
        }

        return convertView;
    }
}