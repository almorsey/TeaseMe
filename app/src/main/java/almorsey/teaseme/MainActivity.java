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

public class MainActivity extends Activity implements View.OnClickListener {

	/**
	 * TEASES_DIR = directory where all teases are located
	 * data = xml document including saves, setttings and misc info
	 * dataFile = File object of @data xml file
	 */
	private static final String TAG = "almorsey";
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
	private LinearLayout buttonsLayout, cheats, homeButtons;
	private MediaPlayer audioPlayer;
	private ScrollView editTextScrollView;
	private Button newDocButton, pauseTimerButton, settingsButton, skipTimerButton, pageIdViewButton, teaseButton, saveButton, removeSaveButton, prevPageButton, downloadButton;
	private LinearLayout.LayoutParams noButtonsLayoutParams, yesButtonsLayoutParams;
	private VideoView videoView;

	private static void setImage(ImageView imageView, String path) {
		BitmapWorkerTask task = new BitmapWorkerTask(imageView);
		task.execute(path);
	}

	private static void setImage(ImageView imageView, int id) {
		imageView.setImageResource(id);
	}

	static void saveDocument(Document document, Object obj) {
		File file;
		if (obj instanceof File) file = (File) obj;
		else file = new File(obj.toString());
		try {
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(file));
		} catch (TransformerException e) {
			Log.e(TAG, "saveDocument: ", e);
		}
	}

	static boolean boolFromXmlElement(String elementName) {
		return MainActivity.data.getElementsByTagName(elementName).item(0).getAttributes().getNamedItem("value").getNodeValue().equals(TRUE);
	}

	static String stringFromCheckBox(CompoundButton v) {
		return v.isChecked() ? TRUE : FALSE;
	}

	private int dpToPx(int dp) {
		return (int) ((dp * getResources().getDisplayMetrics().density) + 0.5);
	}

	private void changeNewDocButton(String string) {
		String[] tasks = string.split("\\|");
		for (String task : tasks) {
			if (task.equals("visible")) newDocButton.setVisibility(Button.VISIBLE);
			if (task.equals("invisible")) newDocButton.setVisibility(Button.INVISIBLE);
			if (task.equals("start")) newDocButton.setText(R.string.start);
			if (task.equals("new") || task.equals("invisible")) newDocButton.setText(R.string._new);
		}
	}

	protected void onStop() {
		Element lastTeaseE = (Element) data.getElementsByTagName("LastTease").item(0);
		lastTeaseE.setTextContent(teaseButton.getText().toString());
		saveDocument(data, dataFile);
		super.onStop();
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		makeFullscreen();
		setContentView(R.layout.activity_main);
		initVars();
		setupDataFile();
		startupActions();
	}

	private void startupActions() {
		TEASES_DIR = data.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).item(0).getTextContent();
		File teasesDir = new File(TEASES_DIR);
		if (!teasesDir.exists()) teasesDir.mkdirs();
		if (data.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).item(0).getAttributes().getNamedItem("value").getNodeValue().equals("true")) {
			audioPlayer = MediaPlayer.create(this, R.raw.hey);
			audioPlayer.start();
		}
		if (data.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).item(0).getAttributes().getNamedItem("value").getNodeValue().equals("true"))
			setImage(imageView, R.drawable.welcome);
		editText.setWebViewClient(new MyWebViewClient());
		editText.loadData(surroundInBody(""), "text/html", "UTF-8");
		if (boolFromXmlElement(getString(R.string.root_settings_rememberLastTease))) {
			String lastTease = data.getElementsByTagName(getString(R.string.root_misc_lastTease)).item(0).getTextContent();
			if (lastTease.isEmpty()) teaseButton.setText(R.string.none);
			else teaseButton.setText(lastTease);
		}
		if (!boolFromXmlElement(getString(R.string.root_settings_cheats_pageID))) pageIdViewButton.setVisibility(View.GONE);
		if (!boolFromXmlElement(getString(R.string.root_settings_cheats_pauseTimer))) pauseTimerButton.setVisibility(View.GONE);
		if (!boolFromXmlElement(getString(R.string.root_settings_cheats_skipTimer))) skipTimerButton.setVisibility(View.GONE);
		if (!boolFromXmlElement(getString(R.string.root_settings_cheats_removeSave))) removeSaveButton.setVisibility(View.GONE);
		if (!boolFromXmlElement(getString(R.string.root_settings_cheats_prevPage))) prevPageButton.setVisibility(View.GONE);
	}

	private void initVars() {
		videoView = (VideoView) findViewById(R.id.videoView);
		teaseButton = (Button) findViewById(R.id.teaseButton);
		imageView = (ImageView) (findViewById(R.id.imageView));
		cheats = (LinearLayout) findViewById(R.id.cheats);
		editText = (WebView) findViewById(R.id.teaseWebView);
		pauseTimerButton = (Button) findViewById(R.id.pauseTimerButton);
		skipTimerButton = (Button) findViewById(R.id.skipTimerButton);
		settingsButton = (Button) findViewById(R.id.settingsButton);
		homeButtons = (LinearLayout) findViewById(R.id.homeButtons);
		pageIdViewButton = (Button) findViewById(R.id.pageIdViewButton);
		saveButton = (Button) findViewById(R.id.saveButton);
		removeSaveButton = (Button) findViewById(R.id.removeSaveButton);
		prevPageButton = (Button) findViewById(R.id.prevPageButton);
		downloadButton = (Button) findViewById(R.id.downloadButton);
		buttonsLayout = (LinearLayout) findViewById(R.id.buttonsLayout);
		timerTextView = (TextView) findViewById(R.id.timerTextView);
		editTextScrollView = (ScrollView) findViewById(R.id.editTextScrollView);
		newDocButton = (Button) findViewById(R.id.newDocButton);
		timers = new HashMap<>();
		fromPrevPageButton = false;
		set = new ArrayList<>();
		prevPages = new ArrayList<>();
		noButtonsLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		yesButtonsLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(250));
		multiplePagesPattern = Pattern.compile("(\\w+)\\((\\d+)\\.\\.(\\d+)\\)");
		audioPlayer = new MediaPlayer();

		newDocButton.setOnClickListener(this);
		imageView.setOnClickListener(this);
		teaseButton.setOnClickListener(this);
		pauseTimerButton.setOnClickListener(this);
		skipTimerButton.setOnClickListener(this);
		pageIdViewButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		removeSaveButton.setOnClickListener(this);
		prevPageButton.setOnClickListener(this);
		settingsButton.setOnClickListener(this);
		downloadButton.setOnClickListener(this);
		videoView.setOnClickListener(this);
		videoView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				imageView.callOnClick();
				return false;
			}
		});
	}

	private void setupDataFile() {
		String MAIN_DIR = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + getApplication().getPackageName() + "/";
		File mainDir = new File(MAIN_DIR);
		if (!mainDir.exists()) mainDir.mkdirs();
		dataFile = new File(MAIN_DIR + "data.xml");
		if (!dataFile.exists()) try {
			data = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = data.createElement("Root");
			data.appendChild(root);
			saveDocument(data, dataFile);
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "onCreate: ", e);
		}
		data = openDocument(dataFile.toString());
		Element root = (Element) data.getElementsByTagName(getString(R.string.root)).item(0);
		if (root.getElementsByTagName(getString(R.string.root_saves)).getLength() == 0) {
			Element saves = data.createElement(getString(R.string.root_saves));
			root.appendChild(saves);
		}
		if (root.getElementsByTagName(getString(R.string.root_settings)).getLength() == 0) {
			Element settings = data.createElement(getString(R.string.root_settings));
			root.appendChild(settings);
		}
		Element settings = (Element) root.getElementsByTagName(getString(R.string.root_settings)).item(0);
		{
			if (settings.getElementsByTagName(getString(R.string.root_settings_teasesDirectory)).getLength() == 0) {
				Element teaseDir = data.createElement(getString(R.string.root_settings_teasesDirectory));
				teaseDir.setTextContent(Environment.getExternalStorageDirectory().toString() + "/Teases/");
				settings.appendChild(teaseDir);
			}
			if (settings.getElementsByTagName(getString(R.string.root_settings_endearOnStartup)).getLength() == 0) {
				Element eos = data.createElement(getString(R.string.root_settings_endearOnStartup));
				eos.setAttribute("value", "true");
				settings.appendChild(eos);
			}
			if (settings.getElementsByTagName(getString(R.string.root_settings_homePagePicture)).getLength() == 0) {
				Element hpp = data.createElement(getString(R.string.root_settings_homePagePicture));
				hpp.setAttribute("value", "true");
				settings.appendChild(hpp);
			}
			if (settings.getElementsByTagName(getString(R.string.root_settings_rememberLastTease)).getLength() == 0) {
				Element rlt = data.createElement(getString(R.string.root_settings_rememberLastTease));
				rlt.setAttribute("value", "true");
				settings.appendChild(rlt);
			}
			if (settings.getElementsByTagName(getString(R.string.root_settings_cheats)).getLength() == 0) {
				Element cheats = data.createElement(getString(R.string.root_settings_cheats));
				cheats.setAttribute("value", "true");
				settings.appendChild(cheats);
			}
			Element cheats = (Element) data.getElementsByTagName(getString(R.string.root_settings_cheats)).item(0);
			{
				if (cheats.getElementsByTagName(getString(R.string.root_settings_cheats_pageID)).getLength() == 0) {
					Element pid = data.createElement(getString(R.string.root_settings_cheats_pageID));
					pid.setAttribute("value", "true");
					cheats.appendChild(pid);
				}
				if (cheats.getElementsByTagName(getString(R.string.root_settings_cheats_pauseTimer)).getLength() == 0) {
					Element pt = data.createElement(getString(R.string.root_settings_cheats_pauseTimer));
					pt.setAttribute("value", "true");
					cheats.appendChild(pt);
				}
				if (cheats.getElementsByTagName(getString(R.string.root_settings_cheats_skipTimer)).getLength() == 0) {
					Element st = data.createElement(getString(R.string.root_settings_cheats_skipTimer));
					st.setAttribute("value", "true");
					cheats.appendChild(st);
				}
				if (cheats.getElementsByTagName(getString(R.string.root_settings_cheats_removeSave)).getLength() == 0) {
					Element rs = data.createElement(getString(R.string.root_settings_cheats_removeSave));
					rs.setAttribute("value", "true");
					cheats.appendChild(rs);
				}
				if (cheats.getElementsByTagName(getString(R.string.root_settings_cheats_prevPage)).getLength() == 0) {
					Element pp = data.createElement(getString(R.string.root_settings_cheats_prevPage));
					pp.setAttribute("value", "true");
					cheats.appendChild(pp);
				}
			}
		}
		if (root.getElementsByTagName(getString(R.string.root_misc)).getLength() == 0) {
			Element misc = data.createElement(getString(R.string.root_misc));
			root.appendChild(misc);
		}
		Element misc = (Element) data.getElementsByTagName(getString(R.string.root_misc)).item(0);
		if (misc.getElementsByTagName(getString(R.string.root_misc_lastTease)).getLength() == 0) {
			Element lastTease = data.createElement(getString(R.string.root_misc_lastTease));
			lastTease.setTextContent("");
			misc.appendChild(lastTease);
		}
	}

	private void setPage(String pageID) {
		try {
			String output = "[(" + pageID + "}";
			Element page = doc.getElementById(pageID);
			audioPlayer.stop();
			NodeList children = page.getChildNodes();
			timerTextView.setVisibility(TextView.GONE);
			buttonsLayout.removeAllViews();
			deleteTimers();
			String[] sets = page.getAttribute("set").split(",");
			String[] unsets = page.getAttribute("unset").split(",");
			timerTarget = "";
			skipTimerButton.setEnabled(false);
			pauseTimerButton.setEnabled(false);
			pauseTimerButton.setText(R.string.pause_timer);
			pageIdViewButton.setText(pageID);
			Log.d(TAG, currentPageId);
			if (!currentPageId.equals("") && !fromPrevPageButton) {
				prevPages.add(new Page(currentPageId, set));
				prevPageButton.setEnabled(true);
			}
			fromPrevPageButton = false;
			for (String setString : sets) {
				if (!set.contains(setString)) set.add(setString);
			}
			for (String unsetString : unsets) {
				if (set.contains(unsetString)) set.remove(unsetString);
			}
			currentPageId = pageID;
			saveButton.setTag("auto");
			saveButton.callOnClick();
			if (autoSetPageWhenSeen) set.add(pageID);
			allChildren:
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				NamedNodeMap attrs = child.getAttributes();
				if (attrs != null) {
					Node ifSet = attrs.getNamedItem("if-set");
					Node ifNotSet = attrs.getNamedItem("if-not-set");
					if (ifSet != null) for (String ifSetPart : ifSet.getNodeValue().split("\\|"))
						if (!set.contains(ifSetPart)) continue allChildren;
					if (ifNotSet != null)
						for (String ifNotSetPart : ifNotSet.getNodeValue().split("\\|"))
							if (set.contains(ifNotSetPart)) continue allChildren;
				}
				if (child.getNodeName().equals("Image")) {
					if (imageView.getVisibility() == ImageView.GONE) {
						imageView.setVisibility(ImageView.VISIBLE);
						videoView.setVisibility(VideoView.GONE);
					}
					String image = processImage(attrs.getNamedItem("id").getNodeValue());
					setImage(imageView, mediaDir + image);
					output += String.format("(Image: %s)", image);
				}
				if (child.getNodeName().equals("Video")) {
					if (videoView.getVisibility() == ImageView.GONE) {
						videoView.setVisibility(ImageView.VISIBLE);
						imageView.setVisibility(VideoView.GONE);
					}
					String video = attrs.getNamedItem("id").getNodeValue();
					videoView.setVideoPath(mediaDir + video);
					Node start_at = attrs.getNamedItem("start-at");
					if (start_at != null) videoView.seekTo(timeToMs(start_at.getNodeValue()));
					videoView.start();
					output += String.format("(Video: %s)", video);
				}
				if (child.getNodeName().equals("Text")) {
					String text = child.getTextContent();
					editText.loadData(surroundInBody(text), "text/html", "UTF-8");
					editTextScrollView.setScrollY(0);
				}
				if (child.getNodeName().equals("Button")) {
					final Node childSets = attrs.getNamedItem("set");
					final Node childUnsets = attrs.getNamedItem("unset");
					Button button = new Button(this);
					button.setText(child.getTextContent());
					button.setTextSize(12);
					button.setAllCaps(false);
					final String target = processTarget(attrs.getNamedItem("target").getNodeValue());
					button.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							if (childSets != null)
								for (String childSet : childSets.getNodeValue().split("\\|"))
									set.add(childSet);
							if (childUnsets != null)
								for (String childUnset : childUnsets.getNodeValue().split("\\|"))
									set.remove(childUnset);
							setPage(target);
						}
					});
					buttonsLayout.addView(button);
					output += String.format("(Button: %s, Target: %s)", child.getTextContent(), target);
				}
				if (child.getNodeName().equals("Delay")) {
					skipTimerButton.setEnabled(true);
					pauseTimerButton.setEnabled(true);
					Node childSets = attrs.getNamedItem("set");
					Node childUnsets = attrs.getNamedItem("unset");
					Node startsWithNode = attrs.getNamedItem("start-with");
					delay = processDelay(attrs.getNamedItem("seconds").getNodeValue()) * 1000;
					if (startsWithNode != null)
						delayDeception = Integer.parseInt(startsWithNode.getNodeValue()) * 1000 - delay;
					else delayDeception = 0;
					delayStartTime = System.currentTimeMillis();
					final String target = processTarget(attrs.getNamedItem("target").getNodeValue());
					timerTarget = target;
					Node styleNode = attrs.getNamedItem("style");
					if (styleNode != null) delayStyle = styleNode.getNodeValue().trim();
					else delayStyle = "normal";
					timerTextView.setVisibility(TextView.VISIBLE);
					if (childSets != null)
						set.addAll(Arrays.asList(childSets.getNodeValue().split("\\|")));
//						for (String childSet : childSets.getNodeValue().split("\\|"))
//							set.add(childSet);
					if (childUnsets != null)
						set.removeAll(Arrays.asList(childUnsets.getNodeValue().split("\\|")));
//						for (String childUnset : childUnsets.getNodeValue().split("\\|"))
//							set.remove(childUnset);
					Timer wait = new Timer();
					wait.schedule(new TimerTask() {
						public void run() {
							runOnUiThread(new Runnable() {
								public void run() {
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
					output += String.format(Locale.getDefault(), "(Delay: %ds, Target: %s)", delay / 1000, target);
				}
				if (child.getNodeName().equals("Audio")) {
					String audio = attrs.getNamedItem("id").getNodeValue();
					try {
						audioPlayer = new MediaPlayer();
						audioPlayer.setDataSource(mediaDir + audio);
						audioPlayer.prepare();
						audioPlayer.start();
					} catch (IOException e) {
						Log.e(TAG, "setPage: ", e);
					}
					output += String.format("(Audio: %s)", audio);
				}
				if (child.getNodeName().equals("Metronome")) {
					int bpm = processDelay(attrs.getNamedItem("bpm").getNodeValue());
					Timer metronome = new Timer();
					audioPlayer = MediaPlayer.create(this, R.raw.tick);
					metronome.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							audioPlayer.start();
						}
					}, 0, (int) (1.0 / ((bpm / 60.0) / 1000.0)));
					timers.put("metronome", metronome);
					output += String.format(Locale.getDefault(), "(Metronome: %dbpm)", bpm);
				}
			}
			Log.d(TAG, output + "]");
			if (buttonsLayout.getChildCount() == 0)
				editTextScrollView.setLayoutParams(noButtonsLayoutParams);
			else editTextScrollView.setLayoutParams(yesButtonsLayoutParams);
		} catch (NullPointerException e) {
			Log.e(TAG, "setPage: ", e);
		}
	}

	private int timeToMs(String time) {
		int[] parts = new int[3];
		String[] split = time.split(":");
		for (int i = 0; i < split.length; i++)
			parts[i] = Integer.parseInt(split[i]);
		return parts[0] * 60 * 60 * 1000 + parts[1] * 60 * 1000 + parts[2] * 1000;
	}

	private String processTarget(String target) {
		Matcher matcher = multiplePagesPattern.matcher(target);
		if (matcher.find()) {
			long start = Long.parseLong(matcher.group(2));
			long end = Long.parseLong(matcher.group(3));
			ArrayList<String> allowed = new ArrayList<>();
			for (long j = start; j < end; j++) {
				String num = String.valueOf(j);
				String processedTarget = "";
				if (!set.contains((matcher.group(1).equals("page") ? num : matcher.group(1) + num))) {
					if (matcher.group(1).equals("page")) processedTarget = num;
					else processedTarget = matcher.group(1) + num;
				}
				if (processedTarget.equals("")) {
					NodeList pages = doc.getElementsByTagName("Page");
					for (int i = 0; i < pages.getLength(); i++) {
						Node page = pages.item(i);
						NamedNodeMap attrs = page.getAttributes();
						String id = attrs.getNamedItem("id").getNodeValue();
						if (id.equals(matcher.group(1) + num)) {
							Node ifNotSetNode = attrs.getNamedItem("if-not-set");
							if (ifNotSetNode != null) {
								if (set.contains(ifNotSetNode.getNodeValue())) processedTarget = id;
							}
						}
					}
				}
				if (!processedTarget.equals("")) allowed.add(processedTarget);
			}
			return allowed.get(rand.nextInt(allowed.size()));
		}
		return target;
	}

	private void deleteTimers() {
		for (Timer timer : timers.values()) {
			timer.cancel();
			timer.purge();
		}
		timers.clear();
	}

	public void onClick(View v) {
		if (v.equals(newDocButton)) {
			if (doc != null && homeButtons.getVisibility() != EditText.GONE) { // HOME
				homeButtons.setVisibility(EditText.GONE);
				mediaDir = TEASES_DIR + doc.getElementsByTagName("MediaDirectory").item(0).getTextContent() + "/";
				autoSetPageWhenSeen = doc.getElementsByTagName("AutoSetPageWhenSeen").item(0).getTextContent().equals("true");
				changeNewDocButton("invisible");
				set.clear();
				prevPages.clear();
				prevPageButton.setEnabled(false);
				currentPageId = "";
				Node node;
				if ((node = data.getElementById(md5(teaseButton.getText().toString()))) != null) {
					Element element = (Element) node;
					String sets = element.getElementsByTagName("Sets").item(0).getTextContent();
					set.addAll(Arrays.asList(sets.substring(1, sets.length() - 1).split(",")));
					prevPages = Page.stringToPages(element.getElementsByTagName("PrevPages").item(0).getTextContent());
					if (prevPages.size() > 0) prevPageButton.setEnabled(true);
					setPage(element.getElementsByTagName("Page").item(0).getTextContent());
				} else setPage("start");
			} else if (doc != null) { // IN TEASE
				doc = null;
				homeButtons.setVisibility(EditText.VISIBLE);
				changeNewDocButton("start|stop");
				buttonsLayout.removeAllViews();
				videoView.setVisibility(VideoView.GONE);
				cheats.setVisibility(View.GONE);
				imageView.setVisibility(ImageView.VISIBLE);
				editTextScrollView.setLayoutParams(yesButtonsLayoutParams);
				mediaDir = "";
				if (boolFromXmlElement(getString(R.string.root_settings_homePagePicture)))
					setImage(imageView, R.drawable.welcome);
				editText.loadData(surroundInBody(""), "text/html", "UTF-8");
				audioPlayer.stop();
				if (boolFromXmlElement(getString(R.string.root_settings_endearOnStartup))) {
					audioPlayer = MediaPlayer.create(this, R.raw.hey);
					audioPlayer.start();
				}
				deleteTimers();
				timerTextView.setVisibility(TextView.GONE);
			} else {
				doc = openDocument(TEASES_DIR + teaseButton.getText().toString());
				if (doc != null) newDocButton.callOnClick();
				else Toast.makeText(this, "No tease selected", Toast.LENGTH_SHORT).show();
			}
		} else if (v.equals(skipTimerButton)) {
			setPage(timerTarget);
		} else if (v.equals(imageView) || v.equals(videoView)) {
			if (newDocButton.getVisibility() == Button.INVISIBLE) {
				newDocButton.setVisibility(Button.VISIBLE);
				if (boolFromXmlElement(getString(R.string.root_settings_cheats)))
					cheats.setVisibility(LinearLayout.VISIBLE);
			} else if (doc != null && homeButtons.getVisibility() == EditText.GONE) {
				changeNewDocButton("invisible");
				cheats.setVisibility(View.GONE);
			}
		} else if (v.equals(settingsButton)) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		} else if (v.equals(teaseButton)) {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_xml_chooser);
			final LinearLayout textViewsLayout = (LinearLayout) dialog.findViewById(R.id.textViewsLayout);
			final ScrollView scrollView = (ScrollView) dialog.findViewById(R.id.scrollView);
			File[] filesList = new File(TEASES_DIR).listFiles();
			boolean current = false;
			int scrollY = 0;
			if (filesList == null || filesList.length == 0) dialog.setTitle("No teases found");
			else {
				Arrays.sort(filesList);
				dialog.setTitle("Choose tease");
				for (File file : filesList) {
					if (file.getName().endsWith(".xml")) {
						final Button fileButton = new Button(this);
						fileButton.setText(file.getName());
						fileButton.setTextSize(14);
						fileButton.setAllCaps(false);
						fileButton.setEllipsize(TextUtils.TruncateAt.MIDDLE);
						fileButton.setLongClickable(true);
						fileButton.setOnClickListener(new View.OnClickListener() {
							public void onClick(View v) {
								teaseButton.setText(fileButton.getText());
								dialog.dismiss();
							}
						});
						fileButton.setOnLongClickListener(new View.OnLongClickListener() {
							public boolean onLongClick(View v) {
								DialogInterface.OnClickListener handler = new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										if (which == -1) {
											Document doc1 = openDocument(TEASES_DIR + fileButton.getText().toString());
											deleteFile(new File(TEASES_DIR + doc1.getElementsByTagName("MediaDirectory").item(0).getTextContent()));
											deleteFile(new File(TEASES_DIR + fileButton.getText().toString()));
											Node node;
											if ((node = data.getElementById(md5(teaseButton.getText().toString()))) != null)
												data.getElementsByTagName("Saves").item(0).removeChild(node);
											if (teaseButton.getText().equals(fileButton.getText()))
												teaseButton.setText(R.string.none);
											textViewsLayout.removeView(fileButton);
										} else dialog.dismiss();
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
						if (teaseButton.getText().toString().equals(file.getName())) current = true;
						if (!current) scrollY++;
						LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
						layoutParams.bottomMargin = 5;
						textViewsLayout.addView(fileButton, layoutParams);
					}
				}
				if (!current) scrollY = 0;
				final int scroll = scrollY * 173;
				scrollView.post(new Runnable() {
					public void run() {
						scrollView.scrollTo(0, scroll);
					}
				});
			}
			dialog.show();
		} else if (v.equals(pauseTimerButton)) {
			if (pauseTimerButton.getText().toString().equals(getString(R.string.pause_timer))) {
				pauseTimerButton.setText(R.string.resume_timer);
				if (timers.get("wait") != null && timers.get("update") != null) {
					timers.get("wait").cancel();
					timers.get("wait").purge();
					timers.get("update").cancel();
					timers.get("update").purge();
					timers.remove("wait");
					timers.remove("update");
					delay = (int) (delayStartTime + delay - System.currentTimeMillis());
				}
			} else if (pauseTimerButton.getText().toString().equals(getString(R.string.resume_timer))) {
				pauseTimerButton.setText(R.string.pause_timer);
				Timer wait = new Timer();
				wait.schedule(new TimerTask() {
					public void run() {
						runOnUiThread(new Runnable() {
							public void run() {
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
		} else if (v.equals(pageIdViewButton)) {
			NodeList pages = doc.getElementsByTagName("Page");
			pauseTimerButton.callOnClick();
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_xml_chooser);
			LinearLayout textViewsLayout = (LinearLayout) dialog.findViewById(R.id.textViewsLayout);
			final ScrollView scrollView = (ScrollView) dialog.findViewById(R.id.scrollView);
			boolean current = false;
			int scrollY = 0;
			for (int i = 0; i < pages.getLength(); i++) {
				Element page = (Element) pages.item(i);
				final String pageId = page.getAttribute("id");
				Button button = new Button(this);
				button.setText(pageId);
				button.setTextSize(14);
				button.setAllCaps(false);
				button.setEllipsize(TextUtils.TruncateAt.MIDDLE);
				button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						dialog.dismiss();
						imageView.callOnClick();
						setPage(pageId);
					}
				});
				if (pageId.equals(currentPageId)) current = true;
				if (!current) scrollY++;
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				layoutParams.bottomMargin = 5;
				textViewsLayout.addView(button, layoutParams);
			}
			if (!current) scrollY = 0;
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					pauseTimerButton.callOnClick();
				}
			});
			dialog.show();
			final int scroll = scrollY * 173;
			scrollView.post(new Runnable() {
				public void run() {
					scrollView.scrollTo(0, scroll);
				}
			});
		} else if (v.equals(saveButton)) {
			Element saves = (Element) data.getElementsByTagName("Saves").item(0);
			Node node;
			if ((node = data.getElementById(md5(teaseButton.getText().toString()))) != null)
				saves.removeChild(node);
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
			if (saveButton.getTag().equals("manual"))
				Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
			saveButton.setTag("manual");
		} else if (v.equals(removeSaveButton)) {
			Element saves = (Element) data.getElementsByTagName("Saves").item(0);
			Node node;
			if ((node = data.getElementById(md5(teaseButton.getText().toString()))) != null)
				saves.removeChild(node);
			Toast.makeText(this, "Removed Save", Toast.LENGTH_SHORT).show();
		} else if (v.equals(prevPageButton)) {
			Page prevPage = prevPages.remove(prevPages.size() - 1);
			if (prevPages.size() == 0) prevPageButton.setEnabled(false);
			set = prevPage.getSets();
			fromPrevPageButton = true;
			setPage(prevPage.getId());
		} else if (v.equals(downloadButton)) {
			Intent intent = new Intent(this, DownloadActivity.class);
			startActivity(intent);
		}
	}

	private String md5(String string) {
		try {
			return new BigInteger(1, MessageDigest.getInstance("MD5").digest(string.getBytes())).toString();
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	private void deleteFile(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) for (File child : fileOrDirectory.listFiles())
			deleteFile(child);
		fileOrDirectory.delete();
	}

	private String toTime(long time) {
		long total_seconds = time / 1000 + 1;
		long min = total_seconds / 60;
		long seconds = total_seconds - min * 60;
		return String.format(Locale.getDefault(), "%d:%d", min, seconds);
	}

	private Document openDocument(String path) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "openDocument: fileNotFound");
		} catch (ParserConfigurationException | SAXException | IOException e) {
			Log.e(TAG, "openDocument: ", e);
		}
		return null;
	}

	private String processImage(String image) {
		int index = image.indexOf("*");
		if (index != -1) {
			image = image.substring(0, index);
			index = image.lastIndexOf("/");
			String name = image;
			if (index != -1) {
				name = image.substring(index + 1);
				image = image.substring(0, index);
			}
			File imageFile = new File(mediaDir + image);
			File[] files;
			if (imageFile.isDirectory()) {
				files = imageFile.listFiles();
			} else files = new File(mediaDir).listFiles();
			ArrayList<String> fileNamesList = new ArrayList<>();
			for (File file : files) {
				if (file.getName().startsWith(name)) {
					fileNamesList.add((imageFile.isDirectory() ? image + "/" : "") + file.getName());
				}
			}
			int num = rand.nextInt(fileNamesList.size());
			return fileNamesList.get(num);
		}
		return image;
	}

	public void onBackPressed() {
		if (doc != null && homeButtons.getVisibility() == View.GONE) {
			newDocButton.callOnClick();
		} else {
			finish();
		}
	}

	private int processDelay(String delay) {
		Matcher matcher = Pattern.compile("\\((\\d+)\\.\\.(\\d+)\\)").matcher(delay);
		if (matcher.find()) {
			int start = Integer.parseInt(matcher.group(1));
			int end = Integer.parseInt(matcher.group(2));
			return rand.nextInt(end - start) + start;
		}
		return Integer.parseInt(delay);
	}

	private void makeFullscreen() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}

	private String surroundInBody(String text) {
		return String.format("<body bgcolor=\"black\" text=\"white\">%s</body>", text);
	}

	private static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private String data = "Test";

		BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		protected Bitmap doInBackground(String... params) {
			data = params[0];
			return decodeSampledBitmapFromFile(data, 100, 100);
		}

		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) { // imageViewReference != null
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}

		int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if (height > reqHeight || width > reqWidth) {
				int shortHeight = 0;
				int shortWidth = 0;
				if (height / reqHeight > width / reqWidth) {
					shortHeight = reqHeight;
					shortWidth = shortWidth * reqHeight / shortHeight;
				} else {
					shortWidth = reqWidth;
					shortHeight = shortHeight * reqWidth / shortWidth;
				}

				while ((shortHeight / inSampleSize) > reqHeight && (shortWidth / inSampleSize) > reqWidth) {
					inSampleSize *= 2;
				}
			}
			return inSampleSize;
		}

		Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeFile(path, options);
		}
	}

	private class MyWebViewClient extends WebViewClient {
		public void onPageFinished(WebView view, String url) {
			view.setInitialScale((int) (100 * getResources().getDisplayMetrics().density)); //view.getScale()
		}
	}

	private class MyTask extends TimerTask {
		public void run() {
			runOnUiThread(new Runnable() {
				public void run() {
					long timeLeft = delayStartTime + delay - System.currentTimeMillis();
					if (timeLeft < 0) {
						updateTimer.cancel();
						updateTimer.purge();
					}
					if (delayStyle.equals("secret")) {
						if (timerTextView.getCurrentTextColor() == Color.WHITE)
							timerTextView.setTextColor(Color.RED);
						else timerTextView.setTextColor(Color.WHITE);
					} else if (delayStyle.equals("normal")) {
						timerTextView.setText(toTime(timeLeft + delayDeception));
					}
				}
			});
		}
	}

}
