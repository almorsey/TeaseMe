package almorsey.teaseme;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Switch;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static almorsey.teaseme.MainActivity.TAG;
import static almorsey.teaseme.MainActivity.boolFromXmlElement;
import static almorsey.teaseme.MainActivity.data;
import static almorsey.teaseme.MainActivity.dataFile;
import static almorsey.teaseme.MainActivity.saveDocument;
import static almorsey.teaseme.MainActivity.stringFromCheckBox;

public class SettingsActivity extends AppCompatActivity
		implements View.OnClickListener, DirectoryChooserFragment.OnFragmentInteractionListener, CompoundButton.OnCheckedChangeListener{

	private Button teasesDirButton, saveButton, rasButton;
	private EditText teasesDirEditText;
	private CheckBox eosCheckBox, hppCheckBox, pidCheckBox, ptCheckBox, stCheckBox, rsCheckBox, ppCheckBox, rltCheckBox;
	private Switch cheatsSwitch;
	private GridLayout cheatsGridLayout;
	private DirectoryChooserFragment mDialog;

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if(toolbar != null) toolbar.setTitle(R.string.settings);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

		teasesDirButton = (Button) findViewById(R.id.teasesDirButton);
		teasesDirEditText = (EditText) findViewById(R.id.teasesDirEditText);
		saveButton = (Button) findViewById(R.id.saveButton);
		eosCheckBox = (CheckBox) findViewById(R.id.eosCheckBox);
		hppCheckBox = (CheckBox) findViewById(R.id.hppCheckBox);
		cheatsSwitch = (Switch) findViewById(R.id.ecSwitch);
		cheatsGridLayout = (GridLayout) findViewById(R.id.cheatsGidLayout);
		pidCheckBox = (CheckBox) findViewById(R.id.pidCheckBox);
		ptCheckBox = (CheckBox) findViewById(R.id.ptCheckBox);
		stCheckBox = (CheckBox) findViewById(R.id.stCheckBox);
		rsCheckBox = (CheckBox) findViewById(R.id.rsCheckBox);
		ppCheckBox = (CheckBox) findViewById(R.id.ppCheckBox);
		rltCheckBox = (CheckBox) findViewById(R.id.rltCheckBox);
		rasButton = (Button) findViewById(R.id.rasButton);

		teasesDirButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		rasButton.setOnClickListener(this);
		cheatsSwitch.setOnCheckedChangeListener(this);

		teasesDirEditText.setText(MainActivity.TEASES_DIR);
		eosCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_endearOnStartup)));
		hppCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_homePagePicture)));
		cheatsSwitch.setChecked(!boolFromXmlElement(getString(R.string.root_settings_cheats)));
		pidCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_cheats_pageID)));
		ptCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_cheats_pauseTimer)));
		stCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_cheats_skipTimer)));
		rsCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_cheats_removeSave)));
		ppCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_cheats_prevPage)));
		rltCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_rememberLastTease)));
		cheatsSwitch.setChecked(!cheatsSwitch.isChecked());

		final DirectoryChooserConfig config = DirectoryChooserConfig.builder().newDirectoryName("Teases").allowNewDirectoryNameModification(true).build();
		mDialog = DirectoryChooserFragment.newInstance(config);
	}

	public void onClick(View v){
		if(v.equals(teasesDirButton)){
			mDialog.show(getFragmentManager(), null);
		}else if(v.equals(saveButton)){
			Log.d(TAG, "onClick: ");
			Element teaseDirE = (Element) data.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).item(0);
			teaseDirE.setTextContent(teasesDirEditText.getText().toString());
			Element eosE = (Element) data.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).item(0);
			eosE.setAttribute("value", stringFromCheckBox(eosCheckBox));
			Element hppE = (Element) data.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).item(0);
			hppE.setAttribute("value", stringFromCheckBox(hppCheckBox));
			Element cheatsE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats)).item(0);
			cheatsE.setAttribute("value", stringFromCheckBox(cheatsSwitch));
			Element pidE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats_pageID)).item(0);
			pidE.setAttribute("value", stringFromCheckBox(pidCheckBox));
			Element ptE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats_pauseTimer)).item(0);
			ptE.setAttribute("value", stringFromCheckBox(ptCheckBox));
			Element stE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats_skipTimer)).item(0);
			stE.setAttribute("value", stringFromCheckBox(stCheckBox));
			Element rsE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats_removeSave)).item(0);
			rsE.setAttribute("value", stringFromCheckBox(rsCheckBox));
			Element ppE = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats_prevPage)).item(0);
			ppE.setAttribute("value", stringFromCheckBox(ppCheckBox));
			Element rltE = (Element) data.getElementsByTagName(getString(R.string.root_settings_rememberLastTease)).item(0);
			rltE.setAttribute("value", stringFromCheckBox(rltCheckBox));
			MainActivity.saveDocument(data, MainActivity.dataFile);
			Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
		}else if(v.equals(rasButton)){
			DialogInterface.OnClickListener handler = new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which){
					if(which == DialogInterface.BUTTON_POSITIVE){
						Node s = data.getElementsByTagName(getString(R.string.root_saves)).item(0);
						Log.d(TAG, "onClick: " + s);
						Element root = (Element) data.getElementsByTagName(getString(R.string.root)).item(0);
						root.removeChild(s);
						Element saves = data.createElement(getString(R.string.root_saves));
						root.appendChild(saves);
						saveDocument(data, dataFile);
						Toast.makeText(SettingsActivity.this, "All saves deleted", Toast.LENGTH_LONG).show();
					}else dialog.dismiss();
				}
			};
			AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)//@formatter:off
				.setTitle("Delete")
				.setMessage("Are you sure you want to delete all saves?")
				.setPositiveButton("Delete", handler)
				.setNegativeButton("Cancel", handler)
				.create();//@formatter:on
			dialog.show();
		}
	}

	public void onSelectDirectory(@NonNull String path){
		teasesDirEditText.setText(path);
		mDialog.dismiss();
	}

	public void onCancelChooser(){
		mDialog.dismiss();
	}

	public void onCheckedChanged(CompoundButton v, boolean isChecked){
		if(v.equals(cheatsSwitch)){
			int flag;
			if(isChecked) flag = View.VISIBLE;
			else flag = View.GONE;
			for(int i = 0; i < cheatsGridLayout.getChildCount(); i++){
				View cheat = cheatsGridLayout.getChildAt(i);
				cheat.setVisibility(flag);
			}
		}
	}
}
