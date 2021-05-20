package com.task.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        List<Contact> contacts = new ArrayList<>();

        Log.d(TAG, "Connection Constructor called.");
        Context mApplicationContext = this;
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid", null);

        String mUsername;
        if (jid != null) {
            mUsername = jid;
        } else {
            mUsername = "";
        }

        contacts.add(new Contact(mUsername));

        ContactAdapter mAdapter = new ContactAdapter(contacts);
        contactsRecyclerView.setAdapter(mAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            //Disconnect from server
            Log.d(TAG, "Initiating the log out process");
            Intent i1 = new Intent(this, ConnectionService.class);
            stopService(i1);

            //Finish this activity
            finish();

            //Start login activity for user to login
            Intent loginIntent = new Intent(this, MainActivity.class);
            startActivity(loginIntent);

        }

        return super.onOptionsItemSelected(item);
    }

    private static class ContactHolder extends RecyclerView.ViewHolder {
        private final TextView contactTextView;

        public ContactHolder(View itemView) {
            super(itemView);

            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);

        }


        public void bindContact(Contact contact) {
            if (contact == null) {
                Log.d(TAG, "Trying to work on a null Contact object ,returning.");
                return;
            }
            contactTextView.setText(contact.getJid());

        }
    }


    private static class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {
        private final List<Contact> mContacts;

        public ContactAdapter(List<Contact> contactList) {
            mContacts = contactList;
        }

        @NonNull
        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater
                    .inflate(R.layout.list_item_contact, parent,
                            false);
            return new ContactHolder(view);
        }

        @Override
        public void onBindViewHolder(ContactHolder holder, int position) {
            Contact contact = mContacts.get(position);
            holder.bindContact(contact);

        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }
    }

}