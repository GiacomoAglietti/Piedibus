package com.example.ids.DBModels;


public class Stop implements Comparable<Stop> {

    private int _id;
    private String name_stop;
    private String latitude;
    private String longitude;
    private String id_itinerary;

    /**
     *
     * @param _id the stop's id
     * @param name_stop the stop's name
     * @param latitude the stop's latitude
     * @param longitude the stop's longitude
     * @param id_itinerary the itinerary to which the stop is part
     */
    public Stop(int _id, String name_stop, String latitude, String longitude, String id_itinerary) {
        this._id = _id;
        this.name_stop = name_stop;
        this.latitude = latitude;
        this.longitude = longitude;
        this.id_itinerary = id_itinerary;
    }

    public String getName_stop() {
        return name_stop;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setName_stop(String name_stop) {
        this.name_stop = name_stop;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getId_itinerary() {
        return id_itinerary;
    }

    public void setId_itinerary(String id_itinerary) {
        this.id_itinerary = id_itinerary;
    }


    @Override
    public String toString() {
        return "Stop{" +
                "_id='" + _id + '\'' +
                ", name_stop='" + name_stop + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", id_itinerary='" + id_itinerary + '\'' +
                '}';
    }


    @Override
    public int compareTo(Stop stop) {
        return this._id - stop.get_id();
    }
}
