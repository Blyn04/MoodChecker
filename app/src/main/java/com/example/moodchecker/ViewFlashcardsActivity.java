package com.example.moodchecker;

import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.moodchecker.model.Flashcard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewFlashcardsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private List<Flashcard> flashcards;
    private int currentIndex = 0;
    private RadioGroup optionsGroup;
    private TextView questionNumberTextView;
    private TextView questionTextView;
    private RadioButton optionOne, optionTwo, optionThree, optionFour;
    private TextView streakTextView;
    private int streakCount = 0;
    private TextToSpeech textToSpeech;
    private ImageButton ttsButton;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_flashcards);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid(); // Make sure to authenticate users

        loadStreakFromFirebase();

        textToSpeech = new TextToSpeech(this, this);

        // Initialize flashcards list from intent
        flashcards = (List<Flashcard>) getIntent().getSerializableExtra("flashcardsList");
        if (flashcards == null) {
            flashcards = new ArrayList<>();
        }

        questionNumberTextView = findViewById(R.id.questionNumber);
        questionTextView = findViewById(R.id.questionText);
        optionOne = findViewById(R.id.optionOne);
        optionTwo = findViewById(R.id.optionTwo);
        optionThree = findViewById(R.id.optionThree);
        optionsGroup = findViewById(R.id.optionsGroup);
        streakTextView = findViewById(R.id.streakTextView);
        ttsButton = findViewById(R.id.ttsButton);

        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);
        Button submitButton = findViewById(R.id.submitButton);

        // Display the first question
        displayFlashcard(currentIndex);

        nextButton.setOnClickListener(view -> {
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                Flashcard flashcard = flashcards.get(currentIndex);
                flashcard.setAnswered(false); // Reset the answer status
                flashcard.setCorrect(false);  // Reset correctness
                displayFlashcard(currentIndex);
            }
        });

        backButton.setOnClickListener(view -> {
            if (currentIndex > 0) {
                currentIndex--;
                displayFlashcard(currentIndex);
            }
        });


        ttsButton.setOnClickListener(view -> {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop(); // Stop TTS if it is currently speaking
                Toast.makeText(ViewFlashcardsActivity.this, "Stopped speaking", Toast.LENGTH_SHORT).show();
            } else {
                // Trigger TTS to read the current question
                Flashcard flashcard = flashcards.get(currentIndex);
                String questionText = flashcard.getQuestion();
                textToSpeech.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });


        submitButton.setOnClickListener(view -> {
            Flashcard flashcard = flashcards.get(currentIndex);
            String correctAnswer = flashcard.getCorrectAnswer();
            String answer = flashcard.getAnswer();

            if (flashcard.isAnswered() && flashcard.isCorrect()) {
                // Don't allow resubmission if the answer was correct
                Toast.makeText(ViewFlashcardsActivity.this, "You have already answered this question correctly!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (flashcard.getAnswerType().equals("Multiple Choice")) {
                String selectedAnswer = getSelectedOption();

                if (selectedAnswer != null) {
                    if (selectedAnswer.equals(correctAnswer)) {
                        streakCount++;
                        flashcard.setCorrect(true); // Mark as correct
                        saveStreakToFirebase(streakCount);

                        flashcard.setAnswered(true);
                        updateStreakDisplay();

                        goToNextQuestion();

                        Toast.makeText(ViewFlashcardsActivity.this, "Correct!", Toast.LENGTH_SHORT).show();
                    } else {
                        streakCount = 0;
                        flashcard.setCorrect(false); // Mark as incorrect
                        saveStreakToFirebase(streakCount);

                        updateStreakDisplay();
                        flashcard.setAnswered(true);

                        Toast.makeText(ViewFlashcardsActivity.this, "Incorrect. The correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();
                    }
//                    flashcard.setAnswered(true);
//                    updateStreakDisplay();

                } else {
                    Toast.makeText(ViewFlashcardsActivity.this, "Please select an answer.", Toast.LENGTH_SHORT).show();
                }

            } else if (flashcard.getAnswerType().equals("Short Text")) {
                EditText shortTextInput = findViewById(R.id.shortTextInput);
                String userAnswer = shortTextInput.getText().toString().trim();

                if (!userAnswer.isEmpty()) {
                    if (userAnswer.equalsIgnoreCase(answer)) {
                        streakCount++;
                        flashcard.setCorrect(true); // Mark as correct
                        saveStreakToFirebase(streakCount);

                        flashcard.setAnswered(true);
                        updateStreakDisplay();
                        goToNextQuestion();

                        Toast.makeText(ViewFlashcardsActivity.this, "Correct!", Toast.LENGTH_SHORT).show();

                    } else {
                        streakCount = 0;
                        flashcard.setCorrect(false); // Mark as incorrect
                        saveStreakToFirebase(streakCount);

                        updateStreakDisplay();
                        flashcard.setAnswered(true);

                        Toast.makeText(ViewFlashcardsActivity.this, "Incorrect. The correct answer is: " + answer, Toast.LENGTH_SHORT).show();
                    }
//                    flashcard.setAnswered(true);
//                    updateStreakDisplay();
                } else {
                    Toast.makeText(ViewFlashcardsActivity.this, "Please enter an answer.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int langResult = textToSpeech.setLanguage(Locale.US);
            if (langResult == TextToSpeech.LANG_MISSING_DATA | langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported or missing data.");
            } else {
                Log.i("TTS", "Language set successfully.");
            }
        } else {
            Log.e("TTS", "Initialization failed.");
        }
    }

    private void goToNextQuestion() {
        if (currentIndex < flashcards.size() - 1) {
            currentIndex++;
            Flashcard flashcard = flashcards.get(currentIndex);
            flashcard.setAnswered(false); // Reset the answer status for the new question
            flashcard.setCorrect(false);  // Reset correctness
            displayFlashcard(currentIndex); // Display the next question
        } else {
            // If no more questions, show a message or finish the activity
            Toast.makeText(ViewFlashcardsActivity.this, "You've completed all the questions!", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayFlashcard(int index) {
        if (index < flashcards.size()) {
            Flashcard flashcard = flashcards.get(index);
            questionNumberTextView.setText("QUESTION " + String.format("%02d", index + 1));
            questionTextView.setText(flashcard.getQuestion());

            String reviewerName = flashcard.getReviewerName(); // Make sure you have this field in Flashcard
            String description = flashcard.getDescription();

            TextView reviewerNameTextView = findViewById(R.id.textView13);
            TextView descriptionTextView = findViewById(R.id.textView11);

            reviewerNameTextView.setText(reviewerName); // Set the name
            descriptionTextView.setText(description);

            // Use TTS to read the question
//            textToSpeech.speak(flashcard.getQuestion(), TextToSpeech.QUEUE_FLUSH, null, null);
            // Use TTS to read the question with audio stream parameter
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            textToSpeech.speak(flashcard.getQuestion(), TextToSpeech.QUEUE_FLUSH, params);


            List<String> options = flashcard.getOptions();

            if (flashcard.getAnswerType().equals("Multiple Choice")) {
                optionsGroup.setVisibility(View.VISIBLE);

                if (options != null && options.size() > 0) {
                    optionOne.setText(options.get(0));
                    optionOne.setVisibility(View.VISIBLE);
                } else {
                    optionOne.setVisibility(View.GONE);
                }

                if (options.size() > 1) {
                    optionTwo.setText(options.get(1));
                    optionTwo.setVisibility(View.VISIBLE);
                } else {
                    optionTwo.setVisibility(View.GONE);
                }

                if (options.size() > 2) {
                    optionThree.setText(options.get(2));
                    optionThree.setVisibility(View.VISIBLE);
                } else {
                    optionThree.setVisibility(View.GONE);
                }

//                if (options.size() > 3) {
//                    optionFour.setText(options.get(3));
//                    optionFour.setVisibility(View.VISIBLE);
//                } else {
//                    optionFour.setVisibility(View.GONE);
//                }

                findViewById(R.id.shortTextInput).setVisibility(View.GONE);

            } else if (flashcard.getAnswerType().equals("Short Text")) {
                optionsGroup.setVisibility(View.GONE);

                findViewById(R.id.shortTextInput).setVisibility(View.VISIBLE);
                EditText shortTextInput = findViewById(R.id.shortTextInput);
                shortTextInput.setText("");
            }
        }
    }


    private String getSelectedOption() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            return selectedRadioButton.getText().toString();
        }
        return null;
    }

    private void updateStreakDisplay() {
        streakTextView.setText("Streak: " + streakCount);

        // Show specific message based on the streak count
        String message = "";
        AppCompatImageView streakIcon = findViewById(R.id.streakIcon);

        // Default icon for all streaks except 30, 50, 70, and 100
//        streakIcon.setImageResource(R.drawable.streakfire);  // Set default icon

        if (streakIcon != null) {
            streakIcon.setImageResource(R.drawable.streakfire);
        } else {
            Log.e("ImageViewError", "ImageView is null. Check the ID or layout inflation.");
        }

        // Check the streak count and update accordingly
        if (streakCount == 10) {
            message = "You're doing great! Keep up the momentum!";
        } else if (streakCount == 20) {
            message = "Fantastic progress! Your hard work is paying off!";
        } else if (streakCount == 30) {
            message = "Impressive! You’re mastering this material!";
            streakIcon.setImageResource(R.drawable.s30);   // Set to s30 for 30 streak
        } else if (streakCount == 40) {
            message = "Amazing focus! You’re well on your way to acing this.";
        } else if (streakCount == 50) {
            message = "Halfway to 100! Keep that knowledge growing!";
            streakIcon.setImageResource(R.drawable.s50);   // Set to s50 for 50 streak
        } else if (streakCount == 60) {
            message = "Incredible dedication! You're truly committed.";
        } else if (streakCount == 70) {
            message = "You're unstoppable! Just a few more to hit 100!";
            streakIcon.setImageResource(R.drawable.s70);   // Set to s70 for 70 streak
        } else if (streakCount == 80) {
            message = "Your effort is inspiring! Keep pushing forward.";
        } else if (streakCount == 90) {
            message = "Almost at 100! You're a reviewing champion!";
        } else if (streakCount == 100) {
            message = "Congratulations on 100! Your dedication is remarkable!";
            streakIcon.setImageResource(R.drawable.s100);   // Set to s100 for 100 streak
        }

        // If a message is available for the streak, show it
        if (!message.isEmpty()) {
            showStreakPopup(streakCount, message);
        }
    }

    private void showStreakPopup(int streak, String message) {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_streak, null);

        // Create the dialog
//        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
//        builder.setView(dialogView);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);  // Set it to be cancellable when clicking outside

        // Set custom dialog window dimensions
        dialog.getWindow().setLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog);

        // Customize dialog elements
        TextView streakMessage = dialogView.findViewById(R.id.streakMessage);
        TextView streakCountMessage = dialogView.findViewById(R.id.streakCountMessage);
        Button okButton = dialogView.findViewById(R.id.okButton);

        // Update the dialog message based on streak count
        streakCountMessage.setText(streak + " Streak!!");
        streakMessage.setText(message);

        // Show the dialog
//        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Handle the OK button click
        okButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewFlashcardsActivity.this, FlashcardsActivity.class); // Replace 'CurrentActivity.this' with your actual activity name
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Optional: Clears back stack
            startActivity(intent);
            finish(); // Optional: Finish the current activity
        });

    }


    private void saveStreakToFirebase(int streakCount) {
        // Get the current user's UID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get Firestore instance and reference to the user's streaks
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference streakRef = db.collection("users").document(userId).collection("streaks").document();

        // Create a streak data object
        HashMap<String, Object> streakData = new HashMap<>();
        streakData.put("streak", streakCount);

        // Save streak data to Firestore
        streakRef.set(streakData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Streak updated successfully"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error updating streak", e));
    }


    private void loadStreakFromFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the streak value safely
                        Long savedStreakLong = documentSnapshot.getLong("streak");

                        if (savedStreakLong != null) {
                            streakCount = savedStreakLong.intValue(); // If valid, set it
                        } else {
                            streakCount = 0; // Default to 0 if the streak is missing
                        }

                        updateStreakDisplay(); // Update UI
                    } else {
                        // Handle case where the document does not exist
                        streakCount = 0; // Default to 0 if the user document does not exist
                        updateStreakDisplay();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to retrieve streak
                    Log.w("Firestore", "Error getting streak", e);
                });

    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

//    @Override
//    public void onBackPressed() {
//        // Show a confirmation dialog
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Confirm Exit");
//        builder.setMessage("You will lose your current streak. Are you sure you want to go back?");
//
//        // If the user confirms
//        builder.setPositiveButton("Yes", (dialog, which) -> {
//            dialog.dismiss();
//            super.onBackPressed(); // Proceed with the default back button behavior
//        });
//
//        // If the user cancels
//        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

    @Override
    public void onBackPressed() {
        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Exit Flashcards")
                .setMessage("Exiting now will reset your current streak. Are you sure you want to go back?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Reset the current streak count but do not affect the database
                    streakCount = 0;
                    updateStreakDisplay();
                    super.onBackPressed(); // Navigate back
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Do nothing, dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }
}
