package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.handlers.ServerManager;
import com.example.myapplication.handlers.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.username_edittext) TextView userName;
    @BindView(R.id.password_edittext) TextView password;
    @BindView(R.id.login_button) Button loginButton;
    @BindView(R.id.create_acc_button) Button regButton;

    public boolean loginFlag = true;
    SessionManager session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        session = new SessionManager(getApplicationContext());
        ButterKnife.bind(this);

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent Registration = new Intent(getApplicationContext(), RegistrationActivity.class);
                startActivity(Registration);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Login();
            }
        });
    }

    private void Login() {
        if (!Validate()) {
            return;
        }
        final String login = userName.getText().toString();
        final String pass = password.getText().toString();
        final JSONObject person = new JSONObject();
        try {
            person.accumulate("login",login);
        } catch (JSONException e){return;}
        try {
            person.accumulate("password", pass);
        } catch (JSONException e){return;}

        Log.d("loginData", person.toString());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String message = person.toString();
                boolean requestFlag = ServerManager.SendJsonToServer(
                        message,"POST","login");
                if (!requestFlag){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowToastInCenterOfScreen("can't connect to server");
                        }
                    });
                    loginFlag = false;
                }
                final String msg;
                String err = ServerManager.GetStringErr();
                String resp = ServerManager.GetStringResponse();
                if (err.equals("")) {
                    msg = resp;
                } else {
                    msg = err;
                    loginFlag = false;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ShowToastInCenterOfScreen(msg);
                    }
                });
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            return;
        }

        if (!loginFlag) {
            return;
        }
        session.createLoginSession(login);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        this.finish();
    }


    private boolean Validate() {
        boolean ValidateFlag = true;
        String login = userName.getText().toString();
        String pass = password.getText().toString();

        if (login.isEmpty() || login.length() < 3) {
            userName.setError("at least 3 characters");
            ValidateFlag = false;
        } else {
            userName.setError(null);
        }

        if (pass.isEmpty()) {
            password.setError("password can't be empty");
            ValidateFlag = false;
        } else {
            password.setError(null);
        }

        return ValidateFlag;
    }

    private void ShowToastInCenterOfScreen(String message) {
        Toast toast =  Toast.makeText(
                getApplicationContext(),message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
