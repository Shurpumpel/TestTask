package com.fedotov.testtask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class listBlocks extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DataAdapter dataAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_blocks);
        recyclerView = findViewById(R.id.list_of_tags);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);


        List<TagWithExpression> names = (List<TagWithExpression>) getIntent().
                getSerializableExtra(Intent.EXTRA_TEXT);

        dataAdapter = new DataAdapter(names);
        recyclerView.setAdapter(dataAdapter);
    }
}