package com.example.moodchecker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodchecker.adapter.FlashcardAdapter;
import com.example.moodchecker.model.Flashcard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {
    private List<Flashcard> flashcards;
    private FlashcardAdapter adapter;

    private EditText questionInput;
    private EditText answerInput;
    private Spinner answerTypeSpinner;
    private String selectedAnswerType;
    private EditText shortTextInput;  // For short text input

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        // Initialize flashcards list and RecyclerView
        flashcards = new ArrayList<>();

        questionInput = findViewById(R.id.questionInput);
        answerInput = findViewById(R.id.answerInput);
        answerTypeSpinner = findViewById(R.id.answerTypeSpinner);
        shortTextInput = findViewById(R.id.shortTextInput); // Get reference to the short text input field
        RecyclerView recyclerView = findViewById(R.id.flashcardsRecyclerView);
        Button addFlashcardButton = findViewById(R.id.addFlashcardButton);
        Button viewFlashcardsButton = findViewById(R.id.viewFlashcardsButton);

        // Get the reviewer ID from the Intent
        String reviewerId = getIntent().getStringExtra("reviewerId");

        // Setup RecyclerView with the adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FlashcardAdapter(flashcards);
        recyclerView.setAdapter(adapter);

        // Setup Spinner with answer types
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.answer_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        answerTypeSpinner.setAdapter(spinnerAdapter);

        answerTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAnswerType = parent.getItemAtPosition(position).toString();
                if (selectedAnswerType.equals("Multiple Choice")) {
                    answerInput.setVisibility(View.VISIBLE);
                    shortTextInput.setVisibility(View.GONE);  // Hide short text input
                    answerInput.setHint("Enter options separated by commas");
                } else if (selectedAnswerType.equals("Short Text")) {
                    answerInput.setVisibility(View.GONE);  // Hide answer input
                    shortTextInput.setVisibility(View.VISIBLE);  // Show short text input
                    shortTextInput.setHint("Enter your short answer");
                } else {
                    answerInput.setVisibility(View.GONE);
                    shortTextInput.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAnswerType = "Short Text";
            }
        });


        addFlashcardButton.setOnClickListener(view -> {
            if (flashcards.size() < 100) {
                String question = questionInput.getText().toString().trim();
                String answer = selectedAnswerType.equals("Short Text")
                        ? shortTextInput.getText().toString().trim()
                        : answerInput.getText().toString().trim();

                // Check if the question already exists in the list
                boolean isDuplicate = false;
                for (Flashcard flashcard : flashcards) {
                    if (flashcard.getQuestion().equalsIgnoreCase(question)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(this, "This question already exists. Please enter a unique question.", Toast.LENGTH_SHORT).show();

                } else if (!question.isEmpty() && !answer.isEmpty()) {
                    List<String> options = new ArrayList<>();
                    String correctAnswer = "";

                    // For Multiple Choice questions, split options by commas
                    if (selectedAnswerType.equals("Multiple Choice")) {
                        String[] optionArray = answer.split(",");
                        List<String> cleanedOptions = new ArrayList<>();

                        // Clean the options to remove empty strings and trim spaces
                        for (String option : optionArray) {
                            String trimmedOption = option.trim();
                            if (!trimmedOption.isEmpty()) {
                                cleanedOptions.add(trimmedOption);
                            }
                        }

                        // Check if there are more than 3 options
                        if (cleanedOptions.size() > 3) {
                            Toast.makeText(this, "You can only provide up to 3 options for multiple choice.", Toast.LENGTH_SHORT).show();
                            answerInput.setText(""); // Clear the input field
                            return;  // Exit the function if there are more than 3 valid options
                        }

                        // Add valid options to the list
                        options.addAll(cleanedOptions);
                        correctAnswer = options.get(0);  // Set the correct answer (first option)

                    } else {
                        options.add(answer);  // For short text questions, the answer itself is the only option
                        correctAnswer = answer;
                    }

                    // Create a new Flashcard and add it to the list
                    Flashcard newFlashcard = new Flashcard(question, answer, selectedAnswerType, options);
                    newFlashcard.setCorrectAnswer(correctAnswer);  // Set the correct answer
                    flashcards.add(newFlashcard);

                    // Notify the adapter to update the RecyclerView
                    adapter.notifyDataSetChanged();

                    // Firestore reference
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Assuming you're using Firebase Auth
                    DocumentReference flashcardsRef = db.collection("users")
                            .document(userId)
                            .collection("reviewer")
                            .document(reviewerId)
                            .collection("flashcards")
                            .document();

                    flashcardsRef.set(newFlashcard)
                            .addOnSuccessListener(aVoid -> {
                                flashcards.add(newFlashcard);  // Add to local list
                                adapter.notifyDataSetChanged(); // Update UI

                                // Clear input fields
                                questionInput.setText("");
                                answerInput.setText("");
                                shortTextInput.setText("");

                                Toast.makeText(this, "Flashcard added successfully", Toast.LENGTH_SHORT).show();

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save flashcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Please enter a question and answer", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If the number of flashcards exceeds 100, show a warning
                Toast.makeText(this, "You can only add up to 100 flashcards", Toast.LENGTH_SHORT).show();
            }
        });


        // Navigate to view flashcards page
        viewFlashcardsButton.setOnClickListener(view -> {
            if (flashcards.isEmpty()) {
                // Show a toast if no flashcards are present
                Toast.makeText(FlashcardsActivity.this, "No flashcards to view", Toast.LENGTH_SHORT).show();
            } else {
                // Proceed to ViewFlashcardsActivity if flashcards are present
                Intent intent = new Intent(FlashcardsActivity.this, ViewFlashcardsActivity.class);
                intent.putExtra("flashcardsList", (ArrayList<Flashcard>) flashcards);
                startActivity(intent);
            }
        });


        if (reviewerId == null || reviewerId.isEmpty()) {
            Toast.makeText(this, "Reviewer ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();

        // Firestore reference to the reviewer's flashcards
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference flashcardsRef = db.collection("users")
                .document(userId)
                .collection("reviewer")
                .document(reviewerId)
                .collection("flashcards");

        // Initialize RecyclerView and FlashcardAdapter
        flashcards = new ArrayList<>();
        recyclerView = findViewById(R.id.flashcardsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FlashcardAdapter(flashcards);
        recyclerView.setAdapter(adapter);

        // Load flashcards from Firestore
        loadFlashcards(flashcardsRef);

        // Add Flashcard Button
//        findViewById(R.id.addFlashcardButton).setOnClickListener(view -> addFlashcard(flashcardsRef));
        findViewById(R.id.addFlashcardButton).setOnClickListener(view -> {
            String question = questionInput.getText().toString().trim();
            String answer = selectedAnswerType.equals("Short Text")
                    ? shortTextInput.getText().toString().trim()
                    : answerInput.getText().toString().trim();

            if (!question.isEmpty() && !answer.isEmpty()) {
                List<String> options = new ArrayList<>();

                if (selectedAnswerType.equals("Multiple Choice")) {
                    String[] optionArray = answer.split(",");
                    for (String option : optionArray) {
                        options.add(option.trim());
                    }

                } else {
                    options.add(answer); // For short text, the answer itself is the only option
                }

                addFlashcard(question, answer, selectedAnswerType, options); // Pass the parameters
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            }
        });

        // View Flashcards Button
        viewFlashcardsButton.setOnClickListener(view -> {
            if (flashcards.isEmpty()) {
                // Show a toast if no flashcards are present
                Toast.makeText(FlashcardsActivity.this, "No flashcards to view", Toast.LENGTH_SHORT).show();
            } else {
                // Proceed to ViewFlashcardsActivity if flashcards are present
                Intent intent = new Intent(FlashcardsActivity.this, ViewFlashcardsActivity.class);
                intent.putExtra("flashcardsList", (ArrayList<Flashcard>) flashcards);
                startActivity(intent);
            }
        });


        adapter.setDeleteCallback((flashcard, position) -> {
            // Display confirmation dialog
            new AlertDialog.Builder(FlashcardsActivity.this)
                    .setTitle("Delete Flashcard")
                    .setMessage("Are you sure you want to delete this flashcard?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // If user clicks Yes, proceed with deletion
                        deleteFlashcard(flashcard, position);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // If user clicks No, dismiss the dialog and don't delete
                        dialog.dismiss();
                    })
                    .show();
        });

        // Assuming reviewerId is already passed from the previous activity
//        String reviewerId = getIntent().getStringExtra("reviewerId");

// Firestore reference to fetch the reviewer's name and description
        db = FirebaseFirestore.getInstance();
        DocumentReference reviewerRef = db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid()) // Get current user
                .collection("reviewer")
                .document(reviewerId);

        reviewerRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String reviewerName = documentSnapshot.getString("name");
                        String reviewerDescription = documentSnapshot.getString("description");

                        // Set the data to the TextViews
                        TextView nameTextView = findViewById(R.id.textView13); // For name
                        TextView descriptionTextView = findViewById(R.id.textView11); // For description
                        nameTextView.setText(reviewerName);
                        descriptionTextView.setText(reviewerDescription);
                    } else {
                        Log.e("FlashcardsActivity", "No such reviewer document");
                        Toast.makeText(FlashcardsActivity.this, "Reviewer data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FlashcardsActivity", "Error fetching reviewer data", e);
                    Toast.makeText(FlashcardsActivity.this, "Failed to load reviewer data", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFlashcard(Flashcard flashcard, int position) {
        FirebaseFirestore dbs = FirebaseFirestore.getInstance();
        FirebaseAuth mAuths = FirebaseAuth.getInstance();
        String userIds = mAuths.getCurrentUser().getUid();
        String reviewerId = getIntent().getStringExtra("reviewerId");

        if (userIds == null || reviewerId == null) {
            Toast.makeText(this, "User or Reviewer ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference flashcardsRefs = dbs.collection("users")
                .document(userIds)
                .collection("reviewer")
                .document(reviewerId)
                .collection("flashcards");

        // Query Firestore for the document to delete
        flashcardsRefs.whereEqualTo("question", flashcard.getQuestion())
                .whereEqualTo("answer", flashcard.getAnswer())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove the item from the RecyclerView list
                                        flashcards.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        Toast.makeText(this, "Flashcard deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to delete flashcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Flashcard not found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to query Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFlashcards(CollectionReference flashcardsRef) {
        flashcardsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("FlashcardsActivity", "Loaded flashcards: " + querySnapshot.size());

                    // Clear the current list to avoid duplication
                    flashcards.clear();

                    // Process the documents and convert them to Flashcard objects
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Flashcard flashcard = document.toObject(Flashcard.class);
                        flashcards.add(flashcard);
                        Log.d("FlashcardsActivity", "Flashcard: " + flashcard.getQuestion());
                    }

                    // Notify the adapter to refresh the RecyclerView
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FlashcardsActivity", "Error loading flashcards", e);
                    Toast.makeText(this, "Failed to load flashcards: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addFlashcard(String question, String answer, String answerType, List<String> options) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid(); // Get the current user's ID
            String reviewerId = getIntent().getStringExtra("reviewerId"); // Get the reviewer ID from Intent

            if (reviewerId == null) {
                Toast.makeText(this, "Error: Reviewer ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference flashcardsRef = db.collection("users")
                    .document(userId)
                    .collection("reviewer")
                    .document(reviewerId)
                    .collection("flashcards");

            // Check for duplicate question
            flashcardsRef.whereEqualTo("question", question)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // If the question already exists, show a message
                            Toast.makeText(this, "This question already exists. Please enter a unique question.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Check if the options list has more than 3 choices for Multiple Choice questions
                            if (answerType.equals("Multiple Choice") && options.size() > 3) {
                                Toast.makeText(this, "You can only provide up to 3 options for multiple choice.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Create a new flashcard object
                                Flashcard flashcard = new Flashcard(question, answer, answerType, options);

                                // Save the flashcard to Firestore
                                flashcardsRef.add(flashcard)
                                        .addOnSuccessListener(documentReference -> {
                                            Toast.makeText(this, "Flashcard added successfully!", Toast.LENGTH_SHORT).show();
                                            loadFlashcards(flashcardsRef);

                                            // Clear the input fields
                                            questionInput.setText("");
                                            answerInput.setText("");
                                            shortTextInput.setText("");
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error adding flashcard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error checking for duplicate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

}