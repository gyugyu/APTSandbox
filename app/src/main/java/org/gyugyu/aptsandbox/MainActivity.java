package org.gyugyu.aptsandbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

@TestAnnotation
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
