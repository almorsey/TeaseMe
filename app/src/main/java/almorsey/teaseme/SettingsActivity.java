package almorsey.teaseme;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import org.w3c.dom.Element;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, DirectoryChooserFragment.OnFragmentInteractionListener {

	private final String TRUE = "true", FALSE = "false";
	private Button teasesDirButton, saveButton;
	private EditText teasesDirEditText;
	private CheckBox eosCheckBox, hppCheckBox;
	private DirectoryChooserFragment mDialog;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.settings);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		teasesDirButton = (Button) findViewById(R.id.teasesDirButton);
		teasesDirEditText = (EditText) findViewById(R.id.teasesDirEditText);
		saveButton = (Button) findViewById(R.id.saveButton);
		eosCheckBox = (CheckBox) findViewById(R.id.eosCheckBox);
		hppCheckBox = (CheckBox) findViewById(R.id.hppCheckBox);

		teasesDirEditText.setText(MainActivity.TEASES_DIR);
		eosCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_endearOnStartup)));
		hppCheckBox.setChecked(boolFromXmlElement(getString(R.string.root_settings_homePagePicture)));

		teasesDirButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);

		final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
				.newDirectoryName("Teases")
				.allowNewDirectoryNameModification(true)
				.build();
		mDialog = DirectoryChooserFragment.newInstance(config);
	}

	public void onClick(View v) {
		if (v.equals(teasesDirButton)) {
			mDialog.show(getFragmentManager(), null);
		} else if (v.equals(saveButton)) {
			Element teaseDirE = (Element) MainActivity.data.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).item(0);
			teaseDirE.setTextContent(teasesDirEditText.getText().toString());
			Element eosE = (Element) MainActivity.data.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).item(0);
			eosE.setAttribute("value", valueFromCheckBox(eosCheckBox));
			Element hppE = (Element) MainActivity.data.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).item(0);
			hppE.setAttribute("value", valueFromCheckBox(hppCheckBox));
			MainActivity.saveDocument(MainActivity.data, MainActivity.dataFile);
		}
	}

	private boolean boolFromXmlElement(String elementName) {
		return MainActivity.data.getElementsByTagName(elementName).item(0).getAttributes().getNamedItem("value").getNodeValue().equals(TRUE);
	}

	private String valueFromCheckBox(CheckBox checkBox) {
		return checkBox.isChecked() ? TRUE : FALSE;
	}

	public void onSelectDirectory(@NonNull String path) {
		teasesDirEditText.setText(path);
		mDialog.dismiss();
	}

	public void onCancelChooser() {
		mDialog.dismiss();
	}

}
