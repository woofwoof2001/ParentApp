package ca.cmpt276.parentapp.flipcoin;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import java.util.ArrayList;

import ca.cmpt276.parentapp.R;
import ca.cmpt276.parentapp.model.Child;
import ca.cmpt276.parentapp.model.FlipCoin;
import ca.cmpt276.parentapp.model.FlipCoinManager;
import ca.cmpt276.parentapp.model.ChildManager;
import ca.cmpt276.parentapp.child_config.ConfigActivity;

/**
 * Generate flip coin activity screen. Displays which child's turn to pick, allows user to
 * click on "HEADS" or "TAILS" button to flip the coin, displays a image view of coin and change to
 * corresponding images indicates sides, and user can launch history screen through "History" button.
 */

public class FlipCoinActivity extends AppCompatActivity {

    public static final String SHARED_PREFERENCE = "Shared Preference";
    public static final String SAVE_COIN_MANAGER = "SAVE_COIN_MANAGER";

    ChildManager manager;
    ArrayList<Child> childrenList;

    FlipCoinManager flipCoinManager;
    FlipCoin flipCoinGame, newGame;

    FlipCoin.CoinSide currentCoinSideInImg = FlipCoin.CoinSide.HEADS; //Initial coin side in image
    FlipCoin.CoinSide coinResult;
    FlipCoin.CoinSide pickerChoice;

    ObjectAnimator animStage1;
    ObjectAnimator animStage2;
    Button headButton;
    Button tailButton;
    ImageView coinImg;
    ImageView playerProfile;
    TextView showPicker;

    Button historyButton;

    MediaPlayer coinFlipSound;

    int rotationCount = 0;
    int maxRepeat = 6;

    TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flip_coin);
        this.setTitle("Flip Coin");

        coinFlipSound = MediaPlayer.create(this, R.raw.coin_flip_sound);

        initializeLayout();
        initializeAnimation();

        loadData();
    }

    @Override
    protected void onResume() {
        if (childrenList.size() > 0 && flipCoinManager.getCurrentPlayer() != null) {
            flipCoinGame = new FlipCoin();
            flipCoinGame.setPicker(flipCoinManager.getCurrentPlayer());
            if(flipCoinGame.getPicker().getPortrait() != null){
                playerProfile.setImageBitmap(flipCoinGame.getPicker().getPortrait());
            }
            else{
                playerProfile.setImageResource(R.drawable.default_portrait);
            }

            String message = getString(R.string.player_turn,flipCoinManager.getCurrentPlayer().getChildName());
            showPicker.setText(message);
        }
        else {
            showPicker.setText(R.string.no_configured_children);
            playerProfile.setImageBitmap(null);
        }

        enableButtons();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        flipCoinManager.resetEpoch();
        saveData();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.flip_coin_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.queue) {
            Intent i = FlipCoinQueue.makeIntent(this);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Save current data of the gameManager using SharedPreferences
    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Convert gridManager to json format
        Gson gson = new Gson();
        String json = gson.toJson(flipCoinManager);

        //Save the json
        editor.putString(SAVE_COIN_MANAGER,json);
        editor.apply();
    }

    //Load data from saved state
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);

        //Get the model in json format
        Gson gson = new Gson();
        String coin_json = sharedPreferences.getString(SAVE_COIN_MANAGER,null);
        String child_json = sharedPreferences.getString(ConfigActivity.CHILD_LIST,null);

        ///----------------------------- Get ChildManager data --------------------------------/////
        //Covert the childManager into an Object and set the instance to the specified gameManager
        manager = gson.fromJson(child_json, ChildManager.class);
        ChildManager.setInstance(manager);

        if(manager == null) {
            manager = ChildManager.getInstance();
        }

        childrenList = manager.getChildList();

        ///----------------------------- Get FlipCoinManager data ----------------------------/////

        //Covert the gameManager into an Object and set the instance to the specified gameManager
        flipCoinManager = gson.fromJson(coin_json,FlipCoinManager.class);
        FlipCoinManager.setInstance(flipCoinManager);

        if(flipCoinManager == null) {
            flipCoinManager = FlipCoinManager.getInstance();
        }

        flipCoinManager.setDefaultEmpty(false);
        flipCoinManager.setPlayerList(childrenList);
        if(flipCoinManager.isNewEpoch()){
            flipCoinManager.shufflePlayer();
        }

        if (childrenList.size() > 0) {
            flipCoinGame = new FlipCoin();

            flipCoinGame.setPicker(flipCoinManager.getCurrentPlayer());

            if(flipCoinGame.getPicker().getPortrait() != null){
                playerProfile.setImageBitmap(flipCoinGame.getPicker().getPortrait());
            }
            else{
                playerProfile.setImageResource(R.drawable.default_portrait);
            }

            String message = getString(R.string.player_turn,flipCoinGame.getPicker().getChildName());
            showPicker.setText(message);
        }
        else {
            showPicker.setText(R.string.no_configured_children);
        }

        flipCoinManager.updateEpoch();

    }

    ///--------------------------Functions for initialization-------------------------///

    private void initializeLayout() {
        resultText = findViewById(R.id.resultMessage);
        coinImg = findViewById(R.id.iv_coin);
        showPicker = findViewById(R.id.showPicker);
        playerProfile = findViewById(R.id.flipCoin_profile);

        setUpButtons();
    }

    private void initializeAnimation() {
        animStage1 = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.flipx1);
        animStage2 = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.flipx2);

        animStage1.setTarget(coinImg);
        animStage2.setTarget(coinImg);

        animStage1.setDuration(100);
        animStage2.setDuration(100);

        animStage1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (currentCoinSideInImg == FlipCoin.CoinSide.HEADS){
                    coinImg.setImageResource(R.drawable.loonie_tails);
                    currentCoinSideInImg = FlipCoin.CoinSide.TAILS;
                }
                else{
                    coinImg.setImageResource(R.drawable.loonie_heads);
                    currentCoinSideInImg = FlipCoin.CoinSide.HEADS;
                }
                animStage2.start();
            }
        });

        animStage2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                rotationCount++;

                if (rotationCount < maxRepeat){
                    animStage1.start();
                }

                else{

                    try {
                        coinFlipSound.stop();
                        coinFlipSound.prepare();
                    } catch (Exception e) {
                        Toast.makeText(FlipCoinActivity.this, "Error in ending sound",
                                Toast.LENGTH_SHORT).show();
                    }

                    enableButtons();

                    //Only update if there are children in the list
                    if (!flipCoinManager.isEmpty()){

                        //Set results onto the object and save that data
                        if (!flipCoinManager.isOverrideDefaultEmpty()){
                            flipCoinManager.addGame(flipCoinGame);
                        }
                        flipCoinManager.updateQueue();
                        flipCoinManager.updateEpoch();
                        displayResultMessage();
                        saveData();

                        //Create a new game
                        newGame = new FlipCoin();
                        flipCoinGame = newGame;
                        flipCoinGame.setPicker(flipCoinManager.getCurrentPlayer());

                        if(flipCoinGame.getPicker().getPortrait() != null){
                            playerProfile.setImageBitmap(flipCoinGame.getPicker().getPortrait());
                        }
                        else{
                            playerProfile.setImageResource(R.drawable.default_portrait);
                        }

                        String message = getString(R.string.player_turn,
                                flipCoinGame.getPicker().getChildName());
                        showPicker.setText(message);
                    }

                    else{
                        displayMessageForEmpty(pickerChoice,coinResult);
                    }

                    rotationCount = 0;
                }
            }
        });
    }

    private void setUpButtons() {
        headButton = findViewById(R.id.btn_heads);
        tailButton = findViewById(R.id.btn_tails);
        historyButton = findViewById(R.id.historyButton);

        headButton.setOnClickListener(view -> {
            disableButtons();

            if (!flipCoinManager.isEmpty() && !flipCoinManager.isOverrideDefaultEmpty()) {
                String message = getString(R.string.player_choice,
                        flipCoinGame.getPicker().getChildName(),
                        FlipCoin.CoinSide.HEADS.toString());
                showPicker.setText(message);
                flipCoinGame.setPickerChoice(FlipCoin.CoinSide.HEADS);
            }

            pickerChoice = FlipCoin.CoinSide.HEADS;
            resultText.setText("");
            coinFlipSound.start();
            flipCoinImg();
        });

        tailButton.setOnClickListener(view -> {
            disableButtons();

            if (!flipCoinManager.isEmpty() && !flipCoinManager.isOverrideDefaultEmpty()) {
                String message = getString(R.string.player_choice,
                                            flipCoinGame.getPicker().getChildName(),
                                            FlipCoin.CoinSide.TAILS.toString());
                showPicker.setText(message);
                flipCoinGame.setPickerChoice(FlipCoin.CoinSide.TAILS);
            }

            pickerChoice = FlipCoin.CoinSide.TAILS;
            resultText.setText("");
            coinFlipSound.start();
            flipCoinImg();
        });

        historyButton.setOnClickListener(View -> {
            disableButtons();

            Intent intent = FlipCoinHistory.makeIntent(this);
            startActivity(intent);
        });
    }

    ///--------------------------Functions to update Views-------------------------///

    private void displayResultMessage() {
        if (flipCoinGame.isPickerWinner()){
            resultText.setText(getString(R.string.win_text,
                    flipCoinGame.getFlipResult().toString()));
        }
        else {
            resultText.setText(getString(R.string.lose_text,
                    flipCoinGame.getFlipResult().toString()));
        }
    }

    private void displayMessageForEmpty(FlipCoin.CoinSide pickerChoice,
                                        FlipCoin.CoinSide flipResult) {
        if (pickerChoice == flipResult){
            resultText.setText(getString(R.string.win_text,
                    flipResult.toString()));
        }
        else {
            resultText.setText(getString(R.string.lose_text,
                    flipResult.toString()));
        }
    }

    private void flipCoinImg() {
        disableButtons();
        coinResult = !flipCoinManager.isEmpty() ? flipCoinGame.flipCoin() : new FlipCoin().flipCoin();
        if (coinResult == FlipCoin.CoinSide.HEADS){
            Log.i("CoinResult:", "HEADS");
            if (currentCoinSideInImg == FlipCoin.CoinSide.HEADS){
                maxRepeat = 6; // Lands on same side
            }
            else{
                maxRepeat = 7; // Land on the different side
            }
        }
        else {
            Log.i("CoinResult:", "TAILS");
            if (currentCoinSideInImg == FlipCoin.CoinSide.TAILS) {
                maxRepeat = 6;
            } else {
                maxRepeat = 7;
            }
        }

        animStage1.start();
    }

    public void enableButtons() {
        headButton.setEnabled(true);
        tailButton.setEnabled(true);
        historyButton.setEnabled(true);
    }

    public void disableButtons(){
        headButton.setEnabled(false);
        tailButton.setEnabled(false);
        historyButton.setEnabled(false);
    }

}
