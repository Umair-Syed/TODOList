

package com.skapps.android.todolist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.skapps.android.todolist.database.TaskEntry;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {


    private static final String DATE_FORMAT = "dd/MM/yyy";

    final private ItemClickListener mItemClickListener;
    final private CheckBoxCheckListener mCheckBoxCheckListener;
    // Class variables for the List that holds task data and the Context
    private List<TaskEntry> mTaskEntries;
    private Context mContext;

    // Date formatter
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    public TaskAdapter(Context context, ItemClickListener listener, CheckBoxCheckListener mCheckBoxCheckListener) {
        mContext = context;
        mItemClickListener = listener;
        this.mCheckBoxCheckListener = mCheckBoxCheckListener;
    }


    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.task_layout, parent, false);

        return new TaskViewHolder(view);
    }


    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        // Determine the values of the wanted data
        TaskEntry taskEntry = mTaskEntries.get(position);
        String description = taskEntry.getDescription();
        int priority = taskEntry.getPriority();
        String updatedAt = dateFormat.format(taskEntry.getUpdatedAt());


        holder.taskDescriptionView.setText(description);
        holder.updatedAtView.setText(updatedAt);

        holder.priorityView.setText(Integer.toString(position+1));


        holder.checkBox.setChecked(taskEntry.isChecked());

        if(taskEntry.isChecked()){
            holder.taskDescriptionView.setBackgroundResource(R.drawable.strike_through);
            holder.taskDescriptionView.setTextColor(Color.GRAY);

        }else {
            holder.taskDescriptionView.setBackgroundResource(0);
            holder.taskDescriptionView.setTextColor(ContextCompat.getColor(mContext, R.color.list_item_text_color));


        }


        GradientDrawable priorityCircle = (GradientDrawable) holder.priorityView.getBackground();

        int priorityColor = getPriorityColor(priority);
        priorityCircle.setColor(priorityColor);


    }

    /*
    Helper method for selecting the correct priority circle color.
    P1 = red, P2 = orange, P3 = yellow
    */
    private int getPriorityColor(int priority) {
        int priorityColor = 0;

        switch (priority) {
            case 1:
                priorityColor = ContextCompat.getColor(mContext, R.color.materialRed);
                break;
            case 2:
                priorityColor = ContextCompat.getColor(mContext, R.color.materialOrange);
                break;
            case 3:
                priorityColor = ContextCompat.getColor(mContext, R.color.materialYellow);
                break;
            default:
                break;
        }
        return priorityColor;
    }


    @Override
    public int getItemCount() {
        if (mTaskEntries == null) {
            return 0;
        }
        return mTaskEntries.size();
    }

    public List<TaskEntry> getTasks() {
        return mTaskEntries;
    }



    public void setTasks(List<TaskEntry> taskEntries) {
        mTaskEntries = taskEntries;
        notifyDataSetChanged();
    }



    public interface ItemClickListener {
        void onItemClickListener(int itemId);
    }

    public interface CheckBoxCheckListener {
        void onCheckBoxCheckListener(TaskEntry taskEntry);
    }


    // Inner class for creating ViewHolders
    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


        TextView taskDescriptionView;
        TextView updatedAtView;
        TextView priorityView;
        CheckBox checkBox;


        public TaskViewHolder(View itemView) {
            super(itemView);

            taskDescriptionView = itemView.findViewById(R.id.taskDescription);
            updatedAtView = itemView.findViewById(R.id.taskUpdatedAt);
            priorityView = itemView.findViewById(R.id.priorityTextView);
            checkBox = itemView.findViewById(R.id.checkBox);
            itemView.setOnClickListener(this);

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mTaskEntries.get(getAdapterPosition()).isChecked()){
                        mTaskEntries.get(getAdapterPosition()).setChecked(false);
                    }else {
                        mTaskEntries.get(getAdapterPosition()).setChecked(true);
                    }
                    Log.d("adpatercheck", "is checked  " + mTaskEntries.get(getAdapterPosition()).isChecked());
                    mCheckBoxCheckListener.onCheckBoxCheckListener(mTaskEntries.get(getAdapterPosition()));
                }
            });

        }

        @Override
        public void onClick(View view) {
            int elementId = mTaskEntries.get(getAdapterPosition()).getId();
            mItemClickListener.onItemClickListener(elementId);
        }

    }
}