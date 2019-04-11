package ru.ok.technopolis.basketball;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GameFragment extends Fragment {
    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";
    private static final String SCORE_KEY = "score";
    private static final String LOG_TAG = "Main_Activity_logs";

    boolean isVibrationOn = true;
    boolean isEasy = false;

    private Vibrator vibrator;
    private ImageView ballView;
    private int[] ballStartLocation = new int[2];
    private int[] leftSideHoopLocation = new int[2];
    private int[] rightSideHoopLocation = new int[2];
    private RelativeLayout mainLayout;
    private double radius;
    private boolean doing = false;
    private View leftSideHoop;
    private View rightSideHoop;
    private TextView scoreView;
    private int width;
    private int height;
    private double minAccuracy;
    private Button stopButton;
    private FragmentManager fragmentManager;
    private OnPauseListener onPauseListener;

    private int score = 0;
    private int rowHit = 0;

    public static GameFragment newInstance(int width, int height, int score) {
        Bundle args = new Bundle();
        GameFragment fragment = new GameFragment();
        args.putInt(WIDTH_KEY, width);
        args.putInt(HEIGHT_KEY, height);

        args.putInt(SCORE_KEY, score);
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
        width = args.getInt(WIDTH_KEY);
        height = args.getInt(HEIGHT_KEY);
        score = args.getInt(SCORE_KEY);
        MainActivity.preferences.getBoolean(MainActivity.APP_PREFERNCES_VIBRATE, isVibrationOn);
        MainActivity.preferences.getBoolean(MainActivity.APP_PREFERNCES_LEVEL, isEasy);
        fragmentManager = getChildFragmentManager();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game,
                container, false);
        if(isVibrationOn && getContext() != null){
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mainLayout = rootView.findViewById(R.id.fragment_game_layout);
        ballView = rootView.findViewById(R.id.main_activity_ball);
        scoreView = rootView.findViewById(R.id.main_activity_score_text);
        leftSideHoop = rootView.findViewById(R.id.main_activity_left_hoop_view);
        rightSideHoop = rootView.findViewById(R.id.main_activity_right_hoop_view);
        stopButton = rootView.findViewById(R.id.main_activity_stop_button);
        mainLayout.post(() -> {
            scoreView.setText(Integer.toString(score));
            leftSideHoop.getLocationOnScreen(leftSideHoopLocation);
            rightSideHoop.getLocationOnScreen(rightSideHoopLocation);
            ballView.getLocationOnScreen(ballStartLocation);
            if(isEasy){
                ballStartLocation[0] = width /2;
            }
            ballView.setX(ballStartLocation[0]);
            ballView.setY(ballStartLocation[1]);
            ballView.setVisibility(View.VISIBLE);
            minAccuracy = Math.sqrt(Math.pow(ballStartLocation[0] - leftSideHoopLocation[0], 2)
                    + Math.pow(ballStartLocation[1] - leftSideHoopLocation[1], 2));
            radius = ballView.getHeight()*0.5;

        });

        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });


        final GestureDetector gestureDetector = new GestureDetector(getContext(), myGestureListener);


        mainLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        stopButton.setOnClickListener(v -> {
            // добавляем фрагмент
            if (onPauseListener != null) {
                onPauseListener.pause(score);
            }
        });
        return rootView;
    }

    public void setOnPauseListener(OnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
    }

    private void changeFragment(Fragment f) {
        fragmentManager.beginTransaction().replace(R.id.fragment_game_layout, f).addToBackStack(f.getClass().getSimpleName()).commit();
    }

    private GestureDetector.OnGestureListener myGestureListener = new GestureDetector.SimpleOnGestureListener() {

        ValueAnimator animator;

        @Override
        public boolean onDown(MotionEvent arg0) {
            return true;
        }

        private boolean isHit = false;


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!doing) {

                final double[] closer = {Double.MAX_VALUE, Double.MAX_VALUE};
                final double[] tmpAccuracy = {0};

                doing = true;
                animator = ValueAnimator.ofFloat(0, 1);
                animator.setDuration(4000);
                final float dY = Math.abs((e2.getRawY() - e1.getRawY()) / (e2.getEventTime() - e1.getEventTime()));
                final float dX = (e2.getRawX() - e1.getRawX()) / (e2.getEventTime() - e1.getEventTime());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    private float speedX = dX / 3;
                    private float speedY = dY / 3;
                    private long lastCollisionYTime = 0;
                    private long lastCollisionXTime = 0;
                    private int collisionCounter = 1;
                    private int[] hitCoords = {ballStartLocation[0], ballStartLocation[1]};

                    {
                        Log.d(LOG_TAG, "start " + speedY + " ");
                    }

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        long time = animation.getCurrentPlayTime();
                        speedY = speedY - 0.0001f * (time - lastCollisionYTime);
                        ballView.setY(hitCoords[1] - (speedY) * (time - lastCollisionYTime));
                        ballView.setX(hitCoords[0] + speedX * (time - lastCollisionXTime));
                        ballView.setRotation(2);
                        if (ballView.getX() + ballView.getWidth() >= leftSideHoopLocation[0]-20
                                && ballView.getX() + ballView.getWidth() <= rightSideHoopLocation[0]
                                && ballView.getY() + radius < leftSideHoopLocation[1]+10
                                && ballView.getY() + radius > leftSideHoopLocation[1] - 50
                                && speedY < 1) {
                            Log.d(LOG_TAG, "speedY = " + speedY);
                            if (!isHit) {
                                score++;
                                isHit = true;
                                Log.d(LOG_TAG, "Hit");
                            }
                        }
                        if (ballView.getY() + radius > ballStartLocation[1]) {
                            if (speedY < 0) {
                                speedY = (float) ((dY / 3) * Math.pow(0.75, collisionCounter));
                                lastCollisionYTime = time;
                                hitCoords[1] = (int) ballView.getY();
                                collisionCounter++;
                            }
                        }

                        Log.d(LOG_TAG, "coords " + ballView.getX() + " " + ballView.getY() + " " + Math.sqrt(Math.pow(ballView.getX() - leftSideHoopLocation[0], 2)
                                + Math.pow(ballView.getY() - leftSideHoopLocation[1], 2)));

                        if (Math.sqrt(Math.pow(ballView.getX() - leftSideHoopLocation[0], 2)
                                + Math.pow(ballView.getY() - leftSideHoopLocation[1], 2)) <= radius
                                && ballView.getX() <= leftSideHoopLocation[0]) {
                            Log.d(LOG_TAG, "direction change 2 " + ballView.getX() + " " + ballView.getY() + " " + Math.sqrt(Math.pow(ballView.getX() - leftSideHoopLocation[0], 2)
                                    + Math.pow(ballView.getY() - leftSideHoopLocation[1], 2)));
                            speedY = -speedY;
                            lastCollisionYTime = time;
                            hitCoords[1] = (int) ballView.getY();
                            hitCoords[0] = (int) ballView.getX();
                            lastCollisionXTime = time;
                            speedX = -speedX;
                        }

                        if (ballView.getX() + ballView.getWidth() >= rightSideHoopLocation[0]
                                && ballView.getX() + radius < rightSideHoopLocation[0] + 30
                                && ballView.getY() + radius > rightSideHoopLocation[1]
                                && ballView.getY() < rightSideHoopLocation[1] + rightSideHoop.getHeight() + 10) {
                            hitCoords[0] = (int) ballView.getX();
                            lastCollisionXTime = time;
                            speedX = -speedX;
                        }

                        if (Math.sqrt(Math.pow(ballView.getX() - (leftSideHoopLocation[0] * 0.5 + rightSideHoopLocation[0] * 0.5), 2)
                                + Math.pow(ballView.getY() - leftSideHoopLocation[1], 2))
                                < Math.sqrt(Math.pow(closer[0] - leftSideHoopLocation[0], 2)
                                + Math.pow(closer[1] - leftSideHoopLocation[1], 2))) {
                            closer[0] = ballView.getX();
                            closer[1] = ballView.getY();
                        }

                        if (ballView.getX() > width * 1.5 || ballView.getX() < -width * 0.5) {
                            animator.cancel();
                        }
                    }
                });

                animator.addListener(new AnimatorListenerAdapter() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isHit) {
                            if(vibrator != null){
                                vibrator.vibrate(400);
                            }
                            changeFragment(new WinFragment());
                            isHit = false;
                            scoreView.setText(score + "");
                            rowHit++;
                            if (rowHit > 0) {

                                tmpAccuracy[0] = 0.9999;
                            }
                        } else {
                            rowHit = 0;
                            tmpAccuracy[0] = Math.sqrt(Math.pow(closer[0] - leftSideHoopLocation[0], 2)
                                    + Math.pow(closer[1] - leftSideHoopLocation[1], 2));
                            Log.d(LOG_TAG, "accuracys " + tmpAccuracy[0] + " " + minAccuracy + " clos " + closer[0] + " " + closer[1]);
                            tmpAccuracy[0] = 1 - tmpAccuracy[0] / minAccuracy;
                            if (tmpAccuracy[0] <= 0) {
                                tmpAccuracy[0] = 0.01;

                            }
                        }
                        ballView.setX(ballStartLocation[0]);
                        ballView.setY(ballStartLocation[1]);
                        doing = false;
                        AccuracyResource.addElement(tmpAccuracy[0]);
                        Log.d(LOG_TAG, "accuracy " + tmpAccuracy[0]);
                    }

                });

                animator.start();
                ballView.animate().start();

            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(LOG_TAG, "Double tap " + ballView.getX() + " " + ballView.getY());
            if (animator != null) {
                animator.cancel();
            }
            return true;
        }
    };

    interface OnPauseListener {
        void pause(int score);
    }
}