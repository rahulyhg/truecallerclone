package com.innovations.djnig.truecallerclone.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.ContactsUtils;
import com.innovations.djnig.truecallerclone.models.CallLog;
import com.squareup.picasso.Picasso;
import com.wickerlabs.logmanager.LogsManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.MyViewHolder> {

    private Context context;
    private OnItemClickListener listener;
    private OnItemLongClickListener onItemLongClickListener;
    private List<CallLog> callsList;

    public void setItems(List<CallLog> callsList) {
            this.callsList.clear();
            this.callsList.addAll(callsList);
            this.notifyDataSetChanged();
    }

    public void clearItems(){
        callsList.clear();
    }

    public void addItem(CallLog log) {
        callsList.add(log);
    }

    public interface OnItemLongClickListener {
        void onItemLongClickListener(View view, CallLog item);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, CallLog item);
        void onInnerItemClick(View view, CallLog item);
    }

    public LogsAdapter(Context context) {
        this.callsList = new ArrayList<>();
        this.context = context;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.call_log_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.bind(callsList.get(position), listener);
        if(callsList.get(position).isSelected())
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        else
            holder.itemView.setBackgroundColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return callsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.contact_name_call)
        TextView callerName;

        @BindView(R.id.profile_image)
        CircleImageView profileImage;

        @BindView(R.id.duration_call)
        TextView durationOfcall;

        @BindView(R.id.call_type)
        ImageView callType;

        @BindView(R.id.call_time)
        TextView callTime;

        @BindView(R.id.call_info)
        ImageView callInfo;

        @BindView(R.id.sms_button_calls)
        ImageView smsButtonCalls;

        @BindView(R.id.call_log_item)
        View root;


        public MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(final CallLog item, final OnItemClickListener listener) {
            profileImage.setImageBitmap(ContactsUtils.retrieveContactPhoto(context, item.getNumber()));

            try{
                callerName.setText(item.getName());

                //searching unknown numbers
                if (item.getName().equals(item.getNumber())) {
                    ContactNumberUtil.searchNameForUnknownNumber(item.getNumber(), callerName);
                }
            }catch (Exception e){
                callerName.setText("Private Number");
            }

            callTime.setText(item.getTime());

            durationOfcall.setText(item.getDuration());

            switch (item.getType()) {

                case LogsManager.INCOMING:
                    Picasso.with(context).load(R.drawable.ic_incoming_call).into(callType);
                    break;
                case LogsManager.OUTGOING:
                    Picasso.with(context).load(R.drawable.ic_outgoing_call).into(callType);
                    break;
                case LogsManager.MISSED:
                    Picasso.with(context).load(R.drawable.ic_missed_call).into(callType);
                    break;
                default:
                    Picasso.with(context).load(R.drawable.ic_cancelled_call).into(callType);
                    break;
            }

            callInfo.setOnClickListener(v -> {
                if(listener != null)
                    listener.onInnerItemClick(v, item);
            });

            smsButtonCalls.setOnClickListener(v -> {
                if(listener != null)
                    listener.onInnerItemClick(v, item);
            });

            root.setOnClickListener(v -> {
                if(listener != null)
                    listener.onItemClick(v, item);
            });

            root.setOnLongClickListener( v -> {
                if(onItemLongClickListener != null)
                    onItemLongClickListener.onItemLongClickListener(v, item);
                return true;
            });
        }
    }
}