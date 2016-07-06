package almorsey.teaseme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends Activity implements View.OnClickListener{

	/**
	 * TEASES_DIR = directory where all teases are located
	 * data = xml document including saves, setttings and misc info
	 * dataFile = File object of @data xml file
	 */
	static final String TAG = "almorsey";
	private static final String TRUE = "true", FALSE = "false";
	static String TEASES_DIR;
	static Document data;
	static File dataFile;

	/**
	 * doc = xml document of tease
	 * mediaDir = path where media of tease is
	 * timerTarget
	 * delayStyle = [normal(shows time left), secret(shows a timer but not the time), hidden(doesn't show a timer)]
	 * currentPageId
	 * timers
	 * set
	 * prevPages
	 * multiplePagesPattern
	 * autoSetPageWhenSeen
	 * fromPrevPageButton = for not adding page to @prevPages so it doesn't loop
	 * delay
	 * delayDeception
	 * delayStartTime
	 * updateTimer
	 */
	private Document doc;
	private String mediaDir, timerTarget, delayStyle, currentPageId;
	private Map<String, Timer> timers;
	private ArrayList<String> set;
	private ArrayList<Page> prevPages;
	private Pattern multiplePagesPattern;
	private boolean autoSetPageWhenSeen, fromPrevPageButton;
	private Random rand = new Random();
	private int delay, delayDeception;
	private long delayStartTime;
	private Timer updateTimer;

	/**
	 * timerTextView = shows timer in the top right
	 * imageView = shows images on the left
	 * editText = shows text on the right
	 * buttonsLayout
	 * cheats = group of cheat buttons in the top left
	 * homeButtons = group of buttons on home page on the right
	 * audioPlayer
	 * editTextScrollView
	 * newDocButton =
	 * pauseTimerButton
	 * settingsButton
	 * skipTimerButton
	 * pageIdViewButton
	 * teaseButton
	 * saveButton
	 * removeSaveButton
	 * prevPageButton
	 */
	private TextView timerTextView;
	private ImageView imageView;
	private WebView editText;
	private LinearLayout buttonsLayout, cheats, homeButtons, noStorageLayout;
	private MediaPlayer audioPlayer;
	private ScrollView editTextScrollView;
	private Button newDocButton, pauseTimerButton, skipTimerButton, pageIdViewButton, teaseButton, saveButton, removeSaveButton, prevPageButton, settingsButton,
			downloadButton;
	private LinearLayout.LayoutParams noButtonsLayoutParams, yesButtonsLayoutParams;
	private VideoView videoView;

	private static void setImage(ImageView imageView, String path){
		BitmapWorkerTask task = new BitmapWorkerTask(imageView);
		task.execute(path);
	}

	private static void setImage(ImageView imageView, int id){
		imageView.setImageResource(id);
	}

	static void saveDocument(Document document, Object obj){
		File file;
		if(obj instanceof File) file = (File) obj;
		else file = new File(obj.toString());
		try{
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(file));
		}catch(TransformerException e){
			Log.e(TAG, "saveDocument: ", e);
		}
	}

	static boolean boolFromXmlElement(String elementName){
		return MainActivity.data.getElementsByTagName(elementName).item(0).getAttributes().getNamedItem("value").getNodeValue().equals(TRUE);
	}

	static String stringFromCheckBox(CompoundButton v){
		return v.isChecked() ? TRUE : FALSE;
	}

	private int dpToPx(int dp){
		return (int) ((dp * getResources().getDisplayMetrics().density) + 0.5);
	}

	private void changeNewDocButton(String string){
		String[] tasks = string.split("\\|");
		for(String task : tasks){
			if(task.equals("visible")) newDocButton.setVisibility(Button.VISIBLE);
			if(task.equals("invisible")) newDocButton.setVisibility(Button.INVISIBLE);
			if(task.equals("start")) newDocButton.setText(R.string.start);
			if(task.equals("new") || task.equals("invisible")) newDocButton.setText(R.string._new);
		}
	}

	protected void onStop(){
		Element lastTeaseE = (Element) data.getElementsByTagName(getString(R.string.root_misc_lastTease)).item(0);
		lastTeaseE.setTextContent(teaseButton.getText().toString());
		saveDocument(data, dataFile);
		super.onStop();
	}

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		makeFullscreen();
		setContentView(R.layout.activity_main);
		initVars();
		setupDataFile();
		if(data != null) startupActions();
		else setContentView(noStorageLayout);
	}

	private void startupActions(){
		TEASES_DIR = data.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).item(0).getTextContent();
		File teasesDir = new File(TEASES_DIR);
		boolean exists = teasesDir.exists();
		if(!exists) exists = teasesDir.mkdirs();
		if(!exists) Toast.makeText(this, "Teases Directory specified not found", Toast.LENGTH_LONG).show();
		if(data.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).item(0).getAttributes().getNamedItem("value").getNodeValue().equals("true")){
			audioPlayer = MediaPlayer.create(this, R.raw.hey);
			audioPlayer.start();
		}
		if(data.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).item(0).getAttributes().getNamedItem("value").getNodeValue().equals("true"))
			setImage(imageView, R.drawable.welcome);
		editText.setWebViewClient(new MyWebViewClient());
		editText.loadData(surroundInBody(""), "text/html", "UTF-8");
		if(boolFromXmlElement(getString(R.string.root_settings_rememberLastTease))){
			String lastTease = data.getElementsByTagName(getString(R.string.root_misc_lastTease)).item(0).getTextContent();
			if(lastTease.isEmpty()) teaseButton.setText(R.string.none);
			else teaseButton.setText(lastTease);
		}
		if(!boolFromXmlElement(getString(R.string.root_settings_cheats_pageID))) pageIdViewButton.setVisibility(View.GONE);
		if(!boolFromXmlElement(getString(R.string.root_settings_cheats_pauseTimer))) pauseTimerButton.setVisibility(View.GONE);
		if(!boolFromXmlElement(getString(R.string.root_settings_cheats_skipTimer))) skipTimerButton.setVisibility(View.GONE);
		if(!boolFromXmlElement(getString(R.string.root_settings_cheats_removeSave))) removeSaveButton.setVisibility(View.GONE);
		if(!boolFromXmlElement(getString(R.string.root_settings_cheats_prevPage))) prevPageButton.setVisibility(View.GONE);
	}

	private void initVars(){
		videoView = (VideoView) findViewById(R.id.videoView);
		teaseButton = (Button) findViewById(R.id.teaseButton);
		imageView = (ImageView) (findViewById(R.id.imageView));
		cheats = (LinearLayout) findViewById(R.id.cheats);
		editText = (WebView) findViewById(R.id.teaseWebView);
		pauseTimerButton = (Button) findViewById(R.id.pauseTimerButton);
		skipTimerButton = (Button) findViewById(R.id.skipTimerButton);
		homeButtons = (LinearLayout) findViewById(R.id.homeButtons);
		pageIdViewButton = (Button) findViewById(R.id.pageIdViewButton);
		saveButton = (Button) findViewById(R.id.saveButton);
		removeSaveButton = (Button) findViewById(R.id.removeSaveButton);
		downloadButton = (Button) findViewById(R.id.downloadButton);
		prevPageButton = (Button) findViewById(R.id.prevPageButton);
		buttonsLayout = (LinearLayout) findViewById(R.id.buttonsLayout);
		timerTextView = (TextView) findViewById(R.id.timerTextView);
		editTextScrollView = (ScrollView) findViewById(R.id.editTextScrollView);
		newDocButton = (Button) findViewById(R.id.newDocButton);
		settingsButton = (Button) findViewById(R.id.settingsButton);
		timers = new HashMap<>();
		fromPrevPageButton = false;
		set = new ArrayList<>();
		prevPages = new ArrayList<>();
		noButtonsLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		yesButtonsLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(270));
		multiplePagesPattern = Pattern.compile("(\\w+)\\((\\d+)\\.\\.(\\d+)\\)");
		audioPlayer = new MediaPlayer();
		noStorageLayout = new LinearLayout(this);
		delayStyle = "";

		noStorageLayout.setBackgroundColor(getColor(R.color.colorPrimary));
		TextView tv = new TextView(this);
		tv.setText(R.string.no_storage_perms);
		noStorageLayout.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

		newDocButton.setOnClickListener(this);
		imageView.setOnClickListener(this);
		pauseTimerButton.setOnClickListener(this);
		skipTimerButton.setOnClickListener(this);
		pageIdViewButton.setOnClickListener(this);
		teaseButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		removeSaveButton.setOnClickListener(this);
		prevPageButton.setOnClickListener(this);
		downloadButton.setOnClickListener(this);
		settingsButton.setOnClickListener(this);
		videoView.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View v, MotionEvent event){
				imageView.callOnClick();
				return false;
			}
		});
	}

	private void setupDataFile(){
		String MAIN_DIR = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + getApplication().getPackageName() + "/";
		File mainDir = new File(MAIN_DIR);
		boolean exists = mainDir.exists();
		if(!exists) exists = mainDir.mkdirs();
		if(!exists) Toast.makeText(this, "Could not create data file. Please check storage permissions for this app", Toast.LENGTH_LONG).show();
		dataFile = new File(MAIN_DIR + "data.xml");
		if(!dataFile.exists()) try{
			data = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = data.createElement("Root");
			data.appendChild(root);
			saveDocument(data, dataFile);
		}catch(ParserConfigurationException e){
			Log.e(TAG, "onCreate: ", e);
		}
		data = openDocument(dataFile.toString());
		if(data != null){
			Element root = (Element) data.getElementsByTagName(getString(R.string.root)).item(0);
			if(root.getElementsByTagName(getString(R.string.root_saves)).getLength() == 0){
				Element saves = data.createElement(getString(R.string.root_saves));
				root.appendChild(saves);
			}
			if(root.getElementsByTagName(getString(R.string.root_settings)).getLength() == 0){
				Element settings = data.createElement(getString(R.string.root_settings));
				root.appendChild(settings);
			}
			Element settings = (Element) root.getElementsByTagName(getString(R.string.root_settings)).item(0);
			{
				if(settings.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).getLength() == 0){
					Element teaseDir = data.createElement(getString(R.string.root_settings_teasesDirectory));
					teaseDir.setTextContent(Environment.getExternalStorageDirectory().toString() + "/Teases/");
					settings.appendChild(teaseDir);
				}
				if(settings.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).getLength() == 0){
					Element eos = data.createElement(getString(R.string.root_settings_endearOnStartup));
					eos.setAttribute("value", "true");
					settings.appendChild(eos);
				}
				if(settings.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).getLength() == 0){
					Element hpp = data.createElement(getString(R.string.root_settings_homePagePicture));
					hpp.setAttribute("value", "true");
					settings.appendChild(hpp);
				}
				if(settings.getElementsByTagName(getString(R.string.root_settings_rememberLastTease)).getLength() == 0){
					Element rlt = data.createElement(getString(R.string.root_settings_rememberLastTease));
					rlt.setAttribute("value", "true");
					settings.appendChild(rlt);
				}
				if(settings.getElementsByTagName(getString(R.string.root_settings_cheats)).getLength() == 0){
					Element cheats = data.createElement(getString(R.string.root_settings_cheats));
					cheats.setAttribute("value", "true");
					settings.appendChild(cheats);
				}
				Element cheats = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats)).item(0);
				{
					if(cheats.getElementsByTagName(getString(R.string.root_settings_cheats_pageID)).getLength() == 0){
						Element pid = data.createElement(getString(R.string.root_settings_cheats_pageID));
						pid.setAttribute("value", "true");
						cheats.appendChild(pid);
					}
					if(cheats.getElementsByTagName(getString(R.string.root_settings_cheats_pauseTimer)).getLength() == 0){
						Element pt = data.createElement(getString(R.string.root_settings_cheats_pauseTimer));
						pt.setAttribute("value", "true");
						cheats.appendChild(pt);
					}
					if(cheats.getElementsByTagName(getString(R.string.root_settings_cheats_skipTimer)).getLength() == 0){
						Element st = data.createElement(getString(R.string.root_settings_cheats_skipTimer));
						st.setAttribute("value", "true");
						cheats.appendChild(st);
					}
					if(cheats.getElementsByTagName(getString(R.string.root_settings_cheats_removeSave)).getLength() == 0){
						Element rs = data.createElement(getString(R.string.root_settings_cheats_removeSave));
						rs.setAttribute("value", "true");
						cheats.appendChild(rs);
					}
					if(cheats.getElementsByTagName(getString(R.string.root_settings_cheats_prevPage)).getLength() == 0){
						Element pp = data.createElement(getString(R.string.root_settings_cheats_prevPage));
						pp.setAttribute("value", "true");
						cheats.appendChild(pp);
					}
				}
			}
			if(root.getElementsByTagName(getString(R.string.root_misc)).getLength() == 0){
				Element misc = data.createElement(getString(R.string.root_misc));
				root.appendChild(misc);
			}
			Element misc = (Element) data.getElementsByTagName(getString(R.string.root_misc)).item(0);
			if(misc.getElementsByTagName(getString(R.string.root_misc_lastTease)).getLength() == 0){
				Element lastTease = data.createElement(getString(R.string.root_misc_lastTease));
				lastTease.setTextContent("");
				misc.appendChild(lastTease);
			}
		}
	}

	private void addImageToPage(NamedNodeMap attrs){
		if(imageView.getVisibility() == ImageView.GONE){
			imageView.setVisibility(ImageView.VISIBLE);
			videoView.setVisibility(VideoView.GONE);
		}
		String image = processImage(attrs.getNamedItem("id").getNodeValue());
		setImage(imageView, mediaDir + image);
	}

	private void addVideoToPage(NamedNodeMap attrs){
		if(videoView.getVisibility() == ImageView.GONE){
			videoView.setVisibility(ImageView.VISIBLE);
			imageView.setVisibility(VideoView.GONE);
		}
		String video = attrs.getNamedItem("id").getNodeValue();
		videoView.setVideoPath(mediaDir + video);
		Node start_at = attrs.getNamedItem(getString(R.string.doc_startAt));
		if(start_at != null) videoView.seekTo(timeToMs(start_at.getNodeValue()));
		videoView.start();
	}

	private void addTextToPage(Node child){
		editText.loadData(surroundInBody(child.getTextContent()), "text/html", "UTF-8");
		editTextScrollView.setScrollY(0);
	}

	private void addButtonToPage(Node child, NamedNodeMap attrs){
		final Node childSets = attrs.getNamedItem(getString(R.string.doc_set));
		final Node childUnsets = attrs.getNamedItem(getString(R.string.doc_unset));
		Button button = new Button(this);
		button.setText(child.getTextContent());
		button.setTextSize(12);
		button.setAllCaps(false);
		final String target = processTarget(attrs.getNamedItem(getString(R.string.doc_target)).getNodeValue());
		button.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				if(childSets != null) set.addAll(Arrays.asList(childSets.getNodeValue().split("\\|")));
				if(childUnsets != null) set.removeAll(Arrays.asList(childUnsets.getNodeValue().split("\\|")));
				setPage(target);
			}
		});
		buttonsLayout.addView(button);
	}

	private void addDelayToPage(NamedNodeMap attrs){
		skipTimerButton.setEnabled(true);
		pauseTimerButton.setEnabled(true);
		Node childSets = attrs.getNamedItem(getString(R.string.doc_set));
		Node childUnsets = attrs.getNamedItem(getString(R.string.doc_unset));
		Node startsWithNode = attrs.getNamedItem(getString(R.string.doc_startWith));
		delay = processDelay(attrs.getNamedItem(getString(R.string.doc_seconds)).getNodeValue()) * 1000;
		if(startsWithNode != null) delayDeception = Integer.parseInt(startsWithNode.getNodeValue()) * 1000 - delay;
		else delayDeception = 0;
		delayStartTime = System.currentTimeMillis();
		final String target = processTarget(attrs.getNamedItem(getString(R.string.doc_target)).getNodeValue());
		timerTarget = target;
		Node styleNode = attrs.getNamedItem(getString(R.string.doc_style));
		if(styleNode != null) delayStyle = styleNode.getNodeValue().trim();
		else delayStyle = "normal";
		timerTextView.setVisibility(TextView.VISIBLE);
		if(childSets != null) set.addAll(Arrays.asList(childSets.getNodeValue().split("\\|")));
		if(childUnsets != null) set.removeAll(Arrays.asList(childUnsets.getNodeValue().split("\\|")));
		Timer wait = new Timer();
		wait.schedule(new TimerTask(){
			public void run(){
				runOnUiThread(new Runnable(){
					public void run(){
						setPage(target);
					}
				});
			}
		}, delay);
		timers.put("wait", wait);
		timerTextView.setText("??:??");
		timerTextView.setTextColor(Color.WHITE);
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new MyTask(), 0, 1000);
		timers.put("update", updateTimer);
	}

	private void addAudioToPage(NamedNodeMap attrs){
		String audio = attrs.getNamedItem("id").getNodeValue();
		try{
			audioPlayer = new MediaPlayer();
			audioPlayer.setDataSource(mediaDir + audio);
			audioPlayer.prepare();
			audioPlayer.start();
		}catch(IOException e){
			Log.e(TAG, "setPage: ", e);
		}
	}

	private void addMetronomeToPage(NamedNodeMap attrs){
		int bpm = processDelay(attrs.getNamedItem("bpm").getNodeValue());
		Timer metronome = new Timer();
		audioPlayer = MediaPlayer.create(this, R.raw.tick);
		metronome.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				audioPlayer.start();
			}
		}, 0, (int) (1.0 / ((bpm / 60.0) / 1000.0)));
		timers.put("metronome", metronome);
	}

	private void setPage(String pageID){
		try{
			Log.d(TAG, "setPage: " + pageID);
			Element page = doc.getElementById(pageID);
			audioPlayer.stop();
			NodeList children = page.getChildNodes();
			timerTextView.setVisibility(TextView.GONE);
			buttonsLayout.removeAllViews();
			deleteTimers();
			String[] sets = page.getAttribute(getString(R.string.doc_set)).split(",");
			String[] unsets = page.getAttribute(getString(R.string.doc_unset)).split(",");
			timerTarget = "";
			skipTimerButton.setEnabled(false);
			pauseTimerButton.setEnabled(false);
			pauseTimerButton.setText(R.string.pause_timer);
			pageIdViewButton.setText(pageID);
			if(!currentPageId.equals("") && !fromPrevPageButton){
				prevPages.add(new Page(currentPageId, set));
				prevPageButton.setEnabled(true);
			}
			fromPrevPageButton = false;
			for(String setString : sets){
				if(!set.contains(setString)) set.add(setString);
			}
			for(String unsetString : unsets){
				if(set.contains(unsetString)) set.remove(unsetString);
			}
			currentPageId = pageID;
			saveButton.setTag("auto");
			saveButton.callOnClick();
			if(autoSetPageWhenSeen) set.add(pageID);
			allChildren:
			for(int i = 0; i < children.getLength(); i++){
				Node child = children.item(i);
				NamedNodeMap attrs = child.getAttributes();
				if(attrs != null){
					Node ifSet = attrs.getNamedItem(getString(R.string.doc_ifSet));
					Node ifNotSet = attrs.getNamedItem(getString(R.string.doc_ifNotSet));
					if(ifSet != null) for(String ifSetPart : ifSet.getNodeValue().split("\\|"))
						if(!set.contains(ifSetPart)) continue allChildren;
					if(ifNotSet != null) for(String ifNotSetPart : ifNotSet.getNodeValue().split("\\|"))
						if(set.contains(ifNotSetPart)) continue allChildren;
				}
				if(child.getNodeName().equals(getString(R.string.doc_image))) addImageToPage(attrs);
				if(child.getNodeName().equals(getString(R.string.doc_video))) addVideoToPage(attrs);
				if(child.getNodeName().equals(getString(R.string.doc_text))) addTextToPage(child);
				if(child.getNodeName().equals(getString(R.string.doc_button))) addButtonToPage(child, attrs);
				if(child.getNodeName().equals(getString(R.string.doc_delay))) addDelayToPage(attrs);
				if(child.getNodeName().equals(getString(R.string.doc_audio))) addAudioToPage(attrs);
				if(child.getNodeName().equals(getString(R.string.doc_metronome))) addMetronomeToPage(attrs);
			}
			if(buttonsLayout.getChildCount() == 0) editTextScrollView.setLayoutParams(noButtonsLayoutParams);
			else editTextScrollView.setLayoutParams(yesButtonsLayoutParams);
		}catch(NullPointerException e){
			Log.e(TAG, "setPage: ", e);
		}
	}

	private int timeToMs(String time){
		int[] parts = new int[3];
		String[] split = time.split(":");
		for(int i = 0; i < split.length; i++)
			parts[i] = Integer.parseInt(split[i]);
		return parts[0] * 60 * 60 * 1000 + parts[1] * 60 * 1000 + parts[2] * 1000;
	}

	private String processTarget(String target){ //range(from:4,to:17)
		Matcher matcher = multiplePagesPattern.matcher(target);
		if(matcher.find()){
			long start = Long.parseLong(matcher.group(2));
			long end = Long.parseLong(matcher.group(3));
			ArrayList<String> allowed = new ArrayList<>();
			for(long j = start; j < end; j++){
				String num = String.valueOf(j);
				String processedTarget = "";
				if(!set.contains((matcher.group(1).equals("page") ? num : matcher.group(1) + num))){
					if(matcher.group(1).equals("page")) processedTarget = num;
					else processedTarget = matcher.group(1) + num;
				}
				if(processedTarget.equals("")){
					NodeList pages = doc.getElementsByTagName(getString(R.string.doc_page));
					for(int i = 0; i < pages.getLength(); i++){
						Node page = pages.item(i);
						NamedNodeMap attrs = page.getAttributes();
						String id = attrs.getNamedItem("id").getNodeValue();
						if(id.equals(matcher.group(1) + num)){
							Node ifNotSetNode = attrs.getNamedItem("if-not-set");
							if(ifNotSetNode != null){
								if(set.contains(ifNotSetNode.getNodeValue())) processedTarget = id;
							}
						}
					}
				}
				if(!processedTarget.equals("")) allowed.add(processedTarget);
			}
			return allowed.get(rand.nextInt(allowed.size()));
		}else if(target.startsWith("range")){
			target = target.substring(target.indexOf('(') + 1, target.length() - 1);
			String[] parts = target.split(",");
			String fromS = parts[0].substring(parts[0].indexOf(':') + 1);
			String toS = parts[1].substring(parts[1].indexOf(':') + 1);
			int from = Integer.parseInt(fromS);
			int to = Integer.parseInt(toS);
			return String.valueOf(rand.nextInt(to) + from);
		}
		return target;
	}

	private void deleteTimers(){
		for(Timer timer : timers.values()){
			timer.cancel();
			timer.purge();
		}
		timers.clear();
	}

	public void onClick(View v){
		if(v.equals(imageView) || v.equals(videoView)) onMediaViewButtonClicked();
		else if(v.equals(teaseButton)) onTeaseButtonClicked();
		else if(v.equals(skipTimerButton)) onSkipTimerButtonClicked();
		else if(v.equals(settingsButton)) onSettingsButtonClicked();
		else if(v.equals(pauseTimerButton)) onPauseTimerButtonClicked();
		else if(v.equals(pageIdViewButton)) onPageIdViewButtonClicked();
		else if(v.equals(saveButton)) onSaveButtonClicked();
		else if(v.equals(removeSaveButton)) onRemoveSaveButtonClicked();
		else if(v.equals(prevPageButton)) onPrevPageButtonClicked();
		else if(v.equals(downloadButton)) onDownloadButtonClicked();
		else if(v.equals(newDocButton)) onNewDocButtonClicked();
	}

	private void onMediaViewButtonClicked(){
		if(newDocButton.getVisibility() == Button.INVISIBLE){
			newDocButton.setVisibility(Button.VISIBLE);
			if(boolFromXmlElement(getString(R.string.root_settings_cheats))) cheats.setVisibility(LinearLayout.VISIBLE);
		}else if(doc != null && homeButtons.getVisibility() == EditText.GONE){
			changeNewDocButton("invisible");
			cheats.setVisibility(View.GONE);
		}
	}

	private void onTeaseButtonClicked(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.dialog_xml_chooser);
		final LinearLayout textViewsLayout = (LinearLayout) dialog.findViewById(R.id.textViewsLayout);
		final ScrollView scrollView = (ScrollView) dialog.findViewById(R.id.scrollView);
		File[] filesList = new File(TEASES_DIR).listFiles();
		boolean current = false;
		int scrollY = 0;
		if(filesList == null || filesList.length == 0) dialog.setTitle("No teases found");
		else{
			Arrays.sort(filesList);
			dialog.setTitle("Choose tease");
			for(File file : filesList){
				if(file.getName().endsWith(".xml")){
					final Button fileButton = new Button(this);
					fileButton.setText(file.getName().substring(0, file.getName().length() - 4));
					fileButton.setTextSize(14);
					fileButton.setAllCaps(false);
					fileButton.setEllipsize(TextUtils.TruncateAt.MIDDLE);
					fileButton.setLongClickable(true);
					fileButton.setOnClickListener(new View.OnClickListener(){
						public void onClick(View v){
							teaseButton.setText(fileButton.getText());
							dialog.dismiss();
						}
					});
					fileButton.setOnLongClickListener(new View.OnLongClickListener(){
						public boolean onLongClick(View v){
							DialogInterface.OnClickListener handler = new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface dialog, int which){
									if(which == DialogInterface.BUTTON_POSITIVE){
										File docF = new File(TEASES_DIR + fileButton.getText().toString() + ".xml");
										Document doc1 = openDocument(docF.getAbsolutePath());
										if(doc1 != null){
											deleteFile(new File(TEASES_DIR + doc1.getElementsByTagName(getString(R.string.doc_mediaDirectory)).item(0).getTextContent
													()));
											deleteFile(docF);
											Node node;
											if((node = data.getElementById(md5(teaseButton.getText().toString()))) != null)
												data.getElementsByTagName(getString(R.string.root_saves)).item(0).removeChild(node);
											if(teaseButton.getText().equals(fileButton.getText())) teaseButton.setText(R.string.none);
											textViewsLayout.removeView(fileButton);
										}else Toast.makeText(MainActivity.this, "Could not delete '" + fileButton.getText() + "'", Toast.LENGTH_LONG).show();
									}else dialog.dismiss();
								}
							};//@formatter:off
							AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
									.setTitle("Delete")
									.setMessage("Do you want to delete '" + fileButton.getText() + "'?")
									.setPositiveButton("Delete", handler)
									.setNegativeButton("Cancel", handler)
									.create();//@formatter:on
							alertDialog.show();
							return false;
						}
					});
					if((teaseButton.getText().toString() + ".xml").equals(file.getName())) current = true;
					if(!current) scrollY++;
					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					                                                                       LinearLayout.LayoutParams.MATCH_PARENT);
					layoutParams.bottomMargin = 5;
					textViewsLayout.addView(fileButton, layoutParams);
				}
			}
			if(!current) scrollY = 0;
			final int scroll = scrollY * 173;
			scrollView.post(new Runnable(){
				public void run(){
					scrollView.scrollTo(0, scroll);
				}
			});
		}
		dialog.show();
	}

	private void onSkipTimerButtonClicked(){
		setPage(timerTarget);
	}

	private void onSettingsButtonClicked(){
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);

	}

	private void onPauseTimerButtonClicked(){
		if(pauseTimerButton.getText().toString().equals(getString(R.string.pause_timer))){
			pauseTimerButton.setText(R.string.resume_timer);
			if(timers.get("wait") != null && timers.get("update") != null){
				timers.get("wait").cancel();
				timers.get("wait").purge();
				timers.get("update").cancel();
				timers.get("update").purge();
				timers.remove("wait");
				timers.remove("update");
				delay = (int) (delayStartTime + delay - System.currentTimeMillis());
			}
		}else if(pauseTimerButton.getText().toString().equals(getString(R.string.resume_timer))){
			pauseTimerButton.setText(R.string.pause_timer);
			Timer wait = new Timer();
			wait.schedule(new TimerTask(){
				public void run(){
					runOnUiThread(new Runnable(){
						public void run(){
							setPage(timerTarget);
						}
					});
				}
			}, delay);
			timers.put("wait", wait);
			delayStartTime = System.currentTimeMillis();
			updateTimer = new Timer();
			updateTimer.scheduleAtFixedRate(new MyTask(), 0, 1000);
			timers.put("update", updateTimer);
		}
	}

	private void onPageIdViewButtonClicked(){
		NodeList pages = doc.getElementsByTagName(getString(R.string.doc_page));
		if(!timerTarget.equals("")) pauseTimerButton.callOnClick();
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.dialog_xml_chooser);
		LinearLayout textViewsLayout = (LinearLayout) dialog.findViewById(R.id.textViewsLayout);
		final ScrollView scrollView = (ScrollView) dialog.findViewById(R.id.scrollView);
		boolean current = false;
		int scrollY = 0;
		for(int i = 0; i < pages.getLength(); i++){
			Element page = (Element) pages.item(i);
			final String pageId = page.getAttribute("id");
			Button button = new Button(this);
			button.setText(pageId);
			button.setTextSize(14);
			button.setAllCaps(false);
			button.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			button.setOnClickListener(new View.OnClickListener(){
				public void onClick(View v){
					dialog.dismiss();
					imageView.callOnClick();
					setPage(pageId);
				}
			});
			if(pageId.equals(currentPageId)) current = true;
			if(!current) scrollY++;
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			layoutParams.bottomMargin = 5;
			textViewsLayout.addView(button, layoutParams);
		}
		if(!current) scrollY = 0;
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
			public void onCancel(DialogInterface dialog){
				if(!timerTarget.equals("")) pauseTimerButton.callOnClick();
			}
		});
		dialog.show();
		final int scroll = scrollY * 173;
		scrollView.post(new Runnable(){
			public void run(){
				scrollView.scrollTo(0, scroll);
			}
		});
	}

	private void onSaveButtonClicked(){
		Element saves = (Element) data.getElementsByTagName(getString(R.string.root_saves)).item(0);
		Node node;
		if((node = data.getElementById(md5(teaseButton.getText().toString()))) != null) saves.removeChild(node);
		Element save = data.createElement("Save");
		save.setAttribute("id", md5(teaseButton.getText().toString()));
		Element tease = data.createElement("Tease");
		tease.setTextContent(teaseButton.getText().toString());
		save.appendChild(tease);
		Element pageId = data.createElement("Page");
		pageId.setTextContent(currentPageId);
		save.appendChild(pageId);
		Element setE = data.createElement("Sets");
		setE.setTextContent(Arrays.toString(set.toArray()));
		save.appendChild(setE);
		Element prevPagesE = data.createElement("PrevPages");
		prevPagesE.setTextContent(prevPages.toString());
		save.appendChild(prevPagesE);
		saves.appendChild(save);
		saveDocument(data, dataFile);
		if(saveButton.getTag().equals("manual")) Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
		saveButton.setTag("manual");
	}

	private void onRemoveSaveButtonClicked(){
		Element saves = (Element) data.getElementsByTagName(getString(R.string.root_saves)).item(0);
		Node node;
		if((node = data.getElementById(md5(teaseButton.getText().toString()))) != null) saves.removeChild(node);
		Toast.makeText(this, "Removed Save", Toast.LENGTH_SHORT).show();
	}

	private void onPrevPageButtonClicked(){
		Page prevPage = prevPages.remove(prevPages.size() - 1);
		if(prevPages.size() == 0) prevPageButton.setEnabled(false);
		set = prevPage.getSets();
		fromPrevPageButton = true;
		setPage(prevPage.getId());
	}

	private void onDownloadButtonClicked(){
		Intent intent = new Intent(this, DownloadActivity.class);
		startActivity(intent);
	}

	private void onNewDocButtonClicked(){
		if(doc != null && homeButtons.getVisibility() != EditText.GONE){ // Home to Tease
			homeButtons.setVisibility(View.GONE);
			mediaDir = TEASES_DIR + doc.getElementsByTagName(getString(R.string.doc_mediaDirectory)).item(0).getTextContent() + "/";
			Node n;
			if((n = doc.getElementsByTagName(getString(R.string.doc_autoSetPageWhenSeen)).item(0)) != null) autoSetPageWhenSeen = n.getTextContent().equals("true");
			changeNewDocButton("invisible");
			set.clear();
			prevPages.clear();
			prevPageButton.setEnabled(false);
			currentPageId = "";
			Node node;
			if((node = data.getElementById(md5(teaseButton.getText().toString()))) != null){
				Element element = (Element) node;
				String sets = element.getElementsByTagName(getString(R.string.save_sets)).item(0).getTextContent();
				set.addAll(Arrays.asList(sets.substring(1, sets.length() - 1).split(",")));
				prevPages = Page.stringToPages(element.getElementsByTagName(getString(R.string.save_prevPages)).item(0).getTextContent());
				if(prevPages.size() > 0) prevPageButton.setEnabled(true);
				setPage(element.getElementsByTagName(getString(R.string.save_page)).item(0).getTextContent());
			}else setPage("start");
		}else if(doc != null){ // Tease to Home
			doc = null;
			homeButtons.setVisibility(EditText.VISIBLE);
			changeNewDocButton("start|stop");
			buttonsLayout.removeAllViews();
			videoView.setVisibility(VideoView.GONE);
			cheats.setVisibility(View.GONE);
			imageView.setVisibility(ImageView.VISIBLE);
			editTextScrollView.setLayoutParams(yesButtonsLayoutParams);
			mediaDir = "";
			if(boolFromXmlElement(getString(R.string.root_settings_homePagePicture))) setImage(imageView, R.drawable.welcome);
			else setImage(imageView, android.R.color.transparent);
			editText.loadData(surroundInBody(""), "text/html", "UTF-8");
			audioPlayer.stop();
			if(boolFromXmlElement(getString(R.string.root_settings_endearOnStartup))){
				audioPlayer = MediaPlayer.create(this, R.raw.hey);
				audioPlayer.start();
			}
			deleteTimers();
			timerTextView.setVisibility(TextView.GONE);
		}else{
			doc = openDocument(TEASES_DIR + teaseButton.getText().toString() + ".xml");
			if(doc != null) newDocButton.callOnClick();
			else Toast.makeText(this, "No tease selected", Toast.LENGTH_SHORT).show();
		}
	}

	private String md5(String string){
		try{
			return new BigInteger(1, MessageDigest.getInstance("MD5").digest(string.getBytes())).toString();
		}catch(NoSuchAlgorithmException e){
			return "";
		}
	}

	private void deleteFile(File fileOrDirectory){
		if(fileOrDirectory.isDirectory()) for(File child : fileOrDirectory.listFiles())
			deleteFile(child);
		if(!fileOrDirectory.delete()) Toast.makeText(this, "Could not delete " + fileOrDirectory.getName(), Toast.LENGTH_LONG).show();
	}

	private String toTime(long time){
		long total_seconds = time / 1000 + 1;
		long min = total_seconds / 60;
		long seconds = total_seconds - min * 60;
		return String.format(Locale.getDefault(), "%d:%d", min, seconds);
	}

	private Document openDocument(String path){
		try{
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
		}catch(FileNotFoundException e){
			Log.e(TAG, "openDocument: fileNotFound");
		}catch(ParserConfigurationException | SAXException | IOException e){
			Log.e(TAG, "openDocument: ", e);
		}
		return null;
	}

	private String processImage(String image){
		int index = image.indexOf("*");
		if(index != -1){
			image = image.substring(0, index);
			index = image.lastIndexOf("/");
			String name = image;
			if(index != -1){
				name = image.substring(index + 1);
				image = image.substring(0, index);
			}
			File imageFile = new File(mediaDir + image);
			File[] files;
			if(imageFile.isDirectory()){
				files = imageFile.listFiles();
			}else files = new File(mediaDir).listFiles();
			ArrayList<String> fileNamesList = new ArrayList<>();
			for(File file : files){
				if(file.getName().startsWith(name)){
					fileNamesList.add((imageFile.isDirectory() ? image + "/" : "") + file.getName());
				}
			}
			int num = rand.nextInt(fileNamesList.size());
			return fileNamesList.get(num);
		}
		return image;
	}

	public void onBackPressed(){
		if(doc != null && homeButtons.getVisibility() == View.GONE){
			newDocButton.callOnClick();
		}else{
			finish();
		}
	}

	private int processDelay(String delay){
		Matcher matcher = Pattern.compile("\\((\\d+)\\.\\.(\\d+)\\)").matcher(delay);
		if(matcher.find()){
			int start = Integer.parseInt(matcher.group(1));
			int end = Integer.parseInt(matcher.group(2));
			return rand.nextInt(end - start) + start;
		}
		return Integer.parseInt(delay);
	}

	private void makeFullscreen(){
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View
						.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}

	private String surroundInBody(String text){
		return String.format("<body bgcolor=\"black\" text=\"white\">%s</body>", text);
	}

	private static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{
		private final WeakReference<ImageView> imageViewReference;
		private String data = "Test";

		BitmapWorkerTask(ImageView imageView){
			imageViewReference = new WeakReference<>(imageView);
		}

		protected Bitmap doInBackground(String... params){
			data = params[0];
			return decodeSampledBitmapFromFile(data, 100, 100);
		}

		protected void onPostExecute(Bitmap bitmap){
			if(bitmap != null){ // imageViewReference != null
				final ImageView imageView = imageViewReference.get();
				if(imageView != null){
					imageView.setImageBitmap(bitmap);
				}
			}
		}

		int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if(height > reqHeight || width > reqWidth){
				int shortHeight = 0;
				int shortWidth = 0;
				if(height / reqHeight > width / reqWidth){
					shortHeight = reqHeight;
					shortWidth = shortWidth * reqHeight / shortHeight;
				}else{
					shortWidth = reqWidth;
					shortHeight = shortHeight * reqWidth / shortWidth;
				}

				while((shortHeight / inSampleSize) > reqHeight && (shortWidth / inSampleSize) > reqWidth){
					inSampleSize *= 2;
				}
			}
			return inSampleSize;
		}

		Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight){
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeFile(path, options);
		}
	}

	private class MyWebViewClient extends WebViewClient{
		public void onPageFinished(WebView view, String url){
			view.setInitialScale((int) (100 * getResources().getDisplayMetrics().density)); //view.getScale()
		}
	}

	private class MyTask extends TimerTask{
		public void run(){
			runOnUiThread(new Runnable(){
				public void run(){
					long timeLeft = delayStartTime + delay - System.currentTimeMillis();
					if(timeLeft < 0){
						updateTimer.cancel();
						updateTimer.purge();
					}
					if(delayStyle.equals("secret")){
						if(timerTextView.getCurrentTextColor() == Color.WHITE) timerTextView.setTextColor(Color.RED);
						else timerTextView.setTextColor(Color.WHITE);
					}else if(delayStyle.equals("normal")){
						timerTextView.setText(toTime(timeLeft + delayDeception));
					}
				}
			});
		}
	}

}
