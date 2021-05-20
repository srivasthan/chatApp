package com.task.chatapp.xmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class Connection implements ConnectionListener {

    private static final String TAG = "Connection";
    private final Context mApplicationContext;
    private final String mUsername;
    private final String mPassword;
    private final String mServiceName;
    private AbstractXMPPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;

    public enum ConnectionState {
        CONNECTED, CONNECTING, DISCONNECTED
    }

    public enum LoggedInState {
        LOGGED_OUT
    }

    public Connection(Context context) {
        Log.d(TAG, "Connection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password", null);

        if (jid != null) {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }


    public void connect() throws IOException, XMPPException, SmackException {
        Log.d(TAG, "Connecting to server " + mServiceName);

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        MemorizingTrustManager mtm = new MemorizingTrustManager(mApplicationContext);
        try {
            if (sc != null) {
                sc.init(null, new X509TrustManager[]{mtm}, new SecureRandom());
            }
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                .setCustomSSLContext(sc)
                .setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                .setXmppDomain(mServiceName)
                .setResource("Chat")
                .build();

        Log.i(TAG, "Username : " + mUsername);
        Log.i(TAG, "Password : " + mPassword);
        Log.i(TAG, "Server : " + mServiceName);

        SmackConfiguration.DEBUG = true;
        mConnection = new XMPPTCPConnection(conf);
        mConnection.addConnectionListener(this);
        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername, mPassword);
            Log.d(TAG, " login() Called ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                ///ADDED
                Log.d(TAG, "message.getBody() :" + message.getBody());
                Log.d(TAG, "message.getFrom() :" + message.getFrom());

                String from = message.getFrom().toString();

                String contactJid = "";
                if (from.contains("/")) {
                    contactJid = from.split("/")[0];
                    Log.d(TAG, "The real jid is :" + contactJid);
                    Log.d(TAG, "The message is from :" + from);
                } else {
                    contactJid = from;
                }

                //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(ConnectionService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(ConnectionService.BUNDLE_FROM_JID, contactJid);
                intent.putExtra(ConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());
                mApplicationContext.sendBroadcast(intent);
                Log.d(TAG, "Received message from :" + contactJid + " broadcast sent.");
                ///ADDED

            }
        });


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        ReconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }


    public void disconnect() {
        Log.d(TAG, "Disconnecting from server " + mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in", false).apply();


        if (mConnection != null) {
            mConnection.disconnect();
        }

        mConnection = null;
        // Unregister the message broadcast receiver.
        if (uiThreadMessageReceiver != null) {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }

    @Override
    public void connected(org.jivesoftware.smack.XMPPConnection connection) {
        ConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Connected Successfully");

    }

    @Override
    public void authenticated(org.jivesoftware.smack.XMPPConnection connection, boolean resumed) {
        ConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Authenticated Successfully");
        showContactListActivityWhenAuthenticated();
    }


    @Override
    public void connectionClosed() {
        ConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG, "Connectionclosed()");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        ConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG, "ConnectionClosedOnError, error " + e.toString());
    }

    @Override
    public void reconnectingIn(int seconds) {
        ConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG, "ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        ConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        ConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG, "ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated() {
        Intent i = new Intent(ConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG, "Sent the broadcast that we are authenticated");
    }

}
