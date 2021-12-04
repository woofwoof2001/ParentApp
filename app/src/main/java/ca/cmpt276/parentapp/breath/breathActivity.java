package ca.cmpt276.parentapp.breath;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ca.cmpt276.parentapp.R;

public class breathActivity extends AppCompatActivity {
    // State Pattern's base states
    private abstract static class State {
        // If putting in new file, you might want State base class to
        // hold a reference to the activity

        public State(breathActivity context) {
        }

        // Empty implementations, so derived class don't need to
        // override methods they don't care about.
        void handleEnter() {}
        void handleExit() {}
        void handleClickOff() {}
        void handleThreeSecsLess() {}
    }
    //breath in
    public final State inState = new inState(breathActivity.this);
    //breath out
    public final State outState = new outState(breathActivity.this);
    private State currentState = new IdleState(breathActivity.this);
    public final State preBreathState = new preBreathState(breathActivity.this);

    public void setState(State newState) {
        currentState.handleExit();
        currentState = newState;
        currentState.handleEnter();
    }

    // Android Code
    //Android global variables
    int breathNum;
    int originalBreathNum;
    CountDownTimer countDownTimer;
    int milliseconds = 0;
    int exhaleTimeInMilliSec = 0;
    final int MINIMUM_MILLISECONDS_FOR_INHALE = 3000;
    MediaPlayer music;
    Button breathButton;
    Button addBreath;
    Button decreaseBreath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_breath);

        //save original num breath
        getBreathNumFromSharedPreferences();

        music = MediaPlayer.create(this, R.raw.piano_moment);
        addBreath = findViewById(R.id.btnAddBreath);
        decreaseBreath = findViewById(R.id.btnRemoveBreath);
        setState(preBreathState);
    }

    //TODO fix string extract
    private void configureHeading() {
        Log.d("configureTextView", "In!");

        updateHeading();

        Button addBreath = findViewById(R.id.btnAddBreath);
        addBreath.setOnClickListener(view -> {
            if (breathNum < 10) {
                breathNum++;
                originalBreathNum++;
                updateHeading();

            } else {
                Toast.makeText(this, "Please select between 1 and 10 breaths", Toast.LENGTH_SHORT).show();
            }
        });

        Button decreaseBreath = findViewById(R.id.btnRemoveBreath);
        decreaseBreath.setOnClickListener(view -> {
            if (breathNum > 0) {
                breathNum--;
                originalBreathNum--;
                updateHeading();
            } else {
                Toast.makeText(this, "Please select between 1 and 10 breaths", Toast.LENGTH_SHORT).show();
            }
        });

    }
    @SuppressLint("SetTextI18n")
    private void updateHeading(){
        TextView heading = findViewById(R.id.tvHeading);
        heading.setText("Let's take " + breathNum + " breaths together");
    }

    private void setHelpText(String string){
        TextView help = findViewById(R.id.helpText);
        help.setText(string);
    }


    // ************************************************************
    // Breath in State
    // ************************************************************
    private class inState extends State {

        public inState(breathActivity context) {
            super(context);
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setUpBreathInButton(){
            breathButton.setOnTouchListener((view, motionEvent) -> {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        Toast.makeText(getApplicationContext(), "Now inhale ...", Toast.LENGTH_LONG).show();
                        breathInAnimation();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        //currentState would be inState
                        if(countDownTimer != null){
                            countDownTimer.cancel();
                        }
                        music.pause();
                        currentState.handleClickOff();
                        break;
                    }
                }
                return false;
            });
        }

        /**Animation for breath in, used in inState**/
        @SuppressLint("ClickableViewAccessibility")
        private void breathInAnimation(){
            music.start();
            breathButton.setText(R.string.breath_in);
            countDownTimer = new CountDownTimer(10000, 100) {
                @Override
                public void onTick(long l) {
                    ViewGroup.LayoutParams params = breathButton.getLayoutParams();
                    params.width *= 1.01;
                    params.height *= 1.01;
                    breathButton.setLayoutParams(params);
                    milliseconds += 100;
                }

                @Override
                public void onFinish() {
                    setHelpText("Release button and breath out");
                    countDownTimer.cancel();
                    music.pause();
                    currentState.handleClickOff();
                }
            }.start();

        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        void handleEnter() {
            Log.d("inState", "In!");

            addBreath.setVisibility(View.INVISIBLE);
            decreaseBreath.setVisibility(View.INVISIBLE);
            if(breathNum <= 0){
                setState(preBreathState);

            }
            else{
                Log.d("inState", "Animation setup-ed!");
                setHelpText("Hold button and breath in!");
                setUpBreathInButton();
            }
        }

        @Override
        void handleClickOff() {
            if (milliseconds < MINIMUM_MILLISECONDS_FOR_INHALE) {
                setHelpText("Hold button and breath in!");
                currentState.handleThreeSecsLess();
            }
            else{
                setState(outState);
            }
        }

        @Override
        void handleThreeSecsLess() {
            Log.d("inState", "Is 3 seconds or less condition triggered");
            setHelpText("Release button when breath out.");
            music.pause();
            if(countDownTimer != null){
                countDownTimer.cancel();
            }
            resetButton();
            milliseconds = 0;
        }
    }

    // ************************************************************
    // Breath out state
    // ************************************************************
    private class outState extends State {

        public outState(breathActivity context) {
            super(context);
        }
        @Override
        void handleEnter() {
            breathOutAnimation();
        }

        @Override
        void handleClickOff() {

            Log.i("outState", "Exiting outState");
            if(breathNum <= 0){
                setState(preBreathState);
            }
            else{
                setState(inState);
            }
        }

        /**Animation for breath out, used in outState**/
        @SuppressLint("ClickableViewAccessibility")
        private void breathOutAnimation() {
            //starts inward animation, start breathing sound
            breathButton.setText(R.string.breath_out);
            breathButton.setOnTouchListener(null);
            breathButton.setOnClickListener(null);
            music.start();

            long interval = 100;

            Toast.makeText(getApplicationContext(), "and exhale.", Toast.LENGTH_LONG).show();

            countDownTimer = new CountDownTimer(milliseconds, interval) {
                boolean isDecrement = false;
                @Override
                public void onTick(long l) {
                    Log.d("BreathOutAnimation", "triggered");
                    ViewGroup.LayoutParams params = breathButton.getLayoutParams();

                    params.width /= 1.009;
                    params.height /= 1.009;

                    if (params.width <= 450) {
                        params.width = 450;
                        params.height = 450;
                    }
                    breathButton.setLayoutParams(params);

                    //only allows for the user to stop animation and sound after 3 seconds
                    exhaleTimeInMilliSec += interval;
                    if (exhaleTimeInMilliSec >= 3000) {
                        if(!isDecrement){
                            --breathNum;
                            updateHeading();
                            isDecrement = true;
                        }
                        if(breathNum == 0){
                            breathButton.setText(R.string.breath_good_job);
                            setHelpText("Click add breath to practice breathing!");
                        }
                        else {
                            breathButton.setText(R.string.breath_in);
                        }
                        breathButton.setOnClickListener(view -> {
                            music.pause();
                            countDownTimer.cancel();
                            resetButton();
                            milliseconds = 0;
                            exhaleTimeInMilliSec = 0;

                            handleClickOff();
                        });
                    }

                }

                @Override
                public void onFinish() {
                    music.pause();
                    if(breathNum == 0){
                        breathButton.setText(R.string.breath_good_job);
                        setHelpText("Click add breath to practice breathing!");
                    }
                    else {
                        breathButton.setText(R.string.breath_in);
                    }
                    resetButton();
                    milliseconds = 0;
                    exhaleTimeInMilliSec = 0;

                    handleClickOff();

                }
            }.start();
        }
    }

    // ************************************************************
    // Pre Breath State
    // ************************************************************
    public class preBreathState extends State {
        public preBreathState(breathActivity context) {
            super(context);
        }

        @Override
        void handleEnter() {
            Log.d("preBreathState", "In!");

            addBreath.setVisibility(View.VISIBLE);
            decreaseBreath.setVisibility(View.VISIBLE);
            setHelpText("Click add breath to practice breathing!");
            configureHeading();
            configureButton();
            breathButton.setText(R.string.breath_start);
        }
        @Override
        void handleClickOff() {
            Log.d("preBreathState/handleClickOff", "In!");
            setState(inState);
        }

        private void configureButton() {
            breathButton = findViewById(R.id.breathButton);
            breathButton.setOnTouchListener(null);
            breathButton.setOnClickListener(View -> {
                if(breathNum > 0) {
                    breathButton.setText(R.string.breath_begin);
                    currentState.handleClickOff();
                }
            });
        }
    }

    private void resetButton() {
        ViewGroup.LayoutParams initialParams = breathButton.getLayoutParams();
        initialParams.width = 450;
        initialParams.height = 450;
        breathButton.setLayoutParams(initialParams);
    }

    private static class IdleState extends State {
        // Use "Null Object" pattern: This class, does nothing! It's like a safe null
        public IdleState(breathActivity context) {
            // ************************************************************
            // Idle State
            // ************************************************************
            super(context);
        }
    }

    private void storeBreathNumToSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("Shared Preference", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("originalBreathNum", originalBreathNum);
        editor.apply();
    }

    private void getBreathNumFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("Shared Preference", MODE_PRIVATE);
        breathNum = sharedPreferences.getInt("originalBreathNum", 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeBreathNumToSharedPreferences();
    }
}