package com.example.android.todolist;

import com.example.android.todolist.database.AppDatabase;
import com.example.android.todolist.database.TaskEntry;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

class AddTaskViewModel extends ViewModel {

    private LiveData<TaskEntry> task;


    public AddTaskViewModel(AppDatabase database, int taskId) {
        task =  database.taskDao().loadTaskById(taskId);
    }

    public LiveData<TaskEntry> getTask() {
        return task;
    }
}
