package com.example.ids.DBModels;

import androidx.annotation.NonNull;

import com.example.ids.login.LoginActivity;

import org.json.JSONObject;

import io.socket.client.Socket;


public class User implements Comparable<User>{
    private String user_id;
    private String given_name;
    private String phone;
    private String role;

    private Boolean flag_phone_role_retrieved = false;
    private final Object lock = new Object();

    /**
     *
     * @param user_id the user's id
     * @param given_name the user's given name
     */
    public User(String user_id, String given_name) {
        this.user_id = user_id;
        this.given_name = given_name;
        phone = "";
        role = "";
    }

    public User(String user_id, String given_name, String phone, String role) {
        this.user_id = user_id;
        this.given_name = given_name;
        this.phone = phone;
        this.role = role;
    }


    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(String given_name) {
        this.given_name = given_name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void update_from_db(Socket socket){

        String[] temp_role = new String[1];
        String[] temp_phone = new String[1];
        socket.on("getRoleAndPhoneOfUser_sock", args -> {
            flag_phone_role_retrieved = false;
            synchronized (lock) {
                JSONObject message = (JSONObject) args[0];
                try {
                    temp_role[0] = message.getJSONArray("role").getJSONObject(0).getString("role");
                    temp_phone[0] = message.getJSONArray("phone").getJSONObject(0).getString("phone");
                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    // procede to store role and phone into the corresponding object's fields
                    flag_phone_role_retrieved = true;
                    lock.notifyAll();
                }
            }
        });

        synchronized (lock) {
            socket.emit("getRoleAndPhoneOfUser_call", user_id, LoginActivity.groupID);
            while(!flag_phone_role_retrieved){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            role = temp_role[0];
            phone = temp_phone[0];
            flag_phone_role_retrieved = false;
        }

    }
    @NonNull
    public String toString(){
        return given_name + ", " + role + ", " + phone;
    }

    @Override
    public int compareTo(User user) {
        return given_name.compareTo(user.getGiven_name());
    }
}
