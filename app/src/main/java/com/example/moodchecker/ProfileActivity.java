package com.example.moodchecker;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodchecker.model.User;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvStreak;
    private EditText etName, etGmail;
    private Button btnEditSave;
    private boolean isEditing = false;
    private ImageView colorOption1, colorOption2, colorOption3, colorOption4, colorOption5, colorOption6, avatarOption1, avatarOption2, avatarOption3, avatarOption4, avatarOption5, avatarOption6, avatarOption7, avatarOption8, avatarOption9, avatarOption10;
    private LinearLayout idCardSection;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView profileAvatar;
    private ImageView badgeImageView;
    private int streak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnEditSave = findViewById(R.id.btnEditSave);

        // Initialize UI components
        etName = findViewById(R.id.etName);
        etGmail = findViewById(R.id.etGmail);
        tvStreak = findViewById(R.id.tvStreak);

        idCardSection = findViewById(R.id.idCardSection);
        colorOption1 = findViewById(R.id.colorOption1);
        colorOption2 = findViewById(R.id.colorOption2);
        colorOption3 = findViewById(R.id.colorOption3);
        colorOption4 = findViewById(R.id.colorOption4);
        colorOption5 = findViewById(R.id.colorOption5);
        colorOption6 = findViewById(R.id.colorOption6);

        avatarOption1 = findViewById(R.id.avatarOption1);
        avatarOption2 = findViewById(R.id.avatarOption2);
        avatarOption3 = findViewById(R.id.avatarOption3);
        avatarOption4 = findViewById(R.id.avatarOption4);
        avatarOption5 = findViewById(R.id.avatarOption5);
        avatarOption6 = findViewById(R.id.avatarOption6);
        avatarOption7 = findViewById(R.id.avatarOption7);
        avatarOption8 = findViewById(R.id.avatarOption8);
        avatarOption9 = findViewById(R.id.avatarOption9);
        avatarOption10 = findViewById(R.id.avatarOption10);

        profileAvatar = findViewById(R.id.profileAvatar);

        // Set click listeners for color options
        colorOption1.setOnClickListener(view -> updateColor("#BCDDD1"));
        colorOption2.setOnClickListener(view -> updateColor("#F9DBA7"));
        colorOption3.setOnClickListener(view -> updateColor("#F1D7DD"));
        colorOption4.setOnClickListener(view -> updateColor("#F4B9A7"));
        colorOption5.setOnClickListener(view -> updateColor("#B3DCEC"));
        colorOption6.setOnClickListener(view -> updateColor("#C4CFE8"));

        // Set click listeners for avatar options
        avatarOption1.setOnClickListener(view -> updateAvatar(1));
        avatarOption2.setOnClickListener(view -> updateAvatar(2));
        avatarOption3.setOnClickListener(view -> updateAvatar(3));
        avatarOption4.setOnClickListener(view -> updateAvatar(4));
        avatarOption5.setOnClickListener(view -> updateAvatar(5));
        avatarOption6.setOnClickListener(view -> updateAvatar(6));
        avatarOption7.setOnClickListener(view -> updateAvatar(7));
        avatarOption8.setOnClickListener(view -> updateAvatar(8));
        avatarOption9.setOnClickListener(view -> updateAvatar(9));
        avatarOption10.setOnClickListener(view -> updateAvatar(10));

        if (tvStreak != null) {
            tvStreak.setText("0"); // Set the streak to 0
        } else {
            Log.e("MyActivity", "TextView with ID tvStreak not found");
        }

        // Handle Save Changes button
        findViewById(R.id.btnSaveChanges).setOnClickListener(view -> {
            String newName = etName.getText().toString().trim();
            String newGmail = etGmail.getText().toString().trim();

            if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newGmail)) {
                Toast.makeText(ProfileActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the username already exists in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("username", newName)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // If the username already exists, show a message
                                Toast.makeText(ProfileActivity.this, "Username is already taken, please choose another", Toast.LENGTH_SHORT).show();
                            } else {
                                // Update the profile with the new values
                                etName.setText(newName);

                                // Show toast message
                                Toast.makeText(ProfileActivity.this, "Changes Saved", Toast.LENGTH_SHORT).show();

                                // Save changes to Firestore
                                saveChanges(newName, newGmail);
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "Error checking username", Toast.LENGTH_SHORT).show();
                        }
                    });
        });


        btnEditSave.setOnClickListener(view -> {
            if (isEditing) {
                // Save changes
                saveChanges(etName.getText().toString(), etGmail.getText().toString());
                toggleEditing(false); // Exit editing mode
            } else {
                // Enter editing mode
                toggleEditing(true);
            }
        });
        toggleEditing(false);

        badgeImageView = findViewById(R.id.badge);
        streak = getUserStreak(); // Assuming you have a method to fetch the user's streak

        updateBadge(streak);

        // Load the current user's profile data from Firestore
        loadUserProfile();

    }

    // Fetch the current user's data from Firestore
