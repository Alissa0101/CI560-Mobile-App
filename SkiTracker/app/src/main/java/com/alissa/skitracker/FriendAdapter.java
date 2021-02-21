package com.alissa.skitracker;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class FriendAdapter extends ArrayAdapter<Friend> {


    public FriendAdapter(Activity context, ArrayList<Friend> friends){
        super(context, 0, friends);
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
            b_join.setBackgroundColor(Color.GREEN);
        } else{
            b_join.setText("OFFLINE");
            b_join.setBackgroundColor(Color.RED);
        }

        return listItemView;
    }

}
