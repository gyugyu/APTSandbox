package org.gyugyu.aptsandbox;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

@TestAnnotation(TestBean.class)
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new MainActivityIntentBuilder(this, null, 0).build();
        Log.d(MainActivity.class.getSimpleName(), Boolean.toString(intent.hasExtra(TestBean.TEST_NAME)));
    }
}
