package net.greenturtles.photoframe;

import java.io.File;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public abstract class ConfigurationActivityBase extends Activity implements OnClickListener, DialogInterface.OnClickListener {
    public static final String PREFS_NAME = "ImagesWidgetPrefs";
    public static final String PREFS_UPDATE_RATE_FIELD_PATTERN = "UpdateRate-%d";
    public static final String PREFS_FOLDER_PATH_FIELD_PATTERN = "FolderPath-%d";
    public static final String PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN = "FrameType-%d";
    public static final String PREFS_UPDATE_CURRENT_PIC_INDEX_FIELD_PATTERN = "CurrentPicIndex-%d";
    private static final int PREFS_UPDATE_RATE_DEFAULT = 5;
    
    private FolderPicker mFolderDialog;
	private View mPickFolder;
	private TextView mFolderPath;
	private TextView mPicsHint;
	private Button mSaveButton;
	private Spinner mSelectFrameSpinner;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
    public abstract String getUriSchemaId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get any data we were launched with
        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            Intent cancelResultValue = new Intent();
            cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, cancelResultValue);
        } else {
            // only launch if it's for configuration
            // Note: when you launch for debugging, this does prevent this
            // activity from running. We could also turn off the intent
            // filtering for main activity.
            // But, to debug this activity, we can also just comment the
            // following line out.
            finish();
        }

        setContentView(R.layout.configuration);

        mPicsHint = (TextView)findViewById(R.id.config_folder_path_hint);
        mPickFolder = findViewById(R.id.config_pick_folder);
        mPickFolder.setOnClickListener(this);
        mFolderPath = (TextView)findViewById(R.id.config_folder_path);
        mSelectFrameSpinner = (Spinner)findViewById(R.id.select_frame_background);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.frame_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSelectFrameSpinner.setAdapter(adapter);
        
        final SharedPreferences config = getSharedPreferences(PREFS_NAME, 0);
        final EditText updateRateEntry = (EditText) findViewById(R.id.update_rate_entry);

        updateRateEntry.setText(String.valueOf(config.getInt(String.format(PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId), PREFS_UPDATE_RATE_DEFAULT)));

        mSaveButton = (Button) findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	// 验证图片列表
            	CharSequence folderPath = mFolderPath.getText();
            	if (null == folderPath || 0 == folderPath.length()) {
            		Toast.makeText(ConfigurationActivityBase.this, R.string.no_image_picked, Toast.LENGTH_SHORT).show();
            		return;
            	} else {
            		File[] pics = RegularSizeWidgetBase.getPicFileList(folderPath.toString());
            		if (null == pics || 0 == pics.length) {
                		Toast.makeText(ConfigurationActivityBase.this, R.string.no_image_picked, Toast.LENGTH_SHORT).show();
                		return;
            		}
            	}
            	
            	// 验证图片更新时间
            	CharSequence updateRate = updateRateEntry.getText();
            	int updateRateSeconds = -1;
            	if (updateRate != null && updateRate.length() >= 0) {
            		try
            		{
            			updateRateSeconds = Integer.parseInt(updateRate.toString());
            		} catch (NumberFormatException e) {
            			Log.e(LOG_TAG, "bad input: " + e.toString());
            		}
            	}
            	if (0 > updateRateSeconds) {
            		Toast.makeText(ConfigurationActivityBase.this, R.string.no_update_rate, Toast.LENGTH_SHORT).show();
            		return;
            	}

                // store off the user setting for update timing
                SharedPreferences.Editor configEditor = config.edit();

                configEditor.putInt(String.format(PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId), updateRateSeconds);
                configEditor.putString(String.format(PREFS_FOLDER_PATH_FIELD_PATTERN, appWidgetId), folderPath.toString());
                configEditor.putInt(String.format(PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN, appWidgetId), mSelectFrameSpinner.getSelectedItemPosition());
                configEditor.commit();

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

                    // tell the app widget manager that we're now configured
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    setResult(RESULT_OK, resultValue);

                    Intent widgetUpdate = new Intent();
                    widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });

                    // make this pending intent unique
                    widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(
                    	ConfigurationActivityBase.this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
                    PendingIntent newPending = PendingIntent.getBroadcast(getApplicationContext(), 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

                    // schedule the new widget for updating
                    AlarmManager alarms = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    alarms.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), updateRateSeconds * 1000, newPending);
                }

                // activity is now done
                finish();
            }
        });
    }

	public void onClick(View v) {
		if (v == mPickFolder) {
			mFolderDialog = new FolderPicker(this, this, 0);
			mFolderDialog.show();
		}
	}

	public void onClick(DialogInterface dialog, int which) {
		if (dialog == mFolderDialog) {
        	String folderPath = mFolderDialog.getPath();
        	if (null == folderPath || 0 == folderPath.length()) {
        		folderPath = "";
        	} else {
        		File picFolder = new File(folderPath);
        		folderPath = picFolder.getAbsolutePath();
        		if (!picFolder.isDirectory() || !picFolder.exists()) {
        			mPicsHint.setText(R.string.no_image_picked);
        		}
        		File[] pics = RegularSizeWidgetBase.getPicFileList(folderPath);
        		if (null == pics || 0 == pics.length) {
        			mPicsHint.setText(R.string.no_image_picked);
        		} else {
        			String hint = String.format(
        				ConfigurationActivityBase.this.getText(R.string.count_image_picked).toString(),
        				pics.length);
        			mPicsHint.setText(hint);
        		}
        	}
        	
			mFolderPath.setText(folderPath);
		}
	}
	
    private static final String LOG_TAG = "ConfigurationActivity";
}
