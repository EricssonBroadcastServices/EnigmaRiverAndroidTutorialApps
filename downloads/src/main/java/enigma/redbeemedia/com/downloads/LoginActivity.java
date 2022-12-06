package enigma.redbeemedia.com.downloads;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidCredentialsError;
import com.redbeemedia.enigma.core.login.EnigmaLogin;
import com.redbeemedia.enigma.core.login.ILoginResultHandler;
import com.redbeemedia.enigma.core.network.BaseNetworkMonitorListener;
import com.redbeemedia.enigma.core.network.INetworkMonitor;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import enigma.redbeemedia.com.downloads.user.UserData;
import enigma.redbeemedia.com.downloads.user.UserDataHolder;
import enigma.redbeemedia.com.downloads.view.AsyncButton;

public class LoginActivity extends AppCompatActivity
{

    private Handler handler;
    private EnigmaLogin enigmaLogin;
    private NetworkListener networkListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        this.enigmaLogin = new EnigmaLogin(Placeholders.CUSTOMER_NAME, Placeholders.BUSINESS_UNIT_NAME);

        this.handler = new Handler();
        this.enigmaLogin.setCallbackHandler(this.handler);

        this.networkListener = new NetworkListener(findViewById(R.id.networkState));


        AsyncButton btn_login = (AsyncButton) findViewById(R.id.btn_login);
        AsyncButton btn_logout = (AsyncButton) findViewById(R.id.btn_logout);
        TextView signinState = findViewById(R.id.signinState);

        UserData userData = UserDataHolder.getUserData();
        if(userData == null) {
            signinState.setText("Not signed in");

            btn_logout.setVisibility(View.GONE);

            btn_login.setVisibility(View.VISIBLE);
            btn_login.setOnClickListener(v -> login());
        } else {
            signinState.setText("Signed in");

            btn_login.setVisibility(View.GONE);

            btn_logout.setVisibility(View.VISIBLE);
            btn_logout.setOnClickListener(v -> {
                UserDataHolder.logout();
                finish();
            });
        }
    }

    private void login() {
        UserDataHolder.login(new ILoginResultHandler() {
            @Override
            public void onSuccess(ISession session) {
                finish();
            }

            @Override
            public void onError(EnigmaError error) {
                if(error instanceof InvalidCredentialsError) {
                    Toast.makeText(LoginActivity.this, "Incorrect username/password", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }
        }, handler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.networkListener.attach(EnigmaRiverContext.getNetworkMonitor());
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.networkListener.detach();
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    private static class NetworkListener extends BaseNetworkMonitorListener {
        private INetworkMonitor networkMonitor = null;
        private final TextView textView;

        public NetworkListener(TextView textView) {
            this.textView = textView;
        }

        public void attach(INetworkMonitor networkMonitor) {
            detach();
            this.networkMonitor = networkMonitor;
            networkMonitor.addListener(this);
            onInternetAccessChanged(networkMonitor.hasInternetAccess());
        }

        public void detach() {
            if(networkMonitor != null) {
                networkMonitor.removeListener(this);
                networkMonitor = null;
            }
        }

        @Override
        public void onInternetAccessChanged(boolean internetAccess) {
            AndroidThreadUtil.runOnUiThread(() -> {
                textView.setText(internetAccess ? "Internet access" : "No internet access");
            });
        }
    }
}
