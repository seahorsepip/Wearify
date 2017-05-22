package com.seapip.thomas.wearify;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.seapip.thomas.wearify.Spotify.Context;
import com.squareup.picasso.Picasso;

import java.util.List;

public class BrowseItemAdapter extends RecyclerView.Adapter<BrowseItemAdapter.ViewHolder> {

    private android.content.Context mContext;
    private List<BrowseItem> mList;

    public BrowseItemAdapter(android.content.Context context, List<BrowseItem> list) {
        mContext = context;
        mList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.browser_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        BrowseItem item =  mList.get(position);
        viewHolder.title.setText(item.title);
        viewHolder.subTitle.setText(item.subTitle);
        if(item.image != null) {
            Picasso.with(mContext).load(item.image).fit().into(viewHolder.image);
            viewHolder.image.setVisibility(View.VISIBLE);
        } else {
            viewHolder.image.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title;
        private final TextView subTitle;
        private final ImageView image;

        public ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subTitle = (TextView) view.findViewById(R.id.sub_title);
            image = (ImageView) view.findViewById(R.id.image);
        }
    }

}