package ca.cmpt276.parentapp.child_config;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;

import ca.cmpt276.parentapp.R;
import ca.cmpt276.parentapp.model.Child;
import ca.cmpt276.parentapp.model.ChildManager;
import ca.cmpt276.parentapp.model.TaskManager;

/**Function that allows user to edit/delete child and their name/picture**/
public class EditChildren extends AppCompatActivity {
    final ChildManager manager = ChildManager.getInstance();
    TaskManager taskManager = TaskManager.getInstance();
    Child child;
    int position;
    ImageView portraitImageView;
    EditText nameEditText;

    String byteArray;
    public static final String SHARED_PREFERENCE = "Shared Preference";
    public static final String CHILD_LIST = "Child List";
    public static final String TASK_LIST = "Task List";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setting up toolbar
        setContentView(R.layout.activity_modify_delete_children);
        fillPositionAndChild();
        fillPortraitAndNameField();
        editImage();
        byteArray = child.getPortraitString();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.backPressedWarning)
                .setPositiveButton(R.string.yes_edit_child, (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton(R.string.no_edit_child, (dialog, which) -> {
                    /*do nothing*/
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void fillPositionAndChild() {
        Intent intent = getIntent();
        position = intent.getIntExtra(ConfigActivity.CHILD_POSITION, -1);
        if (position == -1) {
            throw new IllegalArgumentException("Error in passing child position from configActivity!");
        }
        child = manager.getChild(position);
        Log.d("Child position", String.valueOf(position));
    }

    private void fillPortraitAndNameField() {
        portraitImageView = findViewById(R.id.modifyPortrait);
        if(child.getPortrait() != null){
            portraitImageView.setImageBitmap(child.getPortrait());
        }

        nameEditText = findViewById(R.id.modifyChildName);
        nameEditText.setText(child.getChildName());
    }

    //implementation by Dhaval URL: https://github.com/Dhaval2404/ImagePicker
    /**Allows user to capture/select new image and change the old photo path to a new one**/
    private void editImage() {
        if(child.getPortrait() != null){
            portraitImageView.setImageBitmap(child.getPortrait());
        }
        else{
            portraitImageView.setImageResource(R.drawable.add_icon);
        }
        portraitImageView.setOnClickListener(View -> ImagePicker.with(this)
                .cropSquare()
                .compress(1024)			//Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                .start());
    }
    //implementation by Dhaval URL: https://github.com/Dhaval2404/ImagePicker
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            //Getting URI and converting it to bitmap
            Uri fileUri = data.getData();
            try {
                if(  fileUri !=null   ){
                    byteArray = convertBitmapString(MediaStore.Images.Media.getBitmap(this.getContentResolver() , fileUri));
                    portraitImageView.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver() , fileUri));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.modify_toolbar_icons,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //SAVE BUTTON CONFIG
        if (item.getItemId() == R.id.action_save){
            String newName = nameEditText.getText().toString();
            String oldName = child.getChildName();

            if (!TextUtils.isEmpty(nameEditText.getText().toString())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.confirm_edit_child, oldName, newName))
                        .setPositiveButton(R.string.yes_edit_child, (dialog, which) -> {
                            editChildInfo(newName);
                            finish();

                        })
                        .setNegativeButton(R.string.no_edit_child, (dialog, which) -> {
                            /*do nothing*/
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.empty_value)
                        .setNeutralButton(R.string.OK, (dialog, which) ->{} /*do nothing*/);
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
        //DELETE BUTTON
        else if(item.getItemId() == R.id.action_delete){
            String name = child.getChildName();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.confirm_delete_child, name))
                    .setPositiveButton(R.string.yes_delete_child, (dialog, which) -> {
                        deleteChildInfo();
                    })
                    .setNegativeButton(R.string.no_delete_child, (dialog, which) -> {
                        /*do nothing*/
                    });

            AlertDialog alert = builder.create();
            alert.show();

        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteChildInfo() {
        manager.removeChildren(position);
        taskManager.updateTaskHistoryList(position);
        taskManager.updateTaskList(position, manager.getNumberOfChildren());
        saveData();
        finish();
    }

    private void editChildInfo(String newName) {
        manager.editChildrenName(newName, position);
        manager.editChildrenByteString(byteArray, position);
        saveData();
        finish();
    }

    public String convertBitmapString(Bitmap bitmap){
        String bytePhoto;

        ByteArrayOutputStream baos = new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        bytePhoto =  Base64.encodeToString(b, Base64.DEFAULT);
        return bytePhoto;
    }


    void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Convert gridManager to json format
        Gson gson = new Gson();
        Gson gsonTaskList = new Gson();

        String json = gson.toJson(manager);
        String jsonTaskList = gsonTaskList.toJson(taskManager);

        //Save the json
        editor.putString(CHILD_LIST,json);
        editor.putString(TASK_LIST, jsonTaskList);

        editor.apply();
    }
}