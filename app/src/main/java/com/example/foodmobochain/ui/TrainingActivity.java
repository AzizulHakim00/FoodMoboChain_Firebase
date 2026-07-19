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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.TrainingResource;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TrainingActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private EditText title;
    private EditText topic;
    private EditText description;
    private Button chooseVideo;
    private Button upload;
    private LinearLayout list;
    private EditText search;
    private final List<TrainingResource> allResources = new ArrayList<>();
    private Uri selectedVideo;

    private final ActivityResultLauncher<String> videoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                selectedVideo = uri;
                chooseVideo.setText(uri == null ? "Choose tutorial video" : "Video selected ✓");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Free training", "Food safety, service and entrepreneurship", true);
        firebase.loadCurrentUser(user -> currentUser = user);
        buildUploadForm();
        listenToTraining();
    }

    private void buildUploadForm() {
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "LEARN WITHOUT COST"));
        intro.addView(Ui.heading(this, "Skills for a stronger food business."));
        intro.addView(Ui.body(this, "All verified users can contribute a useful tutorial video. Content can be flagged and moderated by the admin."));
        content.addView(intro);

        LinearLayout form = Ui.card(this);
        form.addView(Ui.label(this, "UPLOAD A TUTORIAL"));
        title = Ui.input(this, "Tutorial title");
        topic = Ui.input(this, "Topic (food safety, service, finance…)");
        description = Ui.input(this, "Short description");
        chooseVideo = Ui.outlineButton(this, "Choose tutorial video");
        chooseVideo.setOnClickListener(v -> videoPicker.launch("video/*"));
        upload = Ui.button(this, "Upload tutorial");
        upload.setOnClickListener(v -> uploadTutorial());
        form.addView(title);
        form.addView(topic);
        form.addView(description);
        form.addView(chooseVideo);
        form.addView(upload);
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

    private void uploadTutorial() {
        String titleValue = title.getText().toString().trim();
        String topicValue = topic.getText().toString().trim();
        String descriptionValue = description.getText().toString().trim();
        String uid = firebase.uid();
        if (uid == null || currentUser == null) return;
        if (TextUtils.isEmpty(titleValue) || TextUtils.isEmpty(topicValue)
                || TextUtils.isEmpty(descriptionValue) || selectedVideo == null) {
            Ui.toast(this, "Complete all fields and choose a video.");
            return;
        }
        String id = firebase.training().push().getKey();
        if (id == null) return;
        setUploading(true);
        StorageReference file = firebase.storage.child("tutorials").child(uid).child(id);
        file.putFile(selectedVideo).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return file.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                setUploading(false);
                Ui.toast(this, "Video upload failed.");
                return;
            }
            TrainingResource resource = new TrainingResource();
            resource.id = id;
            resource.title = titleValue;
            resource.topic = topicValue;
            resource.description = descriptionValue;
            resource.videoUrl = task.getResult().toString();
            resource.uploaderId = uid;
            resource.uploaderName = currentUser.name;
            resource.createdAt = System.currentTimeMillis();
            firebase.training().child(id).setValue(resource).addOnCompleteListener(saveTask -> {
                setUploading(false);
                Ui.toast(this, saveTask.isSuccessful() ? "Tutorial uploaded successfully." : "Tutorial metadata could not be saved.");
                if (saveTask.isSuccessful()) clearForm();
            });
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
                query.isEmpty() ? "No tutorials have been uploaded yet." : "No tutorial matches this search."));
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
                Ui.toast(this, "No video app can open this tutorial.");
            }
        });
        card.addView(watch);
        return card;
    }

    private void setUploading(boolean busy) {
        upload.setEnabled(!busy);
        chooseVideo.setEnabled(!busy);
        upload.setText(busy ? "Uploading…" : "Upload tutorial");
    }

    private void clearForm() {
        title.setText("");
        topic.setText("");
        description.setText("");
        selectedVideo = null;
        chooseVideo.setText("Choose tutorial video");
    }
}
