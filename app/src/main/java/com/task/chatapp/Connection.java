package com.task.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

public class Connection implements ConnectionListener {

    private static final String TAG = "Connection";

    private final Context mApplicationContext;
    private final String mUsername;
    private final String mPassword;
    private final String mServiceName;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;


    public static enum ConnectionState {
        CONNECTED, AUTHENTICATED, CONNECTING, DISCONNECTING, DISCONNECTED;
    }

    public static enum LoggedInState {
        LOGGED_IN, LOGGED_OUT;
    }


    public Connection(Context context) {
        Log.d(TAG, "Connection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password", null);

        if (jid != null) {
            mUsername = "kader@mchat.miimfi.com";
            mServiceName = "http://mchat.miimfi.com";
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }


    public void connect() throws IOException, XMPPException, SmackException {
        Log.d(TAG, "Connecting to server " + mServiceName);

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("mchat.miimfi.com")
                .setHost(mServiceName)
                .setUsernameAndPassword(mUsername, mPassword)
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setResource("yaxim.019E5B5F")
                .setDebuggerEnabled(true)
                //   .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
//                .setUsernameAndPassword("baeldung", "baeldung")
//                .setXmppDomain("jabb3r.org")
//                .setHost("jabb3r.org")
                // .setUsernameAndPassword(mUsername, mPassword)
//                .setXmppDomain(mServiceName)

//                .setHostAddress(InetAddress.getByName(mServiceName))
               // .setHost(server)
//                .setUsernameAndPassword(mUsername, mPassword)
//                .setPort(5222)
//                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
//                .setXmppDomain(serviceName)
//                .setHostnameVerifier(verifier)
//                .setHostAddress(addr)
//                .setDebuggerEnabled(true)
                .build();

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(this);
            sc.init(null, new X509TrustManager[] { mtm }, new java.security.SecureRandom());
            conf.setCustomSSLContext(sc);
            conf.setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Username : " + mUsername);
        Log.i(TAG, "Password : " + mPassword);
        Log.i(TAG, "Server : " + mServiceName);


        //Set up the ui thread broadcast message receiver.
        // setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(conf);
        mConnection.addConnectionListener(this);
        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            //  mConnection.login("baeldung","baeldung");
          //  mConnection.login(mServiceName, mPassword);
            Log.d(TAG, " login() Called ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
//            @Override
//            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
//                ///ADDED
//                Log.d(TAG, "message.getBody() :" + message.getBody());
//                Log.d(TAG, "message.getFrom() :" + message.getFrom());
//
//                String from = message.getFrom().toString();
//
//                String contactJid = "";
//                if (from.contains("/")) {
//                    contactJid = from.split("/")[0];
//                    Log.d(TAG, "The real jid is :" + contactJid);
//                    Log.d(TAG, "The message is from :" + from);
//                } else {
//                    contactJid = from;
//                }
//
//                //Bundle up the intent and send the broadcast.
//                Intent intent = new Intent(ConnectionService.NEW_MESSAGE);
//                intent.setPackage(mApplicationContext.getPackageName());
//                intent.putExtra(ConnectionService.BUNDLE_FROM_JID, contactJid);
//                intent.putExtra(ConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());
//                mApplicationContext.sendBroadcast(intent);
//                Log.d(TAG, "Received message from :" + contactJid + " broadcast sent.");
//                ///ADDED
//
//            }
//        });


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }

//    private void setupUiThreadBroadCastMessageReceiver() {
//        uiThreadMessageReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                //Check if the Intents purpose is to send the message.
//                String action = intent.getAction();
//                if (action.equals(ConnectionService.SEND_MESSAGE)) {
//                    //Send the message.
//                    sendMessage(intent.getStringExtra(ConnectionService.BUNDLE_MESSAGE_BODY),
//                            intent.getStringExtra(ConnectionService.BUNDLE_TO));
//                }
//            }
//        };
//
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ConnectionService.SEND_MESSAGE);
//        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter);
//
//    }

//    private void sendMessage(String body, String toJid) {
//        Log.d(TAG, "Sending message to :" + toJid);
//
//        EntityBareJid jid = null;
//
//
//        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
//
//        try {
//            jid = JidCreate.entityBareFrom(toJid);
//        } catch (XmppStringprepException e) {
//            e.printStackTrace();
//        }
//        Chat chat = chatManager.chatWith(jid);
//        try {
//            Message message = new Message(jid, Message.Type.chat);
//            message.setBody(body);
//            chat.send(message);
//
//        } catch (SmackException.NotConnectedException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }


    public void disconnect() {
        Log.d(TAG, "Disconnecting from serser " + mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in", false).commit();


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
    public void connected(XMPPConnection connection) {
        ConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Connected Successfully");

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        ConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG, "Authenticated Successfully");
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

}
