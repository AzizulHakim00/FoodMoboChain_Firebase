package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.NewsPost;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsfeedActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private EditText postInput;
    private Button share;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Community newsfeed", "Promotions, announcements and business tips", true);
        firebase.loadCurrentUser(user -> currentUser = user);
        buildComposer();
        listenToPosts();
    }

    private void buildComposer() {
        LinearLayout composer = Ui.softCard(this);
        composer.addView(Ui.label(this, "SHARE WITH THE COMMUNITY"));
        postInput = Ui.input(this, "Write an update, promotion or useful tip…");
        postInput.setSingleLine(false);
        postInput.setMinLines(3);
        share = Ui.button(this, "Share post");
        share.setOnClickListener(v -> sharePost());
        composer.addView(postInput);
        composer.addView(share);
        content.addView(composer);
        content.addView(Ui.spacer(this, 14));
        content.addView(Ui.heading(this, "Latest posts"));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
    }

    private void sharePost() {
        String text = postInput.getText().toString().trim();
        String uid = firebase.uid();
        if (TextUtils.isEmpty(text) || uid == null || currentUser == null) {
            Ui.toast(this, "Write something before sharing.");
            return;
        }
        String id = firebase.newsfeed().push().getKey();
        if (id == null) return;
        NewsPost post = new NewsPost();
        post.id = id;
        post.authorId = uid;
        post.authorName = currentUser.name;
        post.authorRole = currentUser.role;
        post.content = text;
        post.createdAt = System.currentTimeMillis();
        share.setEnabled(false);
        firebase.newsfeed().child(id).setValue(post).addOnCompleteListener(task -> {
            share.setEnabled(true);
            Ui.toast(this, task.isSuccessful() ? "Post uploaded successfully." : "Could not share the post.");
            if (task.isSuccessful()) postInput.setText("");
        });
    }

    private void listenToPosts() {
        firebase.newsfeed().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NewsPost> posts = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NewsPost post = child.getValue(NewsPost.class);
                    if (post != null) posts.add(post);
                }
                posts.sort(Comparator.comparingLong((NewsPost post) -> post.createdAt).reversed());
                list.removeAllViews();
                for (NewsPost post : posts) list.addView(postCard(post));
                if (posts.isEmpty()) list.addView(Ui.body(NewsfeedActivity.this, "The newsfeed is ready for its first post."));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(NewsfeedActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout postCard(NewsPost post) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, post.authorRole == null ? "MEMBER" : post.authorRole));
        card.addView(Ui.title(this, post.authorName));
        card.addView(Ui.body(this, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(post.createdAt))));
        card.addView(Ui.spacer(this, 8));
        card.addView(Ui.body(this, post.content));
        Button flag = Ui.outlineButton(this, "Report this post");
        flag.setOnClickListener(v -> askFlagReason(post));
        card.addView(flag);
        return card;
    }

    private void askFlagReason(NewsPost post) {
        EditText reason = Ui.input(this, "Why are you reporting this post?");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Report content")
                .setView(reason)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Report", (dialog, which) -> flag(post, reason.getText().toString().trim()))
                .show();
    }

    private void flag(NewsPost post, String reason) {
        String uid = firebase.uid();
        if (uid == null || post.id == null || TextUtils.isEmpty(reason)) {
            Ui.toast(this, "Please include a reason.");
            return;
        }
        String id = firebase.flags().push().getKey();
        if (id == null) return;
        Map<String, Object> flag = new HashMap<>();
        flag.put("id", id);
        flag.put("contentType", "newsPost");
        flag.put("contentId", post.id);
        flag.put("contentPreview", post.content);
        flag.put("reportedBy", uid);
        flag.put("reason", reason);
        flag.put("status", "open");
        flag.put("createdAt", System.currentTimeMillis());
        firebase.flags().child(id).setValue(flag).addOnCompleteListener(task ->
                Ui.toast(this, task.isSuccessful() ? "Report sent to the admin." : "Could not send the report."));
    }
}
