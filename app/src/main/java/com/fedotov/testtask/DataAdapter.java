package com.fedotov.testtask;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {


    private int itemCount;
    private List<TagWithExpression> names;

    public DataAdapter(List<TagWithExpression> names) {
        this.itemCount = names.size();
        this.names = new ArrayList<>(names);
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.tag_list_item;

        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new DataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        holder.bind(names.get(position).tagName, names.get(position).Expression);
    }

    @Override
    public int getItemCount() {
        return this.itemCount;
    }

    class DataViewHolder extends RecyclerView.ViewHolder{

        TextView tag_name_view;
        TextView tag_check_view;

        public DataViewHolder(@NonNull View itemView) {
            super(itemView);
            tag_name_view = itemView.findViewById(R.id.tag_name);
            tag_check_view = itemView.findViewById(R.id.is_tag_good);
        }

        void bind(String tagName, String goodOrBad){
            tag_name_view.setText(tagName);
            tag_check_view.setText(goodOrBad);
        }
    }
}
