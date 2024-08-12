package cz.xlisto.elektrodroid.modules.backup;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.xlisto.elektrodroid.R;


public class GoogleDriveFragment extends Fragment {

    public GoogleDriveFragment() {
        // Required empty public constructor
    }


    public static GoogleDriveFragment newInstance() {
        return new GoogleDriveFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_google_drive, container, false);
    }

}