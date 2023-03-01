package com.skapps.android.todolist;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.skapps.android.todolist.database.AppDatabase;
import com.skapps.android.todolist.database.TaskEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;


public class MainActivity extends AppCompatActivity implements TaskAdapter.ItemClickListener, TaskAdapter.CheckBoxCheckListener{

    private RecyclerView mRecyclerView;
    private TaskAdapter mAdapter;
    private ProgressBar mprogressBar;
    private TextView mProgressValue;
    private TextView mEmptyView;
    private ConstraintLayout mConstraintLayout;


    private double mTotalProgressPercent;
    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mprogressBar = findViewById(R.id.progressBar);
        mRecyclerView = findViewById(R.id.recyclerViewTasks);
        mProgressValue = findViewById(R.id.progressValue);
        mEmptyView = findViewById(R.id.emptyView);
        mConstraintLayout = findViewById(R.id.constraintLayout);


        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new TaskAdapter(this, this, this);
        mRecyclerView.setAdapter(mAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        mDb = AppDatabase.getInstance(getApplicationContext());

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mConstraintLayout.getLayoutParams();
        params.bottomMargin = 0;


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            boolean drag = false;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int position_dragged = viewHolder.getAdapterPosition();
                int position_target = target.getAdapterPosition();

                Collections.swap(mAdapter.getTasks(), position_dragged, position_target);
                mAdapter.notifyItemMoved(position_dragged, position_target);

                return false;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    drag = true;
                }

                if(actionState == ItemTouchHelper.ACTION_STATE_IDLE && drag) {
                    Log.d("DragTest","DRAGGGING stop");
                    drag= false;

                    final List<TaskEntry> NewTasks =  mAdapter.getTasks();
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mDb.taskDao().deleteAll();

                                for(int i =0; i < NewTasks.size(); i++){
                                    TaskEntry task = NewTasks.get(i);
                                    mDb.taskDao().insertTask(new TaskEntry(
                                            task.getDescription(),
                                            task.getPriority(),
                                            task.getUpdatedAt(),
                                            task.isChecked()
                                    ));
                                }
                            }catch (Exception ignored){}

                        }
                    });
                }
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                List<TaskEntry> tasks =  mAdapter.getTasks();
                final TaskEntry taskToBeDeleted = tasks.get(position);

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mDb.taskDao().deleteTask(taskToBeDeleted);
                        }catch (Exception ignored){}
                    }
                });

                Snackbar snackbar = Snackbar
                        .make(viewHolder.itemView, "Task deleted!", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppExecutors.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mDb.taskDao().insertTask(taskToBeDeleted);
                                }catch (Exception ignored){}
                            }
                        });

                    }
                }).show();
            }
        }).attachToRecyclerView(mRecyclerView);


        FloatingActionButton fabButton = findViewById(R.id.fab);

        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent addTaskIntent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(addTaskIntent);
            }
        });
        setupViewModel();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_all_tasks:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Delete all tasks")
                        .setPositiveButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDb.taskDao().deleteAll();
                                    }
                                });
                            }
                        })
                        .setMessage( "Do you want to delete all tasks?")
                        .show();
                break;
            case R.id.uncheck_all:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Reset task list")
                        .setPositiveButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        for(TaskEntry taskEntry: mAdapter.getTasks()){
                                            taskEntry.setChecked(false);
                                            mDb.taskDao().updateTask(taskEntry);
                                        }
                                    }
                                });
                            }
                        })
                        .setMessage( "Do you want to un-check all tasks?")
                        .show();
                break;
            case R.id.sort_by_priority:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Sort tasks")
                        .setPositiveButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final List<TaskEntry> tasks =  mAdapter.getTasks();
                                            mDb.taskDao().deleteAll();
                                            Collections.sort(tasks, new Comparator<TaskEntry>(){
                                                public int compare(TaskEntry o1, TaskEntry o2){
                                                    return o1.getPriority() - o2.getPriority();
                                                }
                                            });

                                            for(int i =0; i < tasks.size(); i++){
                                                TaskEntry task = tasks.get(i);
                                                mDb.taskDao().insertTask(new TaskEntry(
                                                        task.getDescription(),
                                                        task.getPriority(),
                                                        task.getUpdatedAt(),
                                                        task.isChecked()
                                                ));
                                            }
                                        }catch (Exception ignored){}
                                    }
                                });
                            }
                        })
                        .setMessage( "Do you want to sort list by priority?")
                        .show();
                break;
            case R.id.sort_by_check:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Show pending tasks first")
                        .setPositiveButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final List<TaskEntry> tasks =  mAdapter.getTasks();
                                            mDb.taskDao().deleteAll();

                                            Collections.sort(tasks, new Comparator<TaskEntry>() {
                                                @Override
                                                public int compare(TaskEntry o1, TaskEntry o2) {
                                                    return Boolean.compare(o1.isChecked(), o2.isChecked());
                                                }
                                            });


                                            for(int i =0; i < tasks.size(); i++){
                                                TaskEntry task = tasks.get(i);
                                                mDb.taskDao().insertTask(new TaskEntry(
                                                        task.getDescription(),
                                                        task.getPriority(),
                                                        task.getUpdatedAt(),
                                                        task.isChecked()
                                                ));
                                            }
                                        }catch (Exception ignored){}
                                    }
                                });
                            }
                        })
                        .setMessage( "Do you want to sort list to show pending tasks first?")
                        .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClickListener(int itemId) {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        intent.putExtra(AddTaskActivity.EXTRA_TASK_ID, itemId);
        startActivity(intent);
    }

    @Override
    public void onCheckBoxCheckListener(final TaskEntry taskEntry) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taskDao().updateTask(taskEntry);
            }
        });
    }


    private void setupViewModel() {
        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getTasks().observe(this, new Observer<List<TaskEntry>>() {
            @Override
            public void onChanged(List<TaskEntry> taskEntries) { //runs on main thread


                if(taskEntries.isEmpty()){
                    mprogressBar.setVisibility(View.INVISIBLE);
                    mProgressValue.setVisibility(View.INVISIBLE);
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }else {
                    mprogressBar.setVisibility(View.VISIBLE);
                    mProgressValue.setVisibility(View.VISIBLE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }

                calculatePercent(taskEntries);
                mprogressBar.setProgress((int)mTotalProgressPercent);
                mProgressValue.setText((int)mTotalProgressPercent + " %");

                mAdapter.setTasks(taskEntries);
            }
        });
    }

    private void calculatePercent(List<TaskEntry> taskEntries) {
        int countChecked = 0;
        for(TaskEntry i: taskEntries){
            if(i.isChecked()) countChecked++;
        }
        mTotalProgressPercent = (double)countChecked/taskEntries.size() *100;
    }
}
