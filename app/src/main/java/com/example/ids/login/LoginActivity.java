package com.example.ids.login;

import android.content.Intent;
import android.os.Bundle;

import com.example.ids.main.MainActivity;
import com.example.ids.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class LoginActivity extends AppCompatActivity {
    private EditText edtEmail;
    private EditText edtPassword;
    private AppCompatButton btnLogin;
    private Socket socket;

    /**
     * field in which is stored the user ID of the user who logged in
     */
    public static String userID;
    /**
     * field in which is stored the group ID
      */
    public static String groupID = "group-01";
    /**
     * field in which is stored the user role of the user who logged in
     */
    public static String userRole = "ruolo user";
    /**
     * field in which is stored the user name of the user who logged in
     */
    public static String userName = "name user";

    private Boolean flag_socket_created = false;
    private Boolean flag_to_main = false;


    /**
     * field to set user permissions based on role
     */
    public static final UserPermissionSupport permission = new UserPermissionSupport();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bindView();
        flag_socket_created = false;
        socketConnection();
        onLoginEvent();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = edtEmail.getText().toString();
                String password = edtPassword.getText().toString();
                socket.emit("checkLogin", user, password);
            }
        });
    }

    /**
     * sets the elements of the view
     */
    private void bindView(){
        edtEmail = findViewById(R.id.edtTxt_email);
        edtPassword = findViewById(R.id.edtTxt_password);
        btnLogin = findViewById(R.id.btn_login);
    }

    /**
     * sets socket connection
     */
    public void socketConnection(){
        if(!flag_socket_created) {

            SocketHandler handler = SocketHandler.getInstance();
            handler.setSocket();
            handler.establishConnection();
            socket = handler.getSocket();
            flag_socket_created = true;
        }
    }

    /**
     * when receiving the login event it receives a token that if equal to 1 logs in, then sets the
     * user's id, name and role and then sets the permissions based on the user's role
     */
    public void onLoginEvent(){
        socket.on("login", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            JSONObject data = (JSONObject) args[0];
                            String message = data.getString("token");

                            JSONObject data2 = (JSONObject) args[1];
                            String role = data2.getString("userRole");
                            userName = data2.getString("userName");

                            userID = data2.getString("userID");
                            userRole = role;
                            Log.d("Login", "message:" + message);
                            if (message.equals("1")){
                                switch (role){
                                    case "admin":
                                        permission.setAdminPermission();
                                        goToMain();
                                        break;
                                    case "genitore":
                                        permission.setUserPermission();
                                        goToMain();
                                        break;
                                    case "guida":
                                        permission.setGuidePermission();
                                        goToMain();
                                        break;
                                    default:
                                        Toast.makeText(LoginActivity.this,"No match",Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                            else{
                                Toast.makeText(LoginActivity.this,"Utente sbagliato",Toast.LENGTH_SHORT).show();
                            }
                        }  catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * create a new Intent to switch from LoginActivity to MainActivity
     */
    public void goToMain(){
        Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
        LoginActivity.this.startActivity(myIntent);
        flag_to_main = true;
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!flag_to_main){
            SocketHandler.getInstance().getSocket().emit("clientDisconnected");
            SocketHandler.getInstance().closeConnection();
            flag_socket_created = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        socketConnection();
        onLoginEvent();
        flag_to_main = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}