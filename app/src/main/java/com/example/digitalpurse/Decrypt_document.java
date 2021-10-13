package com.example.digitalpurse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class Decrypt_document extends AppCompatActivity {
    TextView file_name,Decrypt,Delete;
    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    ProgressBar progressBar2;
    int key = 99;
    Uri imageLocationPath;
    StorageReference objReference;
    OutputStream outputStream;
    InputStream inputStream;
    StorageReference mStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_document);

        file_name = findViewById(R.id.file_name);
        Decrypt = findViewById(R.id.Decrypt);
        progressBar2 = findViewById(R.id.progressBar2);
        Delete = findViewById(R.id.Delete);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        objReference = FirebaseStorage.getInstance().getReference("ImageFolder");

        String Name = getIntent().getStringExtra("keyName");

        file_name.setText(Name);

        Delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String id = fAuth.getCurrentUser().getUid();

                fStore.collection("Purse").document(id).collection("Images").document(Name).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String fileName = documentSnapshot.getString("name");
                                String ext = documentSnapshot.getString("ext");
                                String url = documentSnapshot.getString("url");

                                deletefile(url,fileName);

                            }
                        });


            }
        });

        Decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                progressBar2.setVisibility(View.VISIBLE);

                String id = fAuth.getCurrentUser().getUid();

                fStore.collection("Purse").document(id).collection("Images").document(Name).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String fileName = documentSnapshot.getString("name");
                                String ext = documentSnapshot.getString("ext");
                                String url = documentSnapshot.getString("url");

                                downloadFile(getApplicationContext(),fileName,"."+ext,DIRECTORY_DOWNLOADS,url);
                            }
                        });
            }
        });


    }

    public void downloadFile(Context context,String fileName,String fileExtension, String destinationDirectory,String url){



        Uri uri = Uri.parse(url);

        try {
            imageLocationPath = uri;
            System.out.println(imageLocationPath);

            String id = fAuth.getCurrentUser().getUid();

            mStorageReference = FirebaseStorage.getInstance().getReference().child("ImageFolder/"+fileName+id+fileExtension);
            File localFile = File.createTempFile(fileName,fileExtension,getCacheDir());
            mStorageReference.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Log.d("DOWN","LOADED" + localFile);
                            try {
                                inputStream = new FileInputStream(localFile);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            byte[] data = new byte[0];
                            try {
                                data = new byte[inputStream.available()];
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                inputStream.read(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            int i = 0;
                            for (byte b : data) {
//                                System.out.println(b);
                                data[i] = (byte) (b ^ key);
                                i++;
                            }
                            try {
                                outputStream = new FileOutputStream(localFile);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            try {
                                outputStream.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            imageLocationPath = FileProvider.getUriForFile(Decrypt_document.this, BuildConfig.APPLICATION_ID + ".provider",localFile);
                            System.out.println(imageLocationPath);

                            StorageReference imgRef = objReference.child(id+fileExtension);
                            objReference.child(id).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Toast.makeText(getApplicationContext(), "File name already Exists! Upload Fail", Toast.LENGTH_SHORT).show();
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
                                            String url = task.getResult().toString();

                                            Uri imagepath = Uri.parse(url);

                                            DownloadManager downloadManager = (DownloadManager) context.
                                            getSystemService(Context.DOWNLOAD_SERVICE);

                                            DownloadManager.Request request = new DownloadManager.Request(imagepath);

                                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                            request.setDestinationInExternalFilesDir(context,destinationDirectory,fileName + fileExtension);

                                            downloadManager.enqueue((request));

                                            progressBar2.setVisibility(View.INVISIBLE);
                                            Toast.makeText(getApplicationContext(), "Decrypted Successfully", Toast.LENGTH_SHORT).show();

                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    deletedata(url);
                                                }
                                            }, 10000);

                                        }
                                    });
                                }
                            });

                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("fail", "onFailure: ");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void deletedata(String url){


        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReferenceFromUrl(url);
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e("Picture","#deleted");
            }
        });

    }

    public void deletefile(String url,String filName){

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReferenceFromUrl(url);
        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e("Picture","#deleted");
            }
        });

        String id = fAuth.getCurrentUser().getUid();

        fStore.collection("Purse").document(id).collection("Images").document(filName)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Deleted Successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(),Document_List.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error !", Toast.LENGTH_SHORT).show();
            }
        });



    }


}