package com.innovations.djnig.truecallerclone.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.models.ContactModel;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by djnig on 1/18/2018.
 */

public class SearchViewAdapter extends SuggestionsAdapter<ContactModel, SearchViewAdapter.SeachViewHolder>{

    private OnItemClickListener listener;

    public interface OnItemClickListener{
        void onItemClick(ContactModel contact);
    }

    public SearchViewAdapter(LayoutInflater inflater) {
        super(inflater);

    }

    @Override
    public void onBindSuggestionHolder(ContactModel suggestion, SeachViewHolder holder, int position) {
        try {
            holder.mContactName.setText(suggestion.getContactName());
            holder.mContactNumber.setText(suggestion.getContactNumber());
        }catch (Exception e){
            e.printStackTrace();
        }

        holder.root.setOnClickListener(v -> {
            if(listener != null){
                listener.onItemClick(suggestion);
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener l){
        this.listener = l;
    }


    @Override
    public int getSingleViewHeight() {
        return 60;
    }

    @Override
    public SeachViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = getLayoutInflater().inflate(R.layout.item_search, parent, false);
        return new SeachViewHolder(v);
    }

    public class SeachViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.search_root)
        ViewGroup root;

        @BindView(R.id.search_contact_name)
        TextView mContactName;

        @BindView(R.id.search_contact_number)
        TextView mContactNumber;


        public SeachViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
