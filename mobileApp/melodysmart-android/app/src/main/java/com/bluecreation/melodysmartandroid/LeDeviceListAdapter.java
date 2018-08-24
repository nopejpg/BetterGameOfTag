package com.bluecreation.melodysmartandroid;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bluecreation.melodysmartandroid.extra.ScanRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by stanl on 21.10.16.
 */ // Adapter for holding devices found through scanning.
class LeDeviceListAdapter extends BaseAdapter {
    private final ArrayList<ScanResult> items = new ArrayList<>();
    private final LayoutInflater layoutInflater;

    private final static UUID melodySmartServiceUuid = UUID.fromString("bc2f4cc6-aaef-4351-9034-d66268e328f0");

    private boolean showOnlyMelodySmartDevices = false;

    public boolean isShowOnlyMelodySmartDevices() {
        return showOnlyMelodySmartDevices;
    }

    public void setShowOnlyMelodySmartDevices(boolean showOnlyMelodySmartDevices) {
        this.showOnlyMelodySmartDevices = showOnlyMelodySmartDevices;
    }

    static class ScanResult {
        BluetoothDevice device;
        ScanRecord scanRecord;
        boolean isMelodySmart;

        ScanResult(BluetoothDevice device, byte[] scanRecord) {
            this.device = device;
            this.scanRecord = ScanRecord.parseFromBytes(scanRecord);

            List<ParcelUuid> serviceUuids = this.scanRecord.getServiceUuids();
            this.isMelodySmart = serviceUuids != null && serviceUuids.contains(new ParcelUuid(melodySmartServiceUuid));
        }
    }

    LeDeviceListAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
    }

    void addDevice(ScanResult scanResult) {
        if (showOnlyMelodySmartDevices && !scanResult.isMelodySmart) return;

        for (ScanResult mLeResult : items) {
            if (mLeResult.device.equals(scanResult.device)) {
                mLeResult.device = scanResult.device;
                mLeResult.scanRecord = scanResult.scanRecord;
                notifyDataSetChanged();
                return;
            }
        }
        items.add(scanResult);
        notifyDataSetChanged();
    }

    BluetoothDevice getDevice(int index) {
        return items.get(index).device;
    }

    void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = layoutInflater.inflate(R.layout.listitem_device, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.scanRecord = (TextView) view.findViewById(R.id.scan_record);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = items.get(i).device;

        ScanRecord scanRecord = items.get(i).scanRecord;

        String manufacturerData = getManufacturerData(scanRecord);

        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText("Unknown device");
        }
        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.scanRecord.setText("data: " + manufacturerData);

        return view;
    }

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView scanRecord;
    }

    private static String getManufacturerData(ScanRecord record) {
        SparseArray<byte[]> data = record.getManufacturerSpecificData();

        if (data.size() == 0) return "";

        StringBuffer sb = new StringBuffer();
        for (byte b : data.valueAt(0)) {
            sb.append(String.format("%02x ", b));
        }

        return sb.toString();
    }

}
