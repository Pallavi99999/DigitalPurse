package com.example.digitalpurse;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.protobuf.Value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class Home extends AppCompatActivity {

    EditText file_name;
    ImageView home_button, list_document, profile;
    TextView upload_document;
    StorageReference objReference;
    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    ProgressBar progress;
    File file;
    String extension;

    Uri imageLocationPath;

    ActivityResultLauncher galleryLauncher;

    int key = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        home_button = findViewById(R.id.home_button);
        list_document = findViewById(R.id.list_document);
        profile = findViewById(R.id.profile);
        upload_document = findViewById(R.id.upload_document);
        file_name = findViewById(R.id.file_name);
        progress = findViewById(R.id.progress);

        objReference = FirebaseStorage.getInstance().getReference("ImageFolder");
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        list_document.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Document_List.class));
            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Profile_User.class));
            }
        });



galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {

                try {
                    imageLocationPath = result;
                    System.out.println(imageLocationPath);

                    extension = getExtension(imageLocationPath);

                    try {
                        file = FileUtil.from(Home.this,imageLocationPath);
                        Log.d("file", "File...:::: uti - "+file .getPath()+" file -" + file + " : " + file .exists());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    InputStream inputStream = new FileInputStream(file);
                    byte[] data = new byte[inputStream.available()];
                    inputStream.read(data);
                    int i = 0;
                    for (byte b : data) {
                        data[i] = (byte) (b ^ key);
                        i++;
                    }

                    OutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(data);
                    imageLocationPath = FileProvider.getUriForFile(Home.this, BuildConfig.APPLICATION_ID + ".provider",file);
                    outputStream.close();
                    inputStream.close();


                } catch (Exception e) {
                    e.printStackTrace();
                }

                upload_document.setText("Image Selected Successfully");
            }
        }
);

        upload_document.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryLauncher.launch("image/*");
            }
        });

    }

    public static void operate() {

    }


    public void uploadImage(View view) {

        progress.setVisibility(View.VISIBLE);

        try {

            if (!file_name.getText().toString().isEmpty() && imageLocationPath != null) {
                String nameOfImage = file_name.getText().toString();

                String id = fAuth.getCurrentUser().getUid();

                StorageReference imgRef = objReference.child(nameOfImage+id+"." +extension);

                objReference.child(nameOfImage+id+"." +extension).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Toast.makeText(getApplicationContext(), "File name already Exists! Upload Fail", Toast.LENGTH_SHORT).show();
                        progress.setVisibility(View.INVISIBLE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        UploadTask uploadTask = imgRef.putFile(imageLocationPath);
                        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return imgRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {

                                    String name = file_name.getText().toString();
                                    String url = task.getResult().toString();

                                    Map<String, String> objectMap = new HashMap<>();
                                    objectMap.put("url", task.getResult().toString());
                                    objectMap.put("name", file_name.getText().toString());
                                    objectMap.put("ext",extension);

                                    fStore.collection("Purse").document(fAuth.getCurrentUser().getUid()).collection("Images").document(name)
                                            .set(objectMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(getApplicationContext(), "Encrypted Successfully", Toast.LENGTH_SHORT).show();
                                                    progress.setVisibility(View.INVISIBLE);
                                                    file_name.getText().clear();
                                                    upload_document.setText("Upload Image");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getApplicationContext(), "File Name Already Exists Upload Fail", Toast.LENGTH_SHORT).show();
                                            progress.setVisibility(View.INVISIBLE);
                                        }
                                    });

                                } else if (!task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

            } else {
                Toast.makeText(getApplicationContext(), "Please fill all blocks", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getExtension(Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

            return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return null;
    }




    private String getPath( Uri uri )
    {
        File backupFile = new File( uri.getPath() );
        String absolutePath = backupFile.getAbsolutePath();
        return absolutePath.substring( absolutePath.indexOf( ':' ) + 1 );
    }

}