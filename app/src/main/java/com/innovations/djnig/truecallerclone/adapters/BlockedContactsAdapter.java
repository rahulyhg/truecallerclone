package com.innovations.djnig.truecallerclone.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.models.BlockedContact;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by dell on 17.01.2018.
 */

public class BlockedContactsAdapter extends RecyclerView.Adapter<BlockedContactsAdapter.MyViewHolder>{
    private final Context context;
    private List<BlockedContact> contactList;

    public interface OnItemClickListener {
        void onItemClick(BlockedContact item);
        void onInnerItemClick(View view, BlockedContact item);
    }

    private OnItemClickListener listener;

    public BlockedContactsAdapter(Context context, List<BlockedContact> contactList) {
        this.contactList = contactList;
        this.context = context;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public List<BlockedContact> getContactList() {
        return contactList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.select_contact_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        BlockedContact item = contactList.get(position);
        holder.contactName.setText(item.getDisplayName());
        if(!item.getPhoneNumbers().isEmpty()){
            holder.numberContact.setText(item.getPhoneNumbers().get(0));
        }
        Uri photo = null;
        try{
            if(item.getPhotoUri() != null)
                photo = Uri.parse(item.getPhotoUri());
        }catch (Exception e){
            e.printStackTrace();
        }

        if(photo == null){
            Picasso.with(context).load(R.drawable.profile_pictures_blocked).into(holder.contactImage);
        }else {
            Picasso.with(context).load(photo).into(holder.contactImage);
        }

        holder.bind(item, listener);

    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.select_contact_item)
        View root;

        @BindView(R.id.select_contact_name)
        TextView contactName;

        @BindView(R.id.select_number_contact)
        TextView numberContact;

        @BindView(R.id.select_profile_image_contact)
        CircleImageView contactImage;

        @BindView(R.id.unlock_button)
        ImageButton unlockButton;


        public MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(final BlockedContact item, final OnItemClickListener listener) {
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(item);
                }
            });

            unlockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onInnerItemClick(v, item);
                }
            });
        }
    }
}
