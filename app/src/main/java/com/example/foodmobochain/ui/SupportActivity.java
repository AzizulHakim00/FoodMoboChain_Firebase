package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.SupportTicket;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SupportActivity extends BaseScreenActivity {
    private LinearLayout tickets;
    private AppUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Help & support", "Track questions, complaints and account issues", true);
        firebase.loadCurrentUser(value -> {
            user = value;
            build();
            listenTickets();
        });
    }

    private void build() {
        content.removeAllViews();
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "SUPPORT CENTRE"));
        intro.addView(Ui.heading(this, "Get help without losing the conversation."));
        intro.addView(Ui.body(this,
                "Create a ticket for order, vendor, rental, account or safety issues. Administrators can reply and update its status."));
        Button create = Ui.button(this, "Create support ticket");
        create.setOnClickListener(v -> showCreateDialog());
        intro.addView(create);
        content.addView(intro);
        content.addView(Ui.heading(this, firebase.isAdminUser() ? "All support tickets" : "My support tickets"));
        tickets = new LinearLayout(this);
        tickets.setOrientation(LinearLayout.VERTICAL);
        content.addView(tickets);
    }

    private void listenTickets() {
        String uid = firebase.uid();
        if (uid == null || tickets == null) return;
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SupportTicket> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SupportTicket ticket = child.getValue(SupportTicket.class);
                    if (ticket == null) continue;
                    if (ticket.id == null) ticket.id = child.getKey();
                    values.add(ticket);
                }
                values.sort(Comparator.comparingLong((SupportTicket value) -> value.updatedAt).reversed());
                render(values);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(SupportActivity.this, error.getMessage());
            }
        };
        if (firebase.isAdminUser()) firebase.supportTickets().addValueEventListener(listener);
        else firebase.supportTickets().orderByChild("userId").equalTo(uid).addValueEventListener(listener);
    }

    private void render(List<SupportTicket> values) {
        tickets.removeAllViews();
        for (SupportTicket ticket : values) tickets.addView(ticketCard(ticket));
        if (values.isEmpty()) tickets.addView(Ui.body(this, "No support tickets yet."));
    }

    private LinearLayout ticketCard(SupportTicket ticket) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, safe(ticket.status, "open").toUpperCase()
                + "  •  " + safe(ticket.category, "General")));
        card.addView(Ui.title(this, ticket.subject));
        card.addView(Ui.body(this, ticket.message));
        card.addView(Ui.body(this, "Created " + DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(ticket.createdAt))
                + (TextUtils.isEmpty(ticket.userName) ? "" : "\nFrom: " + ticket.userName)));
        if (!TextUtils.isEmpty(ticket.adminReply)) {
            LinearLayout reply = Ui.softCard(this);
            reply.addView(Ui.label(this, "ADMIN REPLY"));
            reply.addView(Ui.body(this, ticket.adminReply));
            card.addView(reply);
        }
        if (firebase.isAdminUser()) {
            Button reply = Ui.button(this, "Reply and update status");
            reply.setOnClickListener(v -> showReplyDialog(ticket));
            card.addView(reply);
        }
        return card;
    }

    private void showCreateDialog() {
        if (user == null || firebase.uid() == null) return;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Order", "Vendor", "Rental", "Account", "Safety", "Other")));
        EditText subject = Ui.input(this, "Subject");
        EditText message = Ui.input(this, "Describe the issue clearly");
        message.setSingleLine(false);
        message.setMinLines(4);
        form.addView(category);
        form.addView(subject);
        form.addView(message);
        new MaterialAlertDialogBuilder(this)
                .setTitle("New support ticket")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", (dialog, which) -> createTicket(
                        String.valueOf(category.getSelectedItem()),
                        subject.getText().toString().trim(), message.getText().toString().trim()))
                .show();
    }

    private void createTicket(String category, String subject, String message) {
        String uid = firebase.uid();
        if (uid == null || user == null) return;
        if (subject.length() < 3 || message.length() < 10) {
            Ui.toast(this, "Add a clear subject and at least 10 characters of detail.");
            return;
        }
        String id = firebase.supportTickets().push().getKey();
        if (id == null) return;
        SupportTicket ticket = new SupportTicket();
        ticket.id = id;
        ticket.userId = uid;
        ticket.userName = user.name;
        ticket.category = category;
        ticket.subject = subject;
        ticket.message = message;
        ticket.status = "open";
        ticket.adminReply = "";
        ticket.createdAt = System.currentTimeMillis();
        ticket.updatedAt = ticket.createdAt;
        firebase.supportTickets().child(id).setValue(ticket).addOnCompleteListener(task ->
                Ui.toast(this, task.isSuccessful() ? "Support ticket created." : "Could not create ticket."));
    }

    private void showReplyDialog(SupportTicket ticket) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        Spinner status = new Spinner(this);
        status.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("open", "in_progress", "resolved", "closed")));
        EditText reply = Ui.input(this, "Administrator reply");
        reply.setSingleLine(false);
        reply.setMinLines(3);
        reply.setText(ticket.adminReply);
        form.addView(status);
        form.addView(reply);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update support ticket")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("status", String.valueOf(status.getSelectedItem()));
                    update.put("adminReply", reply.getText().toString().trim());
                    update.put("updatedAt", System.currentTimeMillis());
                    firebase.supportTickets().child(ticket.id).updateChildren(update);
                }).show();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
