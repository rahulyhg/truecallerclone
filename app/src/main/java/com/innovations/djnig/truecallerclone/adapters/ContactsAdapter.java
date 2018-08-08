package com.innovations.djnig.truecallerclone.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.tamir7.contacts.Contact;
import com.innovations.djnig.truecallerclone.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by djnig on 12/1/2017.
 */

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.MyViewHolder> {

    private final Context context;
    private List<Contact> contactList;

    public interface OnItemClickListener {
        void onItemClick(Contact item);
        void onInnerItemClick(View view, Contact item);
    }

    private OnItemClickListener listener;

    public ContactsAdapter(Context context, List<Contact> contactList) {
        this.contactList = contactList;
        this.context = context;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Contact item = contactList.get(position);
        holder.contactName.setText(item.getDisplayName());
        if(!item.getPhoneNumbers().isEmpty()){
            holder.numberContact.setText(item.getPhoneNumbers().get(0).getNumber());
        }
        Uri photo = null;
        try{
            if(item.getPhotoUri() != null)
                photo = Uri.parse(item.getPhotoUri());
        }catch (Exception e){
            e.printStackTrace();
        }

        if(photo == null){
            Picasso.with(context).load(R.drawable.profile_pictures).into(holder.contactImage);
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

        @BindView(R.id.contact_item)
        View root;

        @BindView(R.id.contact_name)
        TextView contactName;

        @BindView(R.id.number_contact)
        TextView numberContact;

        @BindView(R.id.profile_image_contact)
        CircleImageView contactImage;

        @BindView(R.id.sms_button)
        ImageButton smsButton;

        @BindView(R.id.call_button)
        ImageButton callButton;

        public MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(final Contact item, final OnItemClickListener listener) {
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(item);
                }
            });

            smsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onInnerItemClick(v, item);
                }
            });

            callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onInnerItemClick(v, item);
                }
            });
        }
    }
    public List<Contact> getContactList() {
        return contactList;
    }
}
