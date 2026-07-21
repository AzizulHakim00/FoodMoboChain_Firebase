package com.example.foodmobochain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.TrainingResource;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TrainingActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private EditText title;
    private EditText topic;
    private EditText description;
    private EditText videoUrl;
    private Button publish;
    private LinearLayout list;
    private EditText search;
    private final List<TrainingResource> allResources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Free training", "Food safety, service and entrepreneurship", true);
        firebase.loadCurrentUser(user -> currentUser = user);
        buildPublishForm();
        listenToTraining();
    }

    private void buildPublishForm() {
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "LEARN WITHOUT COST"));
        intro.addView(Ui.heading(this, "Skills for a stronger food business."));
        intro.addView(Ui.body(this,
                "Verified users can share a public YouTube or HTTPS tutorial link. Content can be flagged and moderated."));
        content.addView(intro);

        LinearLayout form = Ui.card(this);
        form.addView(Ui.label(this, "SHARE A TUTORIAL LINK"));
        title = Ui.input(this, "Tutorial title");
        topic = Ui.input(this, "Topic (food safety, service, finance…)");
        description = Ui.input(this, "Short description");
        videoUrl = Ui.input(this, "YouTube or public video URL");
        publish = Ui.button(this, "Publish tutorial link");
        publish.setOnClickListener(v -> publishTutorial());
        form.addView(title);
        form.addView(topic);
        form.addView(description);
        form.addView(videoUrl);
        form.addView(publish);
        content.addView(form);
        content.addView(Ui.spacer(this, 14));
        content.addView(Ui.heading(this, "Tutorial library"));
        search = Ui.input(this, "Search by topic, title or tutor");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderTraining(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        content.addView(search);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
    }

    private void publishTutorial() {
        String titleValue = title.getText().toString().trim();
        String topicValue = topic.getText().toString().trim();
        String descriptionValue = description.getText().toString().trim();
        String urlValue = videoUrl.getText().toString().trim();
        String uid = firebase.uid();
        if (uid == null || currentUser == null) return;
        if (TextUtils.isEmpty(titleValue) || TextUtils.isEmpty(topicValue)
                || TextUtils.isEmpty(descriptionValue) || TextUtils.isEmpty(urlValue)) {
            Ui.toast(this, "Complete all fields and add a video link.");
            return;
        }
        if (!isWebUrl(urlValue)) {
            videoUrl.setError("Use a public http:// or https:// link");
            return;
        }
        String id = firebase.training().push().getKey();
        if (id == null) return;
        setPublishing(true);
        TrainingResource resource = new TrainingResource();
        resource.id = id;
        resource.title = titleValue;
        resource.topic = topicValue;
        resource.description = descriptionValue;
        resource.videoUrl = urlValue;
        resource.uploaderId = uid;
        resource.uploaderName = currentUser.name;
        resource.createdAt = System.currentTimeMillis();
        firebase.training().child(id).setValue(resource).addOnCompleteListener(task -> {
            setPublishing(false);
            Ui.toast(this, task.isSuccessful()
                    ? "Tutorial link published successfully."
                    : "Tutorial could not be saved.");
            if (task.isSuccessful()) clearForm();
        });
    }

    private void listenToTraining() {
        firebase.training().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allResources.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TrainingResource resource = child.getValue(TrainingResource.class);
                    if (resource != null) allResources.add(resource);
                }
                allResources.sort(Comparator.comparingLong((TrainingResource r) -> r.createdAt).reversed());
                renderTraining();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(TrainingActivity.this, error.getMessage());
            }
        });
    }

    private void renderTraining() {
        if (list == null || search == null) return;
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        list.removeAllViews();
        int count = 0;
        for (TrainingResource resource : allResources) {
            String text = value(resource.title) + " " + value(resource.topic) + " " + value(resource.uploaderName);
            if (!query.isEmpty() && !text.toLowerCase(Locale.ROOT).contains(query)) continue;
            list.addView(resourceCard(resource));
            count++;
        }
        if (count == 0) list.addView(Ui.body(this,
                query.isEmpty() ? "No tutorials have been shared yet." : "No tutorial matches this search."));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private LinearLayout resourceCard(TrainingResource resource) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, resource.topic));
        card.addView(Ui.title(this, resource.title));
        card.addView(Ui.body(this, resource.description));
        card.addView(Ui.body(this, "Shared by " + resource.uploaderName));
        Button watch = Ui.button(this, "Watch tutorial");
        watch.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resource.videoUrl)));
            } catch (Exception exception) {
                Ui.toast(this, "No app can open this tutorial link.");
            }
        });
        card.addView(watch);
        return card;
    }

    private void setPublishing(boolean busy) {
        publish.setEnabled(!busy);
        publish.setText(busy ? "Publishing…" : "Publish tutorial link");
    }

    private void clearForm() {
        title.setText("");
        topic.setText("");
        description.setText("");
        videoUrl.setText("");
    }

    private boolean isWebUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
