package com.lmgy.redirect.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.lmgy.redirect.R;
import com.lmgy.redirect.bean.HostData;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by lmgy on 15/8/2019
 */
public class HostSettingAdapter extends RecyclerView.Adapter<HostSettingAdapter.HostSettingViewHolder> {
    private Context mContext;
    private List<HostData> mHostDataList;
    private List<String> mSelectedIds = new ArrayList<>();

    public HostSettingAdapter(Context context, List<HostData> hostDataList) {
        this.mContext = context;
        this.mHostDataList = hostDataList;
    }

    @Override
    public HostSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_list, parent, false);
        return new HostSettingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HostSettingViewHolder holder, int position) {
        String text = mHostDataList.get(position).getIpAddress()
                + "   " + mHostDataList.get(position).getHostName()
                + "   " + mHostDataList.get(position).getRemark();
        if (!mHostDataList.get(position).getType())
            text = "#" + text;
        holder.title.setText(text);
        if (mSelectedIds.contains(String.valueOf(position))) {
            holder.rootView.setForeground(new ColorDrawable(ContextCompat.getColor(mContext, R.color.colorControlActivated)));
        } else {
            holder.rootView.setForeground(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.transparent)));
        }
    }

    @Override
    public int getItemCount() {
        return mHostDataList.size();
    }

    public HostData getItem(int position) {
        return mHostDataList.get(position);
    }

    public void setSelectedIds(List<String> selectedIds) {
        this.mSelectedIds = selectedIds;
        notifyDataSetChanged();
    }

    class HostSettingViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        FrameLayout rootView;

        HostSettingViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            rootView = itemView.findViewById(R.id.root_view);
        }
    }
}
