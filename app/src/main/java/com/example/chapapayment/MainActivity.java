package com.example.chapapayment;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Button paymentChapa,respay;
    TextView clearFilters;
    ProgressBar progressBar;
    ImageView backBtn;
    String itemId;
    String deliveryDate;
    String proposalDescription;
    String price;
    String userId2;
    // Declare additional member variables for payment details
    private String paymentAmount;
    private String paymentFullName;
    private String paymentEmail;
    private String paymentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
        getIntentExtra();
        handleButtons();
    }
    private void initialization() {
        paymentChapa = findViewById(R.id.button);
        respay=findViewById(R.id.button2);
    }
    private void handleButtons() {
        paymentChapa.setOnClickListener(view -> {
            if (paymentChapa.getText().equals("Award")) {
                // awardProject();
                // payment();
                return;
            }
            // Prompt user for payment details
            showPaymentDetailsDialog();
        });
        respay.setOnClickListener(view -> {
            if (respay.getText().equals("Award")) {
                // awardProject();
                // payment();
                return;
            }
            // Prompt user for payment details
            showPaymentDetails();
        });


    }
    private void getIntentExtra() {
        Intent intent = getIntent();
        itemId = intent.getStringExtra("itemId");
        deliveryDate = intent.getStringExtra("deliveryDate");
        proposalDescription = intent.getStringExtra("proposalDescription");
        price = intent.getStringExtra("price");
        userId2 = intent.getStringExtra("userId2");
    }
    private void showPaymentDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Payment Details");

        // Inflate the layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment_details, null);
        builder.setView(dialogView);
        // Set up the input fields
        final EditText amountInput = dialogView.findViewById(R.id.amountInput);
        final EditText fullNameInput = dialogView.findViewById(R.id.fullNameInput);
        final EditText emailInput = dialogView.findViewById(R.id.emailInput);
        final EditText locationInput = dialogView.findViewById(R.id.locationInput);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            paymentAmount = amountInput.getText().toString();
            paymentFullName = fullNameInput.getText().toString();
            paymentEmail = emailInput.getText().toString();
            paymentLocation = locationInput.getText().toString();
            if (!paymentAmount.isEmpty()) {
                // Check if the payment location is valid before processing payment
                DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("Hotels reserved");
                Query query = bookingRef.orderByChild("Code").equalTo(paymentLocation);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // If a valid payment location is entered, proceed with payment
                            new NetworkT().execute(paymentAmount, paymentFullName, paymentEmail, paymentLocation);
                        } else {
                            // If the payment location is invalid, display an error message
                            Toast.makeText(MainActivity.this, "Invalid Code Reference Enter Again Please", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage());
                    }
                });
            }  else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    private class NetworkT extends AsyncTask<String, Void, String> {
        String txRef = generateRandomTxRe();

        @Override
        protected String doInBackground(String... details) {
            String amount = details[0];
            String fullName = details[1];
            String email = details[2];
            String location = details[3];
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{" +
                    "\"amount\":\"" + amount + "\"," +
                    "\"currency\": \"ETB\"," +
                    "\"phone_number\": \"0912345678\"," +
                    "\"tx_ref\": \"" + txRef + "\"," +
                    "\"callback_url\": \"https://webhook.site/077164d6-29cb-40df-ba29-8a00e59a7e60\"," +
                    "\"return_url\": \"https://www.google.com/\"," +
                    "\"customization[title]\": \"Payment for my favourite merchant\"," +
                    "\"customization[description]\": \"I love online payments\"," +
                    "\"customer[name]\": \"" + fullName + "\"," +
                    "\"customer[email]\": \"" + email + "\"," +
                    "\"customer[address]\": \"" + location + "\"" +
                    "}");
            Request request = new Request.Builder()
                    .url("https://api.chapa.co/v1/transaction/initialize")
                    .method("POST", body)
                    .addHeader("Authorization", "Bearer CHASECK_TEST-bxbjHdrcxOHPwqJqH8OPzeytJamQNzXB")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return "Error: " + response.code() + " - " + response.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Network error";
            }
        }

        private String generateRandomTxRe() {
            Random random = new Random();
            int randomNumber = 100000 + random.nextInt(900000);
            return "housepay-" + randomNumber;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("API Response:\n" + result);

            try {
                // Print the result string to see what's coming from the server
                System.out.println("Raw Response:\n" + result);

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(result);

                if ("success".equals(jsonResponse.optString("status"))) {
                    JSONObject data = jsonResponse.optJSONObject("data");

                    if (data != null) {
                        String checkoutUrl = data.optString("checkout_url");
                        System.out.println("Checkout URL: " + checkoutUrl);

                        // Save data to Firestore
                        saveToFirestor(paymentLocation);

                        // Open the Checkout URL
                        openCheckoutUr(checkoutUrl);
                    } else {
                        System.out.println("No data in the response");
                    }
                } else {
                    System.out.println("API response indicates failure");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("Error parsing JSON response");
            }
        }
        private void saveToFirestor(String paymentLocation) {
            DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("Hotels reserved");

            // Query the "booking" path to find the entry with the specified BookingReference
            Query query = bookingRef.orderByChild("Code").equalTo(paymentLocation);
            // Execute the query
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // If a matching entry is found, proceed to save the payment data
                        DatabaseReference paymentRef = FirebaseDatabase.getInstance().getReference("Reservation_Payment").child(paymentLocation);
                        Map<String, Object> paymentData = getPaymentDataMap();

                        // Save payment data to the specified location
                        paymentRef.setValue(paymentData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Data successfully saved to Firebase Realtime Database"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error saving data to Firebase Realtime Database", e));
                    } else {
                        // If no matching entry is found, display an error message
                        Toast.makeText(MainActivity.this, "Invalid payment location", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                }
            });
        }
        private Map<String, Object> getPaymentDataMap() {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("amount", paymentAmount);
            paymentData.put("fullName", paymentFullName);
            paymentData.put("email", paymentEmail);
            paymentData.put("BookingReference", paymentLocation);
            return paymentData;
        }
        private void openCheckoutUr(String checkoutUrl) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(checkoutUrl));
            startActivity(intent);
            finish();
        }
    }
    private void showPaymentDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Payment Details");

        // Inflate the layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_payment_details, null);
        builder.setView(dialogView);
        // Set up the input fields
        final EditText amountInput = dialogView.findViewById(R.id.amountInput);
        final EditText fullNameInput = dialogView.findViewById(R.id.fullNameInput);
        final EditText emailInput = dialogView.findViewById(R.id.emailInput);
        final EditText locationInput = dialogView.findViewById(R.id.locationInput);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            paymentAmount = amountInput.getText().toString();
            paymentFullName = fullNameInput.getText().toString();
            paymentEmail = emailInput.getText().toString();
            paymentLocation = locationInput.getText().toString();
            if (!paymentAmount.isEmpty()) {
                // Check if the payment location is valid before processing payment
                DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("booking");
                Query query = bookingRef.orderByChild("BookingReference").equalTo(paymentLocation);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // If a valid payment location is entered, proceed with payment
                            new NetworkTask().execute(paymentAmount, paymentFullName, paymentEmail, paymentLocation);
                        } else {
                            // If the payment location is invalid, display an error message
                            Toast.makeText(MainActivity.this, "Invalid Booking Reference Enter Again Please", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage());
                    }
                });
            }  else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    private class NetworkTask extends AsyncTask<String, Void, String> {
        String txRef = generateRandomTxRef();

        @Override
        protected String doInBackground(String... details) {
            String amount = details[0];
            String fullName = details[1];
            String email = details[2];
            String location = details[3];
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{" +
                    "\"amount\":\"" + amount + "\"," +
                    "\"currency\": \"ETB\"," +
                    "\"phone_number\": \"0912345678\"," +
                    "\"tx_ref\": \"" + txRef + "\"," +
                    "\"callback_url\": \"https://webhook.site/077164d6-29cb-40df-ba29-8a00e59a7e60\"," +
                    "\"return_url\": \"https://www.google.com/\"," +
                    "\"customization[title]\": \"Payment for my favourite merchant\"," +
                    "\"customization[description]\": \"I love online payments\"," +
                    "\"customer[name]\": \"" + fullName + "\"," +
                    "\"customer[email]\": \"" + email + "\"," +
                    "\"customer[address]\": \"" + location + "\"" +
                    "}");
            Request request = new Request.Builder()
                    .url("https://api.chapa.co/v1/transaction/initialize")
                    .method("POST", body)
                    .addHeader("Authorization", "Bearer CHASECK_TEST-bxbjHdrcxOHPwqJqH8OPzeytJamQNzXB")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return "Error: " + response.code() + " - " + response.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Network error";
            }
        }
        private String generateRandomTxRef() {
            Random random = new Random();
            int randomNumber = 100000 + random.nextInt(900000);
            return "housepay-" + randomNumber;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("API Response:\n" + result);

            try {
                // Print the result string to see what's coming from the server
                System.out.println("Raw Response:\n" + result);

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(result);

                if ("success".equals(jsonResponse.optString("status"))) {
                    JSONObject data = jsonResponse.optJSONObject("data");

                    if (data != null) {
                        String checkoutUrl = data.optString("checkout_url");
                        System.out.println("Checkout URL: " + checkoutUrl);

                        // Save data to Firestore
                        saveToFirestore(paymentLocation);

                        // Open the Checkout URL
                        openCheckoutUrl(checkoutUrl);
                    } else {
                        System.out.println("No data in the response");
                    }
                } else {
                    System.out.println("API response indicates failure");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("Error parsing JSON response");
            }
        }
        private void saveToFirestore(String paymentLocation) {
            DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("booking");

            // Query the "booking" path to find the entry with the specified BookingReference
            Query query = bookingRef.orderByChild("BookingReference").equalTo(paymentLocation);
            // Execute the query
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // If a matching entry is found, proceed to save the payment data
                        DatabaseReference paymentRef = FirebaseDatabase.getInstance().getReference("Booking_Payment").child(paymentLocation);
                        Map<String, Object> paymentData = getPaymentDataMap();

                        // Save payment data to the specified location
                        paymentRef.setValue(paymentData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Data successfully saved to Firebase Realtime Database"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error saving data to Firebase Realtime Database", e));
                    } else {
                        // If no matching entry is found, display an error message
                        Toast.makeText(MainActivity.this, "Invalid payment location", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                }
            });
        }

