package com.alissa.skitracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;

public class FriendAdapter extends ArrayAdapter<Friend> {

    Context c;

    public FriendAdapter(Activity context, ArrayList<Friend> friends){
        super(context, 0, friends);
        this.c = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }


        Friend currentFriend = getItem(position);

        TextView tv_name = (TextView) listItemView.findViewById(R.id.tv_name);

        tv_name.setText(currentFriend.getName());

        Button b_join = (Button) listItemView.findViewById(R.id.b_join);

        if(currentFriend.getOnline() == true){
            if(currentFriend.getcanBeJoined() == true){
                b_join.setText("JOIN");
            } else{
                b_join.setText("ONLINE");
            }
            //b_join.setBackgroundColor(Color.GREEN);
            //b_join.setBackgroundResource(R.color.online);
            b_join.setBackgroundTintList(ContextCompat.getColorStateList(this.c,R.color.online));
        } else{
            b_join.setText("OFFLINE");
            //b_join.setBackgroundColor(Color.RED);
            //b_join.setBackgroundResource(R.color.offline);
            b_join.setBackgroundTintList(ContextCompat.getColorStateList(this.c,R.color.offline));
            // ViewCompat.setBackgroundTintList(b_join, ContextCompat.getColorStateList(this, android.R.color.white));
        }

        return listItemView;
    }

}