//    private void loadUserProfile() {
//        // Get the current user from FirebaseAuth
//        String userId = mAuth.getCurrentUser().getUid();  // Current logged-in user's UID
//
//        db.collection("users")
//                .document(userId)
//                .collection("streaks") // Access the streaks sub-collection
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    if (!queryDocumentSnapshots.isEmpty()) {
//                        // Loop through the documents in the streaks collection
//                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                            // Retrieve the 'streak' field from each document
//                            Object streakField = documentSnapshot.get("streak");
//
//                            if (streakField != null) {
//                                // Check if the streak is a number (long or int)
//                                if (streakField instanceof Long) {
//                                    // If the streak is a Long (number), display it as String
//                                    tvStreak.setText(String.valueOf(streakField));
//                                } else if (streakField instanceof Integer) {
//                                    // If the streak is an Integer, display it as String
//                                    tvStreak.setText(String.valueOf(streakField));
//                                } else if (streakField instanceof Double) {
//                                    // If the streak is a Double, display it as String
//                                    tvStreak.setText(String.valueOf(streakField));
//                                } else {
//                                    // If the streak is of an unsupported type, handle it
//                                    tvStreak.setText("0");
//                                }
//                                break;  // Assuming you only need the first streak document
//                            }
//                        }
//                    } else {
//                        // If no streak document exists, set default value (e.g., "0")
//                        tvStreak.setText("0");
//                        Toast.makeText(ProfileActivity.this, "Streak data not found!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(ProfileActivity.this, "Error loading streak data", Toast.LENGTH_SHORT).show();
//                });
//
//        db.collection("users")
//                .document(userId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        // Get user details from the document
//                        String name = documentSnapshot.getString("username");
//                        String email = documentSnapshot.getString("email");
//                        String cardColor = documentSnapshot.getString("cardColor");
//
//                        User user = new User(name, email);
//
//                        // Set the fetched details in the UI
//                        if (name != null) etName.setText(name);
//                        if (email != null) etGmail.setText(email);
//                        if (cardColor != null) {
//                            GradientDrawable background = (GradientDrawable) idCardSection.getBackground();
//                            background.setColor(android.graphics.Color.parseColor(cardColor));
//                        }
//
//                        toggleEditing(false);
//                    } else {
//                        Toast.makeText(ProfileActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(ProfileActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
//                });
//
////        db.collection("users").document(userId)
////                .get()
////                .addOnSuccessListener(documentSnapshot -> {
////                    if (documentSnapshot.exists()) {
////                        String avatar = documentSnapshot.getString("avatar");
////                        if (avatar != null) {
////                            switch (avatar) {
////                                case "avatar1":
////                                    updateAvatar(1);
////                                    break;
////                                case "avatar2":
////                                    updateAvatar(2);
////                                    break;
////                                case "avatar3":
////                                    updateAvatar(3);
////                                    break;
////                                case "avatar4":
////                                    updateAvatar(4);
////                                    break;
////                                case "avatar5":
////                                    updateAvatar(5);
////                                    break;
////                                case "avatar6":
////                                    updateAvatar(6);
////                                    break;
////                                case "avatar7":
////                                    updateAvatar(7);
////                                    break;
////                                case "avatar8":
////                                    updateAvatar(8);
////                                    break;
////                                case "avatar9":
////                                    updateAvatar(9);
////                                    break;
////                                case "avatar10":
////                                    updateAvatar(10);
////                                    break;
////                            }
////                        }
////                    }
////                })
////                .addOnFailureListener(e -> Log.e("ProfileActivity", "Failed to load user profile", e));
//
//        db.collection("users").document(userId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        String avatarName = documentSnapshot.getString("avatar");
//                        if (avatarName != null && !avatarName.isEmpty()) {
//                            loadAvatarFromDrawable(avatarName);
//                        } else {
//                            // Use default avatar if the avatar field is null or empty
//                            profileAvatar.setImageResource(R.drawable.avatar1);
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("ProfileActivity", "Failed to load profile data", e);
//                    // Use default avatar in case of failure
//                    profileAvatar.setImageResource(R.drawable.avatar1);
//                });
//    }

    private void loadUserProfile() {
        // Get the current user from FirebaseAuth
        String userId = mAuth.getCurrentUser().getUid();  // Current logged-in user's UID

        db.collection("users")
                .document(userId)
                .collection("streaks") // Access the streaks sub-collection
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int totalStreaks = 0;  // Initialize total streaks to 0
                        // Loop through the documents in the streaks collection
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            // Retrieve the 'streak' field from each document
                            Object streakField = documentSnapshot.get("streak");

                            if (streakField != null) {
                                // Check if the streak is a number (long or int)
                                if (streakField instanceof Long) {
                                    // If the streak is a Long (number), add it to the total streaks
                                    totalStreaks += ((Long) streakField).intValue();
                                } else if (streakField instanceof Integer) {
                                    // If the streak is an Integer, add it to the total streaks
                                    totalStreaks += (Integer) streakField;
                                } else if (streakField instanceof Double) {
                                    // If the streak is a Double, add it to the total streaks
                                    totalStreaks += ((Double) streakField).intValue();
                                }
                            }
                        }
                        // Set the total streaks in the TextView
                        tvStreak.setText(String.valueOf(totalStreaks));
                        updateBadge(totalStreaks);
                    } else {
                        // If no streak document exists, set default value (e.g., "0")
                        tvStreak.setText("0");
                        Toast.makeText(ProfileActivity.this, "Streak data not found!", Toast.LENGTH_SHORT).show();
                        updateBadge(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error loading streak data", Toast.LENGTH_SHORT).show();
                    updateBadge(0);
                });

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get user details from the document
                        String name = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        String cardColor = documentSnapshot.getString("cardColor");

                        User user = new User(name, email);

                        // Set the fetched details in the UI
                        if (name != null) etName.setText(name);
                        if (email != null) etGmail.setText(email);
                        if (cardColor != null) {
                            GradientDrawable background = (GradientDrawable) idCardSection.getBackground();
                            background.setColor(android.graphics.Color.parseColor(cardColor));
                        }

                        toggleEditing(false);
                    } else {
                        Toast.makeText(ProfileActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
                });

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String avatarName = documentSnapshot.getString("avatar");
                        if (avatarName != null && !avatarName.isEmpty()) {
                            loadAvatarFromDrawable(avatarName);
                        } else {
                            // Use default avatar if the avatar field is null or empty
                            profileAvatar.setImageResource(R.drawable.avatar1);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Failed to load profile data", e);
                    // Use default avatar in case of failure
                    profileAvatar.setImageResource(R.drawable.avatar1);
                });
    }

    private void updateBadge(int streak) {
        if (streak >= 100) {
            badgeImageView.setImageResource(R.drawable.s100);
        } else if (streak >= 70) {
            badgeImageView.setImageResource(R.drawable.s70);
        } else if (streak >= 50) {
            badgeImageView.setImageResource(R.drawable.s50);
        } else if (streak >= 30) {
            badgeImageView.setImageResource(R.drawable.s30);
        } else {
            badgeImageView.setImageResource(R.drawable._0_streak); // Default badge if streak is less than 30
        }
    }

    private int getUserStreak() {
        // For example, get the streak from a user object or a database
        return 50; // Replace with actual logic to get the streak
    }

    private void updateColor(String color) {
        // Change the color of the ID Card section
        GradientDrawable background = (GradientDrawable) idCardSection.getBackground();
        background.setColor(android.graphics.Color.parseColor(color));

        // Save the selected color to Firestore
        saveColorToDatabase(color);

        // Optional: Show a confirmation message
        Toast.makeText(ProfileActivity.this, "Color updated", Toast.LENGTH_SHORT).show();
    }

    private void saveColorToDatabase(String color) {
        // Get the current user's ID
        String userId = mAuth.getCurrentUser().getUid();

        // Create a map to store the color data
        Map<String, Object> updates = new HashMap<>();
        updates.put("cardColor", color);

        // Update the user's document in Firestore
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ProfileActivity", "Color updated successfully in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error updating color in Firestore", e);
                });
    }

    private void updateAvatar(int avatarIndex) {
        String selectedAvatar = "avatar" + avatarIndex;

        // Update the profile avatar dynamically
        int avatarResourceId = getResources().getIdentifier(selectedAvatar, "drawable", getPackageName());
        profileAvatar.setImageResource(avatarResourceId);

        // Save the selected avatar to Firestore or SharedPreferences
        saveAvatarToDatabase(selectedAvatar);

        highlightSelectedAvatar(avatarIndex);

        // Provide feedback (e.g., highlight the selected avatar)
        switch (avatarIndex) {
            case 1:
                avatarOption1.setBackgroundResource(R.drawable.selected_border); // Optional visual feedback
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 2:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(R.drawable.selected_border);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 3:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(R.drawable.selected_border);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 4:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(R.drawable.selected_border);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 5:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(R.drawable.selected_border);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 6:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(R.drawable.selected_border);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 7:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(R.drawable.selected_border);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 8:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(R.drawable.selected_border);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(0);
                break;
            case 9:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(R.drawable.selected_border);
                avatarOption10.setBackgroundResource(0);
                break;

            case 10:
                avatarOption1.setBackgroundResource(0);
                avatarOption2.setBackgroundResource(0);
                avatarOption3.setBackgroundResource(0);
                avatarOption4.setBackgroundResource(0);
                avatarOption5.setBackgroundResource(0);
                avatarOption6.setBackgroundResource(0);
                avatarOption7.setBackgroundResource(0);
                avatarOption8.setBackgroundResource(0);
                avatarOption9.setBackgroundResource(0);
                avatarOption10.setBackgroundResource(R.drawable.selected_border);
                break;
        }
    }

    private void highlightSelectedAvatar(int avatarIndex) {
        ImageView[] avatars = {
                avatarOption1, avatarOption2, avatarOption3, avatarOption4, avatarOption5,
                avatarOption6, avatarOption7, avatarOption8, avatarOption9, avatarOption10
        };

        // Reset all avatar borders
        for (ImageView avatar : avatars) {
            avatar.setBackgroundResource(0); // Remove any applied border
        }

        // Highlight the selected avatar
        if (avatarIndex > 0 && avatarIndex <= avatars.length) {
            avatars[avatarIndex - 1].setBackgroundResource(R.drawable.selected_border);
        }
    }

    private void saveAvatarToDatabase(String avatarName) {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatar", avatarName);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Log.e("ProfileActivity", "Failed to update avatar", e));
    }

    private void loadAvatarFromDrawable(String avatarName) {
        try {
            // Get the resource ID of the drawable by name
            Resources res = getResources();
            int resourceId = res.getIdentifier(avatarName, "drawable", getPackageName());

            // Check if the resource exists
            if (resourceId != 0) {
                profileAvatar.setImageResource(resourceId);
            } else {
                // Fallback to a default image if the resource is not found
                profileAvatar.setImageResource(R.drawable.avatar1);
            }
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error loading avatar", e);
            profileAvatar.setImageResource(R.drawable.avatar1);
        }
    }

    // Optional: Save changes to Firestore when the user edits their profile
    private void saveChanges(String newName, String newGmail) {
        String userId = mAuth.getCurrentUser().getUid();

        // Check for duplicate username
        db.collection("users")
                .whereEqualTo("username", newName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Check if the duplicate username belongs to a different user
                            boolean isDuplicate = false;
                            for (DocumentSnapshot document : task.getResult()) {
                                if (!document.getId().equals(userId)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (isDuplicate) {
                                // Duplicate username exists
                                Toast.makeText(ProfileActivity.this, "Username is already taken, please choose another", Toast.LENGTH_SHORT).show();
                            } else {
                                // Proceed with updating the profile
                                updateProfile(userId, newName, newGmail);
                            }
                        } else {
                            // No duplicate found, proceed with updating
                            updateProfile(userId, newName, newGmail);
                        }
                    } else {
                        // Handle query error
                        Toast.makeText(ProfileActivity.this, "Error checking username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to update the profile
    private void updateProfile(String userId, String newName, String newGmail) {
        // Create a map of updated profile data
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("username", newName);
        userUpdates.put("email", newGmail);

        // Save updated data to Firestore
        db.collection("users").document(userId)
                .update(userUpdates)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }


    private void toggleEditing(boolean enable) {
        isEditing = enable;

        // Toggle EditText fields' editable state
        etName.setFocusableInTouchMode(enable);
        etName.setCursorVisible(enable);
        etGmail.setFocusableInTouchMode(enable);
        etGmail.setCursorVisible(enable);

        // Update button text
        btnEditSave.setText(enable ? "Save Changes" : "Edit");
    }
}
