package com.skapps.android.todolist;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.skapps.android.todolist.database.AppDatabase;
import com.skapps.android.todolist.database.TaskEntry;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;


public class MainActivity extends AppCompatActivity implements TaskAdapter.ItemClickListener, TaskAdapter.CheckBoxCheckListener, BillingProcessor.IBillingHandler{

    private String PRODUCT_ID = "com.skapps.android.todolist.id";

    private CheckPurchase mCheckPurchase;
    private RecyclerView mRecyclerView;
    private TaskAdapter mAdapter;
    private ProgressBar mprogressBar;
    private TextView mProgressValue;
    private TextView mEmptyView;
    private AdView mAdView;
    private LinearLayout adContainer;
    private ConstraintLayout mConstraintLayout;
    private BillingProcessor mBillingProcessor;


    private double mTotalProgressPercent;
    private AppDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCheckPurchase = new CheckPurchase(this);

        mprogressBar = findViewById(R.id.progressBar);
        mRecyclerView = findViewById(R.id.recyclerViewTasks);
        mProgressValue = findViewById(R.id.progressValue);
        mEmptyView = findViewById(R.id.emptyView);
        mConstraintLayout = findViewById(R.id.constraintLayout);
        adContainer = findViewById(R.id.adContainer);
        mAdView = findViewById(R.id.adView);

        if(!isConnected(this) || mCheckPurchase.isUserPurchased()){
            removeAds();

        } else{

            adContainer.setVisibility(View.VISIBLE);
            mBillingProcessor = new BillingProcessor(this, AppConfig.LICENSE_KEY, this);


            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mConstraintLayout.getLayoutParams();
            int pxValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, this.getResources().getDisplayMetrics());
            params.bottomMargin = pxValue;

            List<String> testDevices = new ArrayList<>();
            testDevices.add(AdRequest.DEVICE_ID_EMULATOR);
            RequestConfiguration requestConfiguration
                    = new RequestConfiguration.Builder()
                    .setTestDeviceIds(testDevices)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);

            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

        }


        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new TaskAdapter(this, this, this);
        mRecyclerView.setAdapter(mAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        mDb = AppDatabase.getInstance(getApplicationContext());


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int position_dragged = viewHolder.getAdapterPosition();
                int position_target = target.getAdapterPosition();

                Collections.swap(mAdapter.getTasks(), position_dragged, position_target);
                mAdapter.notifyItemMoved(position_dragged, position_target);
                return false;
            }


            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        int position = viewHolder.getAdapterPosition();
                        List<TaskEntry> tasks =  mAdapter.getTasks();
                        mDb.taskDao().deleteTask(tasks.get(position));
                    }
                });
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

        if(mCheckPurchase.isUserPurchased()){
            MenuItem item = menu.findItem(R.id.remove_ads);
            item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_all_tasks:
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        mDb.taskDao().deleteAll();
                    }
                });
                return true;
            case R.id.remove_ads:
                if(isConnected(this)){
                    if(mBillingProcessor == null){
                        mBillingProcessor = new BillingProcessor(this, AppConfig.LICENSE_KEY, this);
                        mBillingProcessor.purchase(this, PRODUCT_ID);
                    }else{
                        mBillingProcessor.purchase(this, PRODUCT_ID);
                    }
                }else{
                    Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_SHORT).show();
                }

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

    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiInfo != null && wifiInfo.isConnected()) || (mobileInfo != null && mobileInfo.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void removeAds() {
        adContainer.setVisibility(View.GONE);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mConstraintLayout.getLayoutParams();
        params.bottomMargin = 0;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(!mBillingProcessor.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        if (mBillingProcessor != null) {
            mBillingProcessor.release();
        }
        super.onDestroy();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if(productId.equals(PRODUCT_ID)){
            mCheckPurchase.setUserPurchased(true);
            Toast.makeText(MainActivity.this, "Your purchase was successful. Ads Removed.", Toast.LENGTH_LONG).show();
            removeAds();
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(MainActivity.this, "Purchase was unsuccessful.", Toast.LENGTH_LONG).show();
        mCheckPurchase.setUserPurchased(false );
    }

    @Override
    public void onBillingInitialized() {

    }

}
