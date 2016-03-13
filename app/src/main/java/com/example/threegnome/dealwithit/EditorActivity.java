package com.example.threegnome.dealwithit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by Chris on 3/13/2016.
 */
public class EditorActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_editor);

            Toolbar toolbar = (Toolbar) findViewById(R.id.topToolbar);
            setSupportActionBar(toolbar);


        }

}
