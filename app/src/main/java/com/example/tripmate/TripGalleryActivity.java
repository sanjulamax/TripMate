package com.example.tripmate;



import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import okhttp3.*;

import java.io.*;
import java.util.*;

public class TripGalleryActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private String tripId;
    private Button btnUpload;
    private RecyclerView recyclerGallery;
    private TripGalleryAdapter adapter;
    private List<String> imageUrls = new ArrayList<>();

    private FirebaseFirestore db;

    // TODO: Replace with your Cloudinary details
    private final String CLOUD_NAME = "dmgrespaq";
    private final String UPLOAD_PRESET = "TripMate";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_gallery);

        tripId = getIntent().getStringExtra("tripId");
        if (tripId == null) {
            Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnUpload = findViewById(R.id.btnUpload);
        recyclerGallery = findViewById(R.id.recyclerGallery);

        recyclerGallery.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new TripGalleryAdapter(this, imageUrls);
        recyclerGallery.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        btnUpload.setOnClickListener(view -> showImagePickerDialog());

        fetchImages();
    }

    private void showImagePickerDialog() {
        String[] options = {"Select from Gallery", "Take Photo"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Upload Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else {
                        takePhotoWithCamera();
                    }
                }).show();
    }

    private void pickImageFromGallery() {
        if (!checkStoragePermission()) return;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void takePhotoWithCamera() {
        if (!checkCameraPermission()) return;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureImageLauncher.launch(intent);
    }

    // Permission checks and requests
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                return false;
            }
        }
        return true;
    }

    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1002);
                return false;
            }
        }
        return true;
    }

    // ActivityResultLaunchers
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadImageToCloudinary(selectedImageUri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> captureImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        Uri imageUri = bitmapToUri(photo);
                        if (imageUri != null) {
                            uploadImageToCloudinary(imageUri);
                        } else {
                            Toast.makeText(this, "Failed to get photo URI", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private Uri bitmapToUri(Bitmap bitmap) {
        File imagesFolder = new File(getCacheDir(), "images");
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "captured_photo_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = getBytes(inputStream);

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "image.jpg",
                                RequestBody.create(imageBytes, MediaType.parse("image/*")))
                        .addFormDataPart("upload_preset", UPLOAD_PRESET)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Parse URL from Cloudinary response
                    String url = new org.json.JSONObject(responseBody).getString("secure_url");
                    runOnUiThread(() -> {
                        saveImageUrlToFirestore(url);
                        Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Cloudinary upload failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void saveImageUrlToFirestore(String url) {
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("url", url);
        imageData.put("timestamp", new Date());
        db.collection("trips").document(tripId).collection("gallery").add(imageData)
                .addOnSuccessListener(documentReference -> fetchImages());
    }

    private void fetchImages() {
        db.collection("trips").document(tripId).collection("gallery")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        imageUrls.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String url = doc.getString("url");
                            if (url != null) imageUrls.add(url);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else if (requestCode == 1002 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoWithCamera();
        }
    }
}