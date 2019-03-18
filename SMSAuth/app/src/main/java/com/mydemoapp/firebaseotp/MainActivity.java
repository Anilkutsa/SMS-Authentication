package com.mydemoapp.firebaseotp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int RC_SIGN_IN = 001;
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean phoneNumberVerified = false;

    public static final String PREF_NAME = "pref_name";
    public static final String VERIFICATION_START_TIME = "ver_start_time";
    public static final String OTP_RECIEVED_TIME = "otp_recieved_time";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private EditText phoneNumberField, smsCodeVerificationField;
    private Button startVerficationButton, verifyPhoneButton;
    private ProgressBar progressCircular;

    private String verificationid;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //intialized firebase auth
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            //   Toast.makeText(getApplicationContext(), "User already signed in", Toast.LENGTH_SHORT).show();
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        phoneNumberField = findViewById(R.id.phone_number_edt);
        smsCodeVerificationField = findViewById(R.id.sms_code_edt);
        startVerficationButton = findViewById(R.id.start_auth_button);
        verifyPhoneButton = findViewById(R.id.verify_auth_button);
        progressCircular = findViewById(R.id.progress_circular);

        startVerficationButton.setOnClickListener(this);
        verifyPhoneButton.setOnClickListener(this);

        smsCodeVerificationField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() > 5) {
                    verifyPhoneButton.setVisibility(View.VISIBLE);
                } else {
                    verifyPhoneButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
                progressCircular.setVisibility(View.GONE);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.e(TAG, e.getMessage());
                progressCircular.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                Log.e(TAG, "onCodeSent - " + forceResendingToken);
                verificationid = s;
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                verifyPhoneButton.setVisibility(View.VISIBLE);
                progressCircular.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "OTP code timeout. Please try verifying your number again.", Toast.LENGTH_LONG).show();
            }
        };
    }


    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            smsCodeVerificationField.setText(credential.getSmsCode());
                            verifyPhoneButton.setVisibility(View.VISIBLE);
                            Log.e(TAG, "Got OTP At - " + System.currentTimeMillis());
                            sharedPreferences.edit().putLong(OTP_RECIEVED_TIME, System.currentTimeMillis()).commit();
                            if (phoneNumberVerified) {
                                startActivity(new Intent(getApplicationContext(), Main2Activity.class));
                            }
                            phoneNumberVerified = true;
                            //startActivity(new Intent(getApplicationContext(), Main2Activity.class));
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                smsCodeVerificationField.setError("Invalid code.");
                            }
                        }
                    }
                });
    }

    private boolean validatePhoneNumberAndCode() {
        String phoneNumber = phoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberField.setError("Invalid phone number.");
            return false;
        }
        return true;
    }

    private boolean validateSMSCode() {
        String code = smsCodeVerificationField.getText().toString();
        if (TextUtils.isEmpty(code)) {
            smsCodeVerificationField.setError("Enter verification Code.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id) {
            case R.id.start_auth_button:
                if (!validatePhoneNumberAndCode()) {
                    return;
                }
                hideKeyboard();
                Log.e(TAG, "Verification Started At - " + System.currentTimeMillis());
                sharedPreferences.edit().putLong(VERIFICATION_START_TIME, System.currentTimeMillis()).commit();
                Toast.makeText(this, "Started Verification. Please wait for OTP message.", Toast.LENGTH_SHORT).show();
                progressCircular.setVisibility(View.VISIBLE);
                startPhoneNumberVerification(phoneNumberField.getText().toString());
                break;
            case R.id.verify_auth_button:
                if (!validateSMSCode()) {
                    return;
                }

                hideKeyboard();
                if (phoneNumberVerified) {
                    startActivity(new Intent(getApplicationContext(), Main2Activity.class));
                }
                verifyPhoneNumberWithCode(verificationid, smsCodeVerificationField.getText().toString());
                break;
        }

    }
}
