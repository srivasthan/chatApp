package com.task.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import okhttp3.OkHttpClient;

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
            mServiceName = "http://mchat.miimfi.com/";
        } else {
            mUsername = "";
            mServiceName = "";
        }
    }


    public void connect() throws IOException, XMPPException, SmackException {
        Log.d(TAG, "Connecting to server " + mServiceName);

//        TrustManagerFactory tmf = null;
//        try {
//            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            try {
//                tmf.init((KeyStore) null);
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            }
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//
//        TrustManager[] trustManagers = tmf.getTrustManagers();
//        final X509TrustManager origTrustmanager = (X509TrustManager) trustManagers[0];
//
//        TrustManager[] wrappedTrustManagers = new TrustManager[]{
//                new X509TrustManager() {
//                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                        return origTrustmanager.getAcceptedIssuers();
//                    }
//
//                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                        try {
//                            origTrustmanager.checkClientTrusted(certs, authType);
//                        } catch (CertificateException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                        try {
//                            origTrustmanager.checkServerTrusted(certs, authType);
//                        } catch (CertificateException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//        };

//        SSLContext sc = null;
//        try {
//            sc = SSLContext.getInstance("TLS");
//            try {
//                sc.init(null, wrappedTrustManagers, null);
//                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//            } catch (KeyManagementException e) {
//                e.printStackTrace();
//            }
//
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }

        //InetAddress addr = InetAddress.getByName("13.126.210.112");
        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return false;
            }
        };
        // DomainBareJid serviceName = JidCreate.domainBareFrom(mServiceName);

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        MemorizingTrustManager mtm = new MemorizingTrustManager(mApplicationContext);
        try {
            assert sc != null;
            sc.init(null, new X509TrustManager[]{mtm}, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        DomainBareJid serviceName = JidCreate.domainBareFrom(mServiceName);

        XMPPTCPConnectionConfiguration builder = XMPPTCPConnectionConfiguration.builder()
                .setPort(5222)
                .setHost(mServiceName)
                .setCompressionEnabled(false)
                .setDebuggerEnabled(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setSendPresence(true)
                .setKeystoreType("AndroidCAStore")
                .setKeystorePath(null)
                .setServiceName(serviceName)
                .setCustomSSLContext(sc)
                .build();

//        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
//                .setHost(mServiceName)
//                .setXmppDomain(mServiceName)
//                .setHostAddress(InetAddress.getByName("192.168.0.103"))
//                .setCustomSSLContext(sc)
//                .setHostnameVerifier(mtm.wrapHostnameVerifier(new StrictHostnameVerifier()))
//                .build();

        Log.i(TAG, "Username : " + mUsername);
        Log.i(TAG, "Password : " + mPassword);
        Log.i(TAG, "Server : " + mServiceName);

        SmackConfiguration.DEBUG = true;
        mConnection = new XMPPTCPConnection(builder);
        mConnection.addConnectionListener(this);
        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername, mPassword);
            if (mConnection.isConnected()) {
                Log.d("XMPP", "Connected");
            }
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
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }

    private void setupUiThreadBroadCastMessageReceiver() {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if (action.equals(ConnectionService.SEND_MESSAGE)) {
                    //Send the message.
                    sendMessage(intent.getStringExtra(ConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(ConnectionService.BUNDLE_TO));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter);

    }

    private void sendMessage(String body, String toJid) {
        Log.d(TAG, "Sending message to :" + toJid);

        EntityBareJid jid = null;


        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);

        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            chat.send(message);

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        Log.d(TAG, "Disconnecting from server " + mServiceName);

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

//    private static OkHttpClient getUnsafeOkHttpClient() {
//        try {
//            // Create a trust manager that does not validate certificate chains
//            final TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        @Override
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
//                        }
//
//                        @Override
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
//                        }
//
//                        @Override
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                            return new java.security.cert.X509Certificate[]{};
//                        }
//                    }
//            };
//
//            // Install the all-trusting trust manager
//            final SSLContext sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
//            // Create an ssl socket factory with our all-trusting manager
//            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
//
//            OkHttpClient.Builder builder = new OkHttpClient.Builder();
//            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
//            builder.hostnameVerifier(new HostnameVerifier() {
//                @Override
//                public boolean verify(String hostname, SSLSession session) {
//                    return true;
//                }
//            });
//
//            OkHttpClient okHttpClient = builder.build();
//            return okHttpClient;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }


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
