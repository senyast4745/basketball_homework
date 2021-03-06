package ru.ok.technopolis.basketball;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import java.io.IOException;

public class SettingsFragment extends Fragment {

    private boolean level;
    private boolean vibrate;
    private boolean music;
    private int score;

    private static final String LEVEL_KEY = "level";
    private static final String VIBRO_KEY = "vibro";
    private static final String MUSIC_KEY = "music";
    private static final String SCORE_KEY = "score";
    private static final String LOG_TAG = "SettingFragment";
    OnCloseSetListener closeListener;

    public void setCloseListener(OnCloseSetListener closeListener) {
        this.closeListener = closeListener;
    }

    public static SettingsFragment newInstance(boolean level, boolean vibrate, boolean music ,int score) {

        Bundle args = new Bundle();
        args.putBoolean(MUSIC_KEY, music);
        args.putBoolean(LEVEL_KEY, level);
        args.putBoolean(VIBRO_KEY, vibrate);
        args.putInt(SCORE_KEY, score);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalArgumentException();
        }
        music = args.getBoolean(MUSIC_KEY);
        level = args.getBoolean(LEVEL_KEY);
        vibrate = args.getBoolean(VIBRO_KEY);
        score = args.getInt(SCORE_KEY);
        Log.d(LOG_TAG, "onCreate args " + vibrate);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings,
                container, false);
        ToggleButton level = view.findViewById(R.id.fragment_settings_level_button);
        level.setChecked(this.level);
        level.setOnCheckedChangeListener((v, isChecked) -> SettingsFragment.this.level = isChecked);

        ToggleButton vibro = view.findViewById(R.id.fragment_settings_vibration_button);
        if(vibrate) {

            vibro.setBackgroundResource(R.drawable.vibrationxxhdpi);
        } else {

            vibro.setBackgroundResource(R.drawable.no_vibrationxxhdpi);
        }
        vibro.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                SettingsFragment.this.vibrate = true;
                v.setBackgroundResource(R.drawable.vibrationxxhdpi);
            } else {
                SettingsFragment.this.vibrate = false;
                v.setBackgroundResource(R.drawable.no_vibrationxxhdpi);
            }
        });

        ToggleButton musicButton = view.findViewById(R.id.fragment_settings_music_button);
        if(music) {
            musicButton.setBackgroundResource(R.drawable.no_musicxxhdpi);
        } else {

            musicButton.setBackgroundResource(R.drawable.musicxxhdpi);
        }
        musicButton.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {

                    try {
                        MainActivity.mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                MainActivity.mediaPlayer.start();
                SettingsFragment.this.music = true;
                v.setBackgroundResource(R.drawable.no_musicxxhdpi);
            } else {
                MainActivity.mediaPlayer.stop();
                SettingsFragment.this.music = false;
                v.setBackgroundResource(R.drawable.musicxxhdpi);
            }
        });

        Button close = view.findViewById(R.id.fragment_settings_close_button);
        close.setOnClickListener(v -> {
            if (closeListener != null) {
                closeListener.close(this.level, this.music ,this.vibrate, this.score);
            }
        });

        Button reset = view.findViewById(R.id.fragment_settings_reset_button);
        reset.setOnClickListener(v -> {
            AccuracyResource.getInstance().deleteAll();
            score = 0;
        });

        reset.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Button viewButton = (Button) v;
                    viewButton.getBackground().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                    v.invalidate();
                    break;
                }
                case MotionEvent.ACTION_UP:
                    // Your action here on button click
                case MotionEvent.ACTION_CANCEL: {
                    Button viewButton = (Button) v;
                    viewButton.getBackground().clearColorFilter();
                    viewButton.invalidate();
                    break;
                }
            }

            return false;
        });

        return view;
    }


    interface OnCloseSetListener {
        void close(boolean level, boolean music, boolean vibro, int score);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

}
