package com.example.ids.DBModels;

public class Child {
    private String child_id;
    private String given_name;

    /**
     *
     * @param child_id the child's id, as fetched from the database
     * @param given_name the child's full name
     */
    public Child(String child_id, String given_name){
        this.child_id = child_id;
        this.given_name = given_name;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public String getChild_id() {
        return child_id;
    }

    @Override
    public String toString(){
        return given_name;
    }
}
