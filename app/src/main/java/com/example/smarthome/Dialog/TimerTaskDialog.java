package com.example.smarthome.Dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.R;
import com.example.smarthome.Service.TimerTaskManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimerTaskDialog {
    
    private final Context context;
    private final TimerTaskManager taskManager;
    private final OnTaskCreatedListener listener;
    private AlertDialog dialog;
    
    private RadioGroup rgDevice;
    private RadioButton rbRobot;
    private RadioButton rbDehumidifier;
    private Spinner spinnerDate;
    private Spinner spinnerHour;
    private Spinner spinnerMinute;
    private SeekBar seekbarDuration;
    private TextView tvDurationDisplay;
    private Button btnCancel;
    private Button btnConfirm;
    
    private List<Long> dateTimestamps = new ArrayList<>();
    
    public interface OnTaskCreatedListener {
        void onTaskCreated();
    }
    
    public TimerTaskDialog(Context context, OnTaskCreatedListener listener) {
        this.context = context;
        this.taskManager = TimerTaskManager.getInstance(context);
        this.listener = listener;
    }
    
    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_timer_task, null);
        
        initViews(view);
        setupDateSpinner();
        setupTimeSpinners();
        setupDurationSeekBar();
        setupButtons();
        
        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();
        
        dialog.show();
    }
    
    private void initViews(View view) {
        rgDevice = view.findViewById(R.id.rg_device);
        rbRobot = view.findViewById(R.id.rb_robot);
        rbDehumidifier = view.findViewById(R.id.rb_dehumidifier);
        spinnerDate = view.findViewById(R.id.spinner_date);
        spinnerHour = view.findViewById(R.id.spinner_hour);
        spinnerMinute = view.findViewById(R.id.spinner_minute);
        seekbarDuration = view.findViewById(R.id.seekbar_duration);
        tvDurationDisplay = view.findViewById(R.id.tv_duration_display);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnConfirm = view.findViewById(R.id.btn_confirm);
    }
    
    private void setupDateSpinner() {
        dateTimestamps.clear();
        List<String> dateLabels = new ArrayList<>();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 (E)", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 0; i < 3; i++) {
            dateTimestamps.add(calendar.getTimeInMillis());
            dateLabels.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, dateLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(adapter);
    }
    
    private void setupTimeSpinners() {
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format(Locale.getDefault(), "%02d", i));
        }
        
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, hours);
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHour.setAdapter(hourAdapter);
        
        Calendar now = Calendar.getInstance();
        spinnerHour.setSelection(now.get(Calendar.HOUR_OF_DAY));
        
        List<String> minutes = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format(Locale.getDefault(), "%02d", i));
        }
        
        ArrayAdapter<String> minuteAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, minutes);
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMinute.setAdapter(minuteAdapter);
        
        spinnerMinute.setSelection(now.get(Calendar.MINUTE));
    }
    
    private void setupDurationSeekBar() {
        seekbarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDurationDisplay(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        updateDurationDisplay(seekbarDuration.getProgress());
    }
    
    private void updateDurationDisplay(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (mins == 0) {
                tvDurationDisplay.setText(hours + "小时");
            } else {
                tvDurationDisplay.setText(hours + "小时" + mins + "分钟");
            }
        } else {
            tvDurationDisplay.setText(minutes + "分钟");
        }
    }
    
    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            if (createTask()) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onTaskCreated();
                }
            }
        });
    }
    
    private boolean createTask() {
        String deviceType;
        String deviceName;
        
        if (rbRobot.isChecked()) {
            deviceType = DeviceTimerTask.DEVICE_ROBOT;
            deviceName = "扫地机器人";
        } else {
            deviceType = DeviceTimerTask.DEVICE_DEHUMIDIFIER;
            deviceName = "除湿器";
        }
        
        int datePosition = spinnerDate.getSelectedItemPosition();
        long dateMillis = dateTimestamps.get(datePosition);
        
        int hour = spinnerHour.getSelectedItemPosition();
        int minute = spinnerMinute.getSelectedItemPosition();
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long startTimeMillis = calendar.getTimeInMillis();
        
        if (startTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(context, "请选择未来的时间", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        int durationMinutes = seekbarDuration.getProgress();
        if (durationMinutes < 1) {
            durationMinutes = 1;
        }
        
        DeviceTimerTask task = new DeviceTimerTask(null, deviceType, deviceName, startTimeMillis, durationMinutes);
        
        boolean success = taskManager.addTask(task);
        
        if (success) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault());
            String timeStr = sdf.format(new Date(startTimeMillis));
            Toast.makeText(context, 
                    deviceName + "定时任务已设置\n" + timeStr + " 启动，工作" + task.getFormattedDuration(), 
                    Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(context, "该设备已有5个定时任务，请先删除部分任务", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
