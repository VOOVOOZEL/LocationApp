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

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegistrationActivity extends AppCompatActivity  {

    @BindView(R.id.reg_button) Button regButton;
    @BindView(R.id.username_edittext) TextView userName;
    @BindView(R.id.password_edittext) TextView password;
    @BindView(R.id.confirm_passwod_edittext) TextView confirmationPassword;
    @BindView(R.id.email_edittext) TextView email;
    @BindView(R.id.password_rules) TextView rules;

    public boolean registerFlag = true;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registartion_activity);
        session = new SessionManager(getApplicationContext());
        ButterKnife.bind(this);

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RegisterUser();
            }
        });
    }

    private void RegisterUser() {
        if (!Validate()) {
            return;
        }

        String login = userName.getText().toString();
        String mail = email.getText().toString();
        String pass = password.getText().toString();

        final JSONObject person = new JSONObject();

        try {
            person.accumulate("login",login);
        } catch (JSONException e){return;}
        try {
            person.accumulate("email", mail);
        } catch (JSONException e){return;}
        try {
            person.accumulate("password", pass);
        } catch (JSONException e){return;}

        Log.d("regData", person.toString());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String message = person.toString();
                boolean requestFlag = ServerManager.SendJsonToServer(
                        message,"POST","register");
                if (!requestFlag){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowToastInCenterOfScreen("can't connect to server");
                        }
                    });
                    registerFlag = false;
                }
                final String msg;
                String err = ServerManager.GetStringErr();
                String resp = ServerManager.GetStringResponse();
                if (err.equals("")) {
                    msg = resp;
                } else {
                    msg = err;
                    registerFlag = false;
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
        if (!registerFlag) {
            return;
        }

        session.createLoginSession(login);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        this.finish();
    }

    private boolean Validate() {
        boolean flag = true;
        String login = userName.getText().toString();
        String mail = email.getText().toString();
        String pass = password.getText().toString();
        String confPass = confirmationPassword.getText().toString();

        if (login.isEmpty() || login.length() < 3) {
            userName.setError("at least 3 characters");
            flag = false;
        } else {
            userName.setError(null);
        }

        if (mail.isEmpty()) {
            email.setError("e-mail can't be empty");
            flag = false;
        } else if (!EMAIL_ADDRESS_PATTERN.matcher(mail).matches()) {
            email.setError("wrong format of e-mail");
            flag = false;
        } else {
            email.setError(null);
        }

        if (pass.isEmpty() || pass.length() < 8) {
            password.setError("at least 8 characters");
            flag = false;
        }
        else if (!isStrong(pass)) {
            password.setError("password isn't strong");
            rules.setText("Password must contains:\n uppercase letter" +
                    "\nlowercase letter \nspecial letter \ndigit");
            flag = false;
        }
        else {
            password.setError(null);
        }

        if (confPass.isEmpty()) {
            confirmationPassword.setError("confirmation password can't be empty");
            flag = false;
        } else {
            confirmationPassword.setError(null);
        }

        if (!confPass.equals(pass)) {
            confirmationPassword.setError("passwords must be equal");
            password.setError("passwords must be equal");
            flag = false;

        }
        return flag;
    }

    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    private boolean isStrong(String password){
        if (password == null) return false;
        if (password.length() < 8 || password.length() > 100) return false;

        boolean containsUpperCase = false;
        boolean containsLowerCase = false;
        boolean containsDigit = false;
        boolean containsSpecialCharacter = false;
        for(char ch: password.toCharArray()){
            if(Character.isUpperCase(ch)) containsUpperCase = true;
            if(Character.isLowerCase(ch)) containsLowerCase = true;
            if(Character.isDigit(ch)) containsDigit = true;
            if(!Character.isAlphabetic(ch) && !Character.isDigit(ch)) containsSpecialCharacter = true;
        }
        return containsUpperCase && containsLowerCase && containsDigit && containsSpecialCharacter;

    }

    private void ShowToastInCenterOfScreen(String message) {
        Toast toast =  Toast.makeText(
                getApplicationContext(),message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
