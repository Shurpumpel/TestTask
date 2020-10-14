package com.fedotov.testtask;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class listBlocks extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_blocks);
        TextView textView = findViewById(R.id.block_of_file);
        Intent intent = getIntent();
        if(intent.hasExtra(Intent.EXTRA_TEXT))
            textView.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
    }
}