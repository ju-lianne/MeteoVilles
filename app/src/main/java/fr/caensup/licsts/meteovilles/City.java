package fr.caensup.licsts.meteovilles;

public class City {
    private String name;
    private String temperature;
    private String weatherDescription;
    private String weatherIcon;
    private String country;
    private double lat;
    private double lon;

    public City(String name, String country, double lat, double lon) {
        this.name = name;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
        this.temperature = "N/A"; // Par défaut
        this.weatherDescription = "N/A"; // Par défaut
        this.weatherIcon = null; // Par défaut
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }

    public City(String name) {
        this.name = name;
        this.temperature = "N/A";
        this.weatherDescription = "N/A";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        this.weatherDescription = weatherDescription;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
