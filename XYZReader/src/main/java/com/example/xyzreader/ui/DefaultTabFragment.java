package com.example.xyzreader.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;

/**
 * Created by clinton on 3/26/18.
 */

public class DefaultTabFragment extends Fragment {

    public DefaultTabFragment(){}


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.default_tablet_fragment,container,false);
        return v;
    }
}
