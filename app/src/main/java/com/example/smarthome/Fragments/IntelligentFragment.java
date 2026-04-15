package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.smarthome.R;
import com.google.android.material.switchmaterial.SwitchMaterial;


public class IntelligentFragment extends Fragment {
    private TextView it_tv;
    private View cardCurrent;
    private View cardHome;
    private View cardAway;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        initName();



    }


    private void initName() {
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
        it_tv.setText(result);
    }

    private void initView(View view) {
        it_tv=view.findViewById(R.id.it_tv);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_intelligent, container, false);
    }
}