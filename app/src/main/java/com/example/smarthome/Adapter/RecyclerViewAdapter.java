package com.example.smarthome.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.Model.DeviceCardModel;
import com.example.smarthome.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    Context context;
    ArrayList<DeviceCardModel> list;
    //构造方法传入上下文和数据
    public RecyclerViewAdapter(Context context, ArrayList<DeviceCardModel> list) {
        this.context=context;
        this.list=list;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //创建视图
        View view= LayoutInflater.from(this.context).inflate(R.layout.item_device_card,parent,false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MyViewHolder holder, int position) {
        holder.iv_icon.setImageResource(list.get(position).getIconId());
        holder.tv_title.setText(list.get(position).getTitle());
        holder.tv_subtitle.setText(list.get(position).getSubtitle());

    }

    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public void updateItem(int position, String newSubtitle) {
        if (position >= 0 && position < list.size()) {
            DeviceCardModel item = list.get(position);
            list.set(position, new DeviceCardModel(newSubtitle, item.getTitle(), item.getIconId()));
            notifyItemChanged(position);
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView iv_icon;
        TextView tv_title;
        TextView tv_subtitle;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_icon=itemView.findViewById(R.id.iv_icon);
            tv_title=itemView.findViewById(R.id.tv_title);
            tv_subtitle=itemView.findViewById(R.id.tv_subtitle);

        }

    }
}
