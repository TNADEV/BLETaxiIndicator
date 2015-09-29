package com.tna6011.bleindicator.adpater;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tna6011.bleindicator.R;

import java.util.List;

public class DeviceListAdapter extends BaseAdapter {
    Context context;
    List<BluetoothDevice> list;
    int itemLayout;

    DeviceListAdapterCallback callback;

    public interface DeviceListAdapterCallback {
        public void onDeviceSelected(String deviceAddress);
    }

    public void setCallback(DeviceListAdapterCallback callback) {
        this.callback = callback;
    }

    public DeviceListAdapter(Context context, int itemLayout, List<BluetoothDevice> list) {
        this.context = context;
        this.itemLayout = itemLayout;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        public TextView nameView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if (convertView == null) {
            holder = new ViewHolder();

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(itemLayout, parent, false);

            holder.nameView = (TextView) convertView
                    .findViewById(R.id.label_device_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.nameView.setText(list.get(position).getName());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onDeviceSelected(list.get(position).getAddress());
                }
            }
        });
        return convertView;
    }

}
