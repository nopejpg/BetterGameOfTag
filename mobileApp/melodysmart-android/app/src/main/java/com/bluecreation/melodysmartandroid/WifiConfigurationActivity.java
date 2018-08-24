package com.bluecreation.melodysmartandroid;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmart.RemoteCommandsService;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiConfigurationActivity extends AppCompatActivity implements RemoteCommandsService.Listener {

    static final String TAG = WifiConfigurationActivity.class.getSimpleName();

    RemoteCommandsService remoteCommandsService;

    String ssid;
    String password;
    int errorCode;

    ConfigurationState state = ConfigurationState.CONFIGURATION_STATE_IDLE;

    StringBuilder buffer = new StringBuilder();

    FastItemAdapter fastAdapter = new FastItemAdapter();

    TextView statusLabel;

    private enum ConfigurationState {
        CONFIGURATION_STATE_IDLE,
        CONFIGURATION_STATE_SET_WIFI_MODE,
        CONFIGURATION_STATE_SCANNING,
        CONFIGURATION_STATE_CONFIGURE_WIFI,
        CONFIGURATION_STATE_CONNECTING,
        CONFIGURATION_STATE_CONNECTED,
        CONFIGURATION_STATE_SCAN_COMPLETE,
        CONFIGURATION_STATE_DISCONNECTED,
    }

    static class Ap {
        int rssi;
        int channel;
        String encryptionMode;
        String ssid;
        String bssid;

        Ap(int rssi, int channel, String encryptionMode, String ssid, String bssid) {
            this.rssi = rssi;
            this.channel = channel;
            this.encryptionMode = encryptionMode;
            this.ssid = ssid;
            this.bssid = bssid;
        }

        @Override
        public String toString() {
            return String.format(Locale.UK,
                    "Ap{rssi=%d, channel=%d, encryptionMode='%s', ssid='%s', bssid='%s'}",
                    rssi, channel, encryptionMode, ssid, bssid);
        }
    }

    static class ApItem extends AbstractItem<ApItem, ApItem.ViewHolder> {
        private final Ap ap;

        ApItem(Ap ap) {
            this.ap = ap;
        }

        //The unique ID for this type of item
        @Override
        public int getType() {
            return R.id.recyclerView;
        }

        //The layout to be used for this type of item
        @Override
        public int getLayoutRes() {
            return R.layout.ap_item;
        }

        //The logic to bind your data to the view
        @Override
        public void bindView(ViewHolder viewHolder, List<Object> payloads) {
            //call super so the selection is already handled for you
            super.bindView(viewHolder, payloads);

            //bind our data
            viewHolder.rssi.setText("" + ap.rssi);
            viewHolder.channel.setText("" + ap.channel);
            viewHolder.encryptionMode.setText(ap.encryptionMode);
            viewHolder.ssid.setText(ap.ssid);
            viewHolder.bssid.setText(ap.bssid);
            viewHolder.rssiImage.setImageResource(ap.rssi > -50 ? R.drawable.signal4 :
                                                  ap.rssi > -60 ? R.drawable.signal3 :
                                                  ap.rssi > -70 ? R.drawable.signal2 :
                                                  ap.rssi > -80 ? R.drawable.signal1 :
                                                                  R.drawable.signal0);
        }

        //reset the view here (this is an optional method, but recommended)
        @Override
        public void unbindView(ViewHolder holder) {
            super.unbindView(holder);
            holder.rssi.setText(null);
            holder.channel.setText(null);
            holder.encryptionMode.setText(null);
            holder.ssid.setText(null);
            holder.bssid.setText(null);
            holder.rssiImage.setImageDrawable(null);
        }

        //Init the viewHolder for this Item
        @Override
        public ViewHolder getViewHolder(View v) {
            return new ViewHolder(v);
        }

        //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView rssi;
            TextView channel;
            TextView encryptionMode;
            TextView ssid;
            TextView bssid;
            ImageView rssiImage;

            ViewHolder(View view) {
                super(view);
                this.rssi = view.findViewById(R.id.rssi);
                this.channel = view.findViewById(R.id.channel);
                this.encryptionMode = view.findViewById(R.id.encryptionMode);
                this.ssid = view.findViewById(R.id.ssid);
                this.bssid = view.findViewById(R.id.bssid);
                this.rssiImage = view.findViewById(R.id.rssiImage);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_configuration);

        statusLabel = findViewById(R.id.textViewStatus);

        remoteCommandsService = MelodySmartDevice.getInstance().getRemoteCommandsService();
        remoteCommandsService.registerListener(this);
        remoteCommandsService.enableNotifications(true);

        fastAdapter = new FastItemAdapter();
        fastAdapter.withOnClickListener(new FastAdapter.OnClickListener() {
            @Override
            public boolean onClick(View v, IAdapter adapter, IItem item, int position) {
                final Ap ap = ((ApItem)item).ap;
                final EditText passwordEditText = new EditText(WifiConfigurationActivity.this);

                new AlertDialog.Builder(WifiConfigurationActivity.this)
                        .setTitle(String.format("Enter passkey for '%s'", ap.ssid))
                        .setView(passwordEditText)
                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ssid = ap.ssid;
                                password = passwordEditText.getText().toString();

                                setState(ConfigurationState.CONFIGURATION_STATE_CONFIGURE_WIFI);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return false;
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fastAdapter);

        setState(ConfigurationState.CONFIGURATION_STATE_SET_WIFI_MODE);
    }

    @Override
    public void onDestroy() {
        remoteCommandsService.unregisterListener(this);
        remoteCommandsService.enableNotifications(false);
        super.onDestroy();
    }

    void showStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusLabel.setText(status);
            }
        });
    }

    void setState(ConfigurationState s) {
        state = s;
        buffer.delete(0, buffer.length());

        switch (s) {
            case CONFIGURATION_STATE_SET_WIFI_MODE:
                showStatus("Setting Wi-Fi mode...");
                remoteCommandsService.send("AT+SRWCFG=3");
                break;

            case CONFIGURATION_STATE_SCANNING:
                showStatus("Scanning...");
                remoteCommandsService.send("AT+SRWSTASCN");
                break;

            case CONFIGURATION_STATE_SCAN_COMPLETE:
                showStatus("Scan complete.");
                remoteCommandsService.send("AT+SRWSTACON?");
                break;

            case CONFIGURATION_STATE_CONFIGURE_WIFI:
                showStatus("Configuring Wi-Fi...");
                remoteCommandsService.send(String.format("AT+SRWSTACFG=\"%s\",\"%s\"", ssid, password));
                break;

            case CONFIGURATION_STATE_CONNECTING:
                showStatus("Connecting...");
                remoteCommandsService.send("AT+SRWSTACON=1");
                break;

            case CONFIGURATION_STATE_CONNECTED:
                showStatus(String.format("Connected to '%s'.", ssid));
                break;

            case CONFIGURATION_STATE_DISCONNECTED:
                if (errorCode == 0) {
                    showStatus("Not connected.");
                } else {
                    showStatus(String.format(Locale.UK, "Connection failed:\n%s.", wifiErrorCodeToString(errorCode)));
                }
                break;
        }
    }

    private String wifiErrorCodeToString(int errorCode) {
        switch (errorCode) {
            default:
            case 0: return "Not Connected";
            case 1: return "Internal failure";
            case 2: return "Authentication no longer valid";
            case 3: return "De-authenticated, because the sending Station is leaving";
            case 4: return "Disassociated due to inactivity";
            case 5: return "Disassociated, because the AP is unable to handle all currently associated STAs at the same time.";
            case 6: return "Packet received from a non-authenticated STA";
            case 7: return "Packet received from a non-associated STA";
            case 8: return "Disassociated, because the sending STA is leaving (or has left) BSS";
            case 9: return "STA requesting (re)association is not authenticated by the responding STA.";
            case 10: return "Disassociated, because the information in the Power Capability element is unacceptable.";
            case 11: return "Disassociated, because the information in the Supported Channels element is unacceptable.";
            case 13: return "Invalid element, i.e. an element whose content does not meet the specifications of the Standard in Clause 8.";
            case 14: return "Message integrity code (MIC) failure.";
            case 15: return "Four-way handshake times out.";
            case 16: return "Group-Key Handshake times out.";
            case 17: return "The element in the four-way handshake is different from the (Re-)Association Request/Probe and Response/Beacon frame.";
            case 18: return "Invalid group cipher.";
            case 19: return "Invalid pairwise cipher.";
            case 20: return "Invalid AKMP.";
            case 21: return "Unsupported RSNE version.";
            case 22: return "Invalid RSNE capabilities.";
            case 23: return "IEEE 802.1X. authentication failed.";
            case 24: return "Cipher suite rejected due to security policies.";
            case 200: return "STA lost N beacons continuously";
            case 201: return "STA failed to scan the target AP";
            case 202: return "STA Authentication failed (not because of timeout)";
            case 203: return "STA Association failed (not because of timeout or too many stations)";
            case 204: return "Handshake failed";
        }
    }

    @Override
    public void handleReply(byte[] bytes) {
        String reply = new String(bytes);

        Log.d(TAG, "Got reply: " + reply);
        buffer.append(reply);

        List<String> lines = new ArrayList<>(Arrays.asList(buffer.toString().split("\r\n")));
        if (!reply.endsWith("\r\n") && lines.size() > 0) {
            lines.remove(lines.size() - 1);
        }

        for (String line : lines) {
            if (line.equals("ERROR") || line.startsWith("+CME ERROR:")) {
                showStatus("Error occurred.");
                buffer.delete(0, buffer.length());
                return;
            }
        }

        Pattern disconnectPattern = Pattern.compile("\\+SRWSTASTATUS: 0(?:,(\\d+))?");
        Pattern connectPattern = Pattern.compile("\\+SRWSTASTATUS: 1,\"(.+)\",\"(.+)\",(\\d+),(\\d+)");

        // +SRWSTAIP: "192.168.100.141","255.255.255.0","192.168.100.1"
        Pattern ipPattern = Pattern.compile("\\+SRWSTAIP: \"(.+)\",\"(.+)\",\"(.+)\"");

        for (String line : lines) {
            Matcher connectionMatcher = connectPattern.matcher(line);
            Matcher disconnectionMatcher = disconnectPattern.matcher(line);
            Matcher ipMatcher = ipPattern.matcher(line);

            if (connectionMatcher.matches()) {
                ssid = connectionMatcher.group(1);
                setState(ConfigurationState.CONFIGURATION_STATE_CONNECTED);
            } else if (disconnectionMatcher.matches()) {
                String errorString = disconnectionMatcher.group(1);
                errorCode = errorString == null ? 0 : Integer.parseInt(errorString);
                Log.d(TAG, "errorString: " + errorString + ", " + "errorCode: " + errorCode);
                showStatus(String.format(Locale.UK, "Connection failed:\n%s.", wifiErrorCodeToString(errorCode)));
            } else if (ipMatcher.matches()) {
                showStatus(String.format("Connected to '%s'.\nip: %s\nnetmask: %s\ngateway: %s", ssid,
                        ipMatcher.group(1), ipMatcher.group(2), ipMatcher.group(3)));
            }
        }

        switch (state) {
            case CONFIGURATION_STATE_IDLE:
                break;

            case CONFIGURATION_STATE_SET_WIFI_MODE:
                if (lines.contains("OK")) {
                    setState(ConfigurationState.CONFIGURATION_STATE_SCANNING);
                }
                break;

            case CONFIGURATION_STATE_SCANNING:
                if (lines.contains("OK")) {
                    // +SRWSTASCN: -97,4,6,"Prism","00:1d:aa:d9:b8:08"
                    Pattern apPattern = Pattern.compile("\\+SRWSTASCN: (-\\d+),(\\d+),(\\d+),\"(.+)\",\"(.+)\"");

                    for (String line : lines) {
                        Matcher matcher = apPattern.matcher(line);
                        if (matcher.matches()) {
                            int rssi = Integer.parseInt(matcher.group(1));
                            int authMode = Integer.parseInt(matcher.group(2));
                            int channel = Integer.parseInt(matcher.group(3));
                            String ssid = matcher.group(4);
                            String bssid = matcher.group(5);

                            String authModeString = (authMode == 0) ? "Open" :
                                                    (authMode == 1) ? "WEP" :
                                                    (authMode == 2) ? "WPA" :
                                                    (authMode == 3) ? "WPA2" :
                                                    (authMode == 4) ? "WPA/WPA2" :
                                                                      "WPA2 Enterprise";

                            final Ap ap = new Ap(rssi, channel, authModeString, ssid, bssid);
                            Log.d(TAG, "Got new AP: " + ap);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fastAdapter.add(new ApItem(ap));
                                }
                            });
                        }
                    }

                    setState(ConfigurationState.CONFIGURATION_STATE_SCAN_COMPLETE);
                }
                break;

            case CONFIGURATION_STATE_CONFIGURE_WIFI:
                if (lines.contains("OK")) {
                    setState(ConfigurationState.CONFIGURATION_STATE_CONNECTING);
                }
                break;

            case CONFIGURATION_STATE_CONNECTING:
                break;

            case CONFIGURATION_STATE_CONNECTED:
                break;
        }
    }

    @Override
    public void onNotificationsEnabled(boolean b) {

    }
}
