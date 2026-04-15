package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smarthome.Adapter.RecyclerViewAdapter;
import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.Model.DeviceCardModel;
import com.example.smarthome.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private TextView home_username;
    private ImageButton home_btn_img;
    private TabLayout home_tablayout;
    private FrameLayout home_scroll_content;
    private RecyclerView recyclerView;
    private MqttManager livingledMqttManager;
    private MqttManager livingCameraMqttManager;

    private static final String TAG = "HomeFragment";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        //设置默认的用户名
        setUserName();
        //给所有的MQTTmanager初始化
        initMqttManager();

        //给加号设置监听
        home_btn_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建popmenue，以当前v为锚点
                PopupMenu popupMenu=new PopupMenu(requireContext(),v);
                //加载menue
                popupMenu.getMenuInflater().inflate(R.menu.home_add,popupMenu.getMenu());
                //给这个菜单添加监听
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId()==R.id.action_add_device){
                            Toast.makeText(requireContext(), "添加设备", Toast.LENGTH_SHORT).show();
                        }

                        return true;
                    }
                });
                //显示菜单
                popupMenu.show();
            }
        });
        //初始化tablayout
        initTablayout();
        //监听点击的是哪个TAB，动态加载xml
        home_tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        //默认显示0,客厅的xml
        switchContent(0);



    }

    private void initMqttManager() {
        //MQTTmanager,连接thingscloud，MQTT
        livingledMqttManager = new MqttManager(
                requireContext(),
                "ch1hpsnpie8hxwuj",   // LED 设备的 AccessToken
                "odoscHX24A",         // ProjectKey
                "state"               // LED 状态字段名
        );
        livingledMqttManager.connect();

        livingCameraMqttManager = new MqttManager(
                requireContext(),
                "taclmu1x2gf4s5cx",   // LED 设备的 AccessToken
                "odoscHX24A",         // ProjectKey
                "camera_status"               // LED 状态字段名
        );
        livingCameraMqttManager.connect();
    }

    @SuppressLint("MissingInflatedId")
    private void switchContent(int position) {
        Log.d(TAG, "switchContent position=" + position);
        if (home_scroll_content == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        //替换之前先把之前的布局清空
        home_scroll_content.removeAllViews();

        int layoutId;//存储id
        if (position == 0) { // 全屋
            layoutId = R.layout.home_tab_all;
        } else if(position == 1) {             // 客厅（先用位置 1，其它先不管）
            layoutId = R.layout.home_tab_living;
        }else if(position == 2) {
            layoutId = R.layout.home_tab_dining;
        }else{
            layoutId = R.layout.home_tab_bedroom;
        }
        Log.d(TAG, "inflate layoutId=" + layoutId);
        //在home_scroll_content加载layoutId布局，然后不允许自动加载，后面add手动加载
        View root = inflater.inflate(layoutId, home_scroll_content, false);
        home_scroll_content.addView(root);
        //当是全屋xml设置recyclerView
        if (position == 0) { // 全屋 tab
            RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
            ArrayList<DeviceCardModel> recyclerViewList = new ArrayList<>();
            //统计灯泡亮的个数
            int lightOnCount=getlightOnCount();
            recyclerViewList.add(new DeviceCardModel(formatLightSubtitle(lightOnCount),"灯泡", R.drawable.light)); // iconId 用真实图片资源
            recyclerViewList.add(new DeviceCardModel("一个窗帘开","窗帘", R.drawable.curtain));
            recyclerViewList.add(new DeviceCardModel("温度湿度","环境", R.drawable.circumstance));
            recyclerViewList.add(new DeviceCardModel("正在工作","扫地机器人", R.drawable.robot));
            //统计摄像头工作的个数
            int cameraOnCount=getCameraOnCount();
            recyclerViewList.add(new DeviceCardModel(formatCameraSubtitle(cameraOnCount),"摄像头", R.drawable.camera));

            RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(requireContext(), recyclerViewList);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(),2));
            recyclerView.setAdapter(recyclerViewAdapter);
        } else if (position==1) {
            SwitchMaterial sw = root.findViewById(R.id.sw_toggle);
            SwitchMaterial sw2 =root.findViewById(R.id.sw_toggle2);
            if(sw!=null){
                //先拿到最近的一次服务器状态
                Boolean lastState=livingledMqttManager.getLastState();
                //设置初始的开关状态，与服务器一致
                if(lastState!=null){
                    sw.setChecked(lastState);
                }
                //点击开关的时候，把状态发给服务器
                sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                        livingledMqttManager.publish("state",isChecked);
                    }
                });
            }
            if(sw2!=null){
                //先拿到最近的一次服务器状态
                Boolean lastCameraState=livingCameraMqttManager.getLastState();
                //设置初始的开关状态，与服务器一致
                if(lastCameraState!=null){
                    sw2.setChecked(lastCameraState);
                }
                //点击开关的时候，把状态发给服务器
                sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                        livingCameraMqttManager.publish("camera_status",isChecked);
                    }
                });
            }

        }

    }

    // 统计当前有几个灯泡是亮的
    private int getlightOnCount() {
        int count = 0;
        // 现在只有客厅这一个灯泡，将来有更多灯泡就在这里继续加判断
        if (livingledMqttManager != null
                && Boolean.TRUE.equals(livingledMqttManager.getLastState())) {
            count++;
        }
        return count;
    }

    // 统计当前有几个摄像头在工作
    private int getCameraOnCount() {
        int count = 0;
        // 现在只有客厅这一个摄像头，将来有更多摄像头就在这里继续加判断
        if (livingCameraMqttManager != null
                && Boolean.TRUE.equals(livingCameraMqttManager.getLastState())) {
            count++;
        }
        return count;
    }

    // 根据灯泡数量拼出文案
    private String formatLightSubtitle(int count) {
        if (count <= 0) {
            return "没有灯泡亮";
        } else if (count == 1) {
            return "一个灯泡亮";
        } else {
            return count + "个灯泡亮";
        }
    }

    // 根据摄像头数量拼出文案
    private String formatCameraSubtitle(int count) {
        if (count <= 0) {
            return "没有摄像头工作";
        } else if (count == 1) {
            return "一个正在工作";
        } else {
            return count + "个正在工作";
        }
    }


    private void initTablayout() {
        home_tablayout.removeAllTabs();
        String[] titles = {"全屋", "客厅", "餐厅", "卧室"};
        for (String title : titles) {
            home_tablayout.addTab(home_tablayout.newTab().setText(title));
        }

    }

    private void setUserName() {
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        String currentAccount = sp.getString("current_account","");
        //拼接然后取sp找，没有就默认
        String name="name_"+currentAccount;
        String string = sp.getString(name, "");
        if(string.isEmpty()){
            string=currentAccount;
        }
        String result=string+"的家";
        home_username.setText(result);
    }

    private void initView(View view) {
        home_username=view.findViewById(R.id.home_username);
        home_btn_img=view.findViewById(R.id.home_btn_img);
        home_tablayout=view.findViewById(R.id.home_tablayout);
        home_scroll_content=view.findViewById(R.id.home_scroll_content);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    public void logout(View v){

    }
}