package com.ginige.ashan.notes.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ginige.ashan.notes.Models.NoteDetails;
import com.ginige.ashan.notes.NoteDetailsActivity;
import com.ginige.ashan.notes.R;
import com.ginige.ashan.notes.Util.Constants;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private Context context;
    private List<NoteDetails> note;
    private DatabaseReference notesDatabaseRef;

    public NotesAdapter(Context context, List<NoteDetails> note, DatabaseReference notesDatabaseRef) {
        this.context = context;
        this.note = note;
        this.notesDatabaseRef = notesDatabaseRef;
    }


    @Override
    public NotesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_note_block, parent, false);
        return new ViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(final NotesAdapter.ViewHolder holder, final int position) {
        holder.noteTitle.setText(note.get(position).getTitle());
        holder.noteBody.setText(note.get(position).getDate()+" - "+note.get(position).getBody());
        holder.noteBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, NoteDetailsActivity.class)
                        .putExtra(Constants.NOTE_PROCESS, Constants.EDIT_NOTE_VALUE)
                        .putExtra(Constants.NOTE_ID_KEY, note.get(position).getNoteID()));
            }
        });

        holder.noteBlock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                holder.noteBlock.setBackgroundColor(context.getColor(R.color.colorAccent));
                holder.noteBody.setTextColor(context.getColor(R.color.textDeleteColor));
                holder.noteTitle.setTextColor(context.getColor(R.color.textDeleteColor));
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        context);
                alert.setCancelable(false);
                alert.setTitle("Delete");
                alert.setMessage("Are you sure that you want to delete this note?");
                alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notesDatabaseRef.child(note.get(position).getNoteID()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                dataSnapshot.getRef().setValue(null);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e("The read failed: " ,databaseError.getMessage());
                            }
                        });
                        dialog.dismiss();
                        holder.noteBlock.setBackgroundColor(Color.TRANSPARENT);
                        holder.noteBody.setTextColor(context.getColor(R.color.noteBodyHintColor));
                        holder.noteTitle.setTextColor(context.getColor(R.color.noteTitleTextColor));

                    }
                });
                alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        holder.noteBlock.setBackgroundColor(Color.TRANSPARENT);
                        holder.noteBody.setTextColor(context.getColor(R.color.noteBodyHintColor));
                        holder.noteTitle.setTextColor(context.getColor(R.color.noteTitleTextColor));
                    }
                });
                alert.show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return note.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView noteTitle;
        public TextView noteBody;
        public ConstraintLayout noteBlock;




        public ViewHolder(View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteAdapterBlockTitle);
            noteBody = itemView.findViewById(R.id.noteAdapterBlockBody);
            noteBlock = itemView.findViewById(R.id.noteBlock);

        }

    }

}