//        private void saveToFirestore(String paymentLocation) {
//            DatabaseReference db = FirebaseDatabase.getInstance().getReference("Payment").child(paymentLocation);
//            Map<String, Object> paymentData = getPaymentDataMap();
//            db.setValue(paymentData)
//                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Data successfully saved to Firebase Realtime Database"))
//                    .addOnFailureListener(e -> Log.e(TAG, "Error saving data to Firebase Realtime Database", e));
//        }

        //        private void saveToFirestore() {
//            DatabaseReference db= FirebaseDatabase.getInstance().getReference("Payment");
//            Map<String, Object> paymentData = getPaymentDataMap();
//            db.getRef().child("users").setValue(paymentData)
//                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Data successfully saved to Firebase Realtime Database"))
//                    .addOnFailureListener(e -> Log.e(TAG, "Error saving data to Firebase Realtime Database", e));
//        }
        private Map<String, Object> getPaymentDataMap() {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("amount", paymentAmount);
            paymentData.put("fullName", paymentFullName);
            paymentData.put("email", paymentEmail);
            paymentData.put("BookingReference", paymentLocation);
            return paymentData;
        }
        private Map<String, Object> getPaymentDataMap(String amount, String fullName, String email, String location) {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("amount", amount);
            paymentData.put("fullName", fullName);
            paymentData.put("email", email);
            paymentData.put("location", location);
            return paymentData;
        }
        private void openCheckoutUrl(String checkoutUrl) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(checkoutUrl));
            startActivity(intent);
            finish();
        }
    }
}
