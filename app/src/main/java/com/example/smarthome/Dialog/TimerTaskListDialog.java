package com.example.smarthome.Dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.Adapter.TimerTaskAdapter;
import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.R;
import com.example.smarthome.Service.TimerTaskManager;

import java.util.ArrayList;
import java.util.List;

public class TimerTaskListDialog {
    
    private final Context context;
    private final TimerTaskManager taskManager;
    private AlertDialog dialog;
    
    private RecyclerView rvTimerTasks;
    private TextView tvEmptyHint;
    private Button btnAddTask;
    private Button btnClose;
    private TimerTaskAdapter adapter;
    
    public TimerTaskListDialog(Context context) {
        this.context = context;
        this.taskManager = TimerTaskManager.getInstance(context);
    }
    
    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_timer_task_list, null);
        
        initViews(view);
        setupRecyclerView();
        loadTasks();
        setupButtons();
        
        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();
        
        dialog.show();
    }
    
    private void initViews(View view) {
        rvTimerTasks = view.findViewById(R.id.rv_timer_tasks);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        btnAddTask = view.findViewById(R.id.btn_add_task);
        btnClose = view.findViewById(R.id.btn_close);
    }
    
    private void setupRecyclerView() {
        adapter = new TimerTaskAdapter();
        rvTimerTasks.setLayoutManager(new LinearLayoutManager(context));
        rvTimerTasks.setAdapter(adapter);
        
        adapter.setOnTaskActionListener(new TimerTaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskEnabledChanged(String taskId, boolean enabled) {
                taskManager.toggleTaskEnabled(taskId, enabled);
                loadTasks();
            }
            
            @Override
            public void onTaskDeleted(String taskId) {
                taskManager.deleteTask(taskId);
                loadTasks();
            }
        });
    }
    
    private void loadTasks() {
        List<DeviceTimerTask> robotTasks = taskManager.getTasksForDevice(DeviceTimerTask.DEVICE_ROBOT);
        List<DeviceTimerTask> dehumidifierTasks = taskManager.getTasksForDevice(DeviceTimerTask.DEVICE_DEHUMIDIFIER);
        
        List<DeviceTimerTask> allTasks = new ArrayList<>();
        allTasks.addAll(robotTasks);
        allTasks.addAll(dehumidifierTasks);
        
        allTasks.sort((t1, t2) -> Long.compare(t1.getStartTimeMillis(), t2.getStartTimeMillis()));
        
        adapter.setTasks(allTasks);
        
        if (allTasks.isEmpty()) {
            rvTimerTasks.setVisibility(View.GONE);
            tvEmptyHint.setVisibility(View.VISIBLE);
        } else {
            rvTimerTasks.setVisibility(View.VISIBLE);
            tvEmptyHint.setVisibility(View.GONE);
        }
    }
    
    private void setupButtons() {
        btnAddTask.setOnClickListener(v -> {
            TimerTaskDialog addDialog = new TimerTaskDialog(context, () -> {
                loadTasks();
            });
            addDialog.show();
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
    }
}
