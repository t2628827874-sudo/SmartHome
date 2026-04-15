package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthome.R;

public class IntelligentFragment extends Fragment {
    private TextView it_tv;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        initName();
        initCards(view);
    }

    private void initCards(View view) {
        View cardCurrentMode = view.findViewById(R.id.card_current_mode);
        View cardModeHome = view.findViewById(R.id.card_mode_home);
        View cardModeAway = view.findViewById(R.id.card_mode_away);

        if (cardCurrentMode != null) {
            TextView tvTitle = cardCurrentMode.findViewById(R.id.tv_title);
            TextView tvSubtitle = cardCurrentMode.findViewById(R.id.tv_subtitle);
            if (tvTitle != null) tvTitle.setText("当前模式");
            if (tvSubtitle != null) tvSubtitle.setText("暂无激活的场景");
        }

        if (cardModeHome != null) {
            TextView tvTitle = cardModeHome.findViewById(R.id.tv_title);
            TextView tvSubtitle = cardModeHome.findViewById(R.id.tv_subtitle);
            if (tvTitle != null) tvTitle.setText("回家模式");
            if (tvSubtitle != null) tvSubtitle.setText("开启灯光，关闭监控");
        }

        if (cardModeAway != null) {
            TextView tvTitle = cardModeAway.findViewById(R.id.tv_title);
            TextView tvSubtitle = cardModeAway.findViewById(R.id.tv_subtitle);
            if (tvTitle != null) tvTitle.setText("离开模式");
            if (tvSubtitle != null) tvSubtitle.setText("关闭电器，开启监控");
        }
    }

    private void initName() {
        if (getActivity() == null) return;
        
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String name = "name_" + currentAccount;
        String string = sp.getString(name, "");
        if (string.isEmpty()) {
            string = currentAccount;
        }
        String result = string + "的家";
        if (it_tv != null) {
            it_tv.setText(result);
        }
    }

    private void initView(View view) {
        it_tv = view.findViewById(R.id.it_tv);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intelligent, container, false);
    }
}
