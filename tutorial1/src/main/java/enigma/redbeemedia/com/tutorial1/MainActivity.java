package enigma.redbeemedia.com.tutorial1;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.test)).setText(EnigmaRiverContext.getVersion());
    }
}
