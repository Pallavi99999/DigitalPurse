package com.example.digitalpurse;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Adapter_doc extends RecyclerView.Adapter<Adapter_doc.myviewholder>{

    private onItemClickListener mListener;
    ArrayList<model_doc> model_docArrayList;

    public interface onItemClickListener{
        void onArrowClick(int position);
    }

    public void setOnItemClickListener(onItemClickListener listener){
        mListener = listener;
    }

    public Adapter_doc(ArrayList<model_doc> model_docArrayList) {
        this.model_docArrayList = model_docArrayList;
    }

    @NonNull
    @Override
    public myviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.document_item,parent,false);
        return new myviewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myviewholder holder, int position) {
        holder.document_name.setText(model_docArrayList.get(position).getName());

    }

    @Override
    public int getItemCount() {
        return model_docArrayList.size();
    }

    class myviewholder extends RecyclerView.ViewHolder{
        TextView document_name;
        ImageView arrow;

        public myviewholder(@NonNull View itemView) {
            super(itemView);
            document_name = itemView.findViewById(R.id.document_name);
            arrow = itemView.findViewById(R.id.arrow);

            arrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mListener!=null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            mListener.onArrowClick(position);
                        }
                    }else{
                        Log.d("arrow","gadbad");
                    }
                }
            });

        }
    }
}
