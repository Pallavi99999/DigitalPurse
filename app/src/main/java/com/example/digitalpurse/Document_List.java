package com.example.digitalpurse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Document_List extends AppCompatActivity {
    ImageView back_button;
    ImageView home_button, list_document, profile;
    RecyclerView recyclerView;
    ArrayList<model_doc> model_docArrayList;
    Adapter_doc adapter_doc;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_list);

        back_button = findViewById(R.id.back_button);
        home_button = findViewById(R.id.home_button);
        list_document = findViewById(R.id.list_document);
        profile = findViewById(R.id.profile);
        recyclerView = findViewById(R.id.recyclerview);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        model_docArrayList = new ArrayList<>();
        adapter_doc = new Adapter_doc(model_docArrayList);
        recyclerView.setAdapter(adapter_doc);

        adapter_doc.setOnItemClickListener(new Adapter_doc.onItemClickListener() {
            @Override
            public void onArrowClick(int position) {
                ArrowClick(position);
            }
        });

        String id = fAuth.getCurrentUser().getUid();

        fStore.collection("Purse").document(id).collection("Images").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                        for(DocumentSnapshot d:list){
                            String documentId = d.getId();
                            model_doc obj = d.toObject(model_doc.class);
                            obj.setDocumentId(documentId);
                            model_docArrayList.add(obj);
                        }
                        adapter_doc.notifyDataSetChanged();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });


        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Home.class));
            }
        });

        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Home.class));
            }
        });

        list_document.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Profile_User.class));
            }
        });


    }

    public void ArrowClick(int position){

        String id = fAuth.getCurrentUser().getUid();

        fStore.collection("Purse").document(id).collection("Images").document(model_docArrayList.get(position).getDocumentId()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String name = documentSnapshot.getString("name");
                        Intent intent = new Intent(Document_List.this,Decrypt_document.class);
                        intent.putExtra("keyName",name);
                        startActivity(intent);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("arrowclick","something went wrong" + e);
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(),Home.class));
        finish();
    }
}