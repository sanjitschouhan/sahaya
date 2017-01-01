package hitam.epics.sahaya;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends Activity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LinearLayout loginFormLinearLayout;
    private LinearLayout loadingLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        loginFormLinearLayout = (LinearLayout) findViewById(R.id.login_form);
        loadingLinearLayout = (LinearLayout) findViewById(R.id.login_loading);
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    LoginActivity.this.finish();
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }

    public void register(View view) {
        startActivity(new Intent(this, RegistrationActivity.class));
        finish();
    }

    public void login(View view) {
        EditText email = (EditText) findViewById(R.id.login_email);
        EditText password = (EditText) findViewById(R.id.login_password);

        boolean valid = true;
        email.setError("");
        if (email.getText().toString().length() == 0) {
            email.setError("Email Required");
            valid = false;
        }
        password.setError("");
        if (password.getText().toString().length() == 0) {
            password.setError("Password Required");
            valid = false;
        }

        if (valid) {
            loginFormLinearLayout.setVisibility(View.INVISIBLE);
            loadingLinearLayout.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("FIREBASE", "signInWithEmail:onComplete:" + task.isSuccessful());

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.w("FIREBASE", "signInWithEmail", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Login Failed")
                                        .setMessage("Invalid Username or Password")
                                        .setPositiveButton("Try Again", null);
                                builder.create().show();

                                loginFormLinearLayout.setVisibility(View.VISIBLE);
                                loadingLinearLayout.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }

    public void resetPassword(View view) {
        LayoutInflater inflater = getLayoutInflater();
        final View content = inflater.inflate(R.layout.reset_password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this, R.style.AppThemeAlert);
        builder.setTitle("Reset Password")
                .setView(content)
                .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText email = (EditText) content.findViewById(R.id.email);
                        if (email.getText().length() == 0) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginActivity.this);
                            builder1.setMessage("Invalid Email")
                                    .setPositiveButton("OK", null)
                                    .create().show();
                        } else {
                            mAuth.sendPasswordResetEmail(email.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginActivity.this);
                                    builder1.setMessage("Password reset email is sent to your mail.")
                                            .setPositiveButton("OK", null)
                                            .create().show();
                                }
                            });
                        }
                    }
                })
                .setNeutralButton("Cancel", null)
                .create().show();
    }
}
