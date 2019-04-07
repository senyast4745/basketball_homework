package ru.ok.technopolis.basketball;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MenuFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stat,
                container, false);
        /*Button stop = view.findViewById(R.id.main_activity_stop_button);
        stop.setOnClickListener(v -> close());*/
        return view;
    }

    private void close(){
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }
}
