package enigma.redbeemedia.com.audioandsubtitles;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import com.redbeemedia.enigma.core.error.InvalidCredentialsError;
import com.redbeemedia.enigma.core.login.EnigmaLogin;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.login.ILoginResultHandler;
import com.redbeemedia.enigma.core.login.UserLoginRequest;
import com.redbeemedia.enigma.core.session.ISession;

public class LoginActivity extends Activity {
    private static final String TAG = "custom_controls";

    private Handler handler;
    private EnigmaLogin enigmaLogin;
    private Button btn_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.enigmaLogin = new EnigmaLogin(Placeholders.CUSTOMER_NAME, Placeholders.BUSINESS_UNIT_NAME);
        this.handler = new Handler();
        this.enigmaLogin.setCallbackHandler(this.handler);

        this.btn_login = (Button)findViewById(R.id.btn_login);
        this.btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login(Placeholders.USERNAME, Placeholders.PASSWORD);
            }
        });
    }

    private void login(String username, String password) {
        enigmaLogin.login(new UserLoginRequest(username, password, new ILoginResultHandler() {
            @Override
            public void onSuccess(ISession session) {
                Intent intent = new Intent(LoginActivity.this, PlaybackActivity.class);
                intent.putExtra(PlaybackActivity.EXTRA_SESSION, session);
                startActivity(intent);
            }

            @Override
            public void onError(EnigmaError error) {
                if(error instanceof InvalidCredentialsError) {
                    Toast.makeText(LoginActivity.this, "Incorrect username/password", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error, login failed: " + error.toString());
                }
            }
        }));
    }
}