package ca.cmpt276.parentapp.newConfig;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ca.cmpt276.parentapp.R;
import ca.cmpt276.parentapp.model.Child;

public class listViewAdapter extends ArrayAdapter<Child> {
    public listViewAdapter(Context context , ArrayList<Child> childList) {
        super(context, R.layout.activity_config, childList);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Child child = getItem(position);

        View itemView = convertView;
        if(itemView == null){
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.config_listview, parent, false);
        }

        if (child != null){
            //image
            ImageView imageView = itemView.findViewById(R.id.portrait);
            if(child.getPortrait() != null){
                imageView.setImageBitmap(child.getPortrait());
            }
            else{
                imageView.setImageResource(R.drawable.default_portrait);
            }

            //name
            TextView name = itemView.findViewById(R.id.name);
            name.setText("Name: " + child.getName());
            Log.d("Name", child.getName());
        }

        return itemView;
    }
}
