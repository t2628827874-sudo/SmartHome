package com.example.smarthome.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smarthome.Fragments.HomeFragment;
import com.example.smarthome.Fragments.IntelligentFragment;
import com.example.smarthome.Fragments.MyInfoFragment;
import com.example.smarthome.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {
    private FrameLayout fragment_container;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        fragment_container=findViewById(R.id.fragment_container);
        bottomNavigationView=findViewById(R.id.bottomNavigationView);
        //切换Fragment函数，默认是HomeFragment
        replaceFragment(new HomeFragment());
        //监听
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id=menuItem.getItemId();
                if (id==R.id.Home){
                    replaceFragment(new HomeFragment());
                }else if(id==R.id.intelligent){
                    replaceFragment(new IntelligentFragment());
                }else if(id==R.id.My){
                    replaceFragment(new MyInfoFragment());
                }
                return true;
            }
        });


    }
    //切换Fragment函数
    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container,fragment);
        fragmentTransaction.commit();

    }

}