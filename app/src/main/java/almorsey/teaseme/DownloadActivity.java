package almorsey.teaseme;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class DownloadActivity extends AppCompatActivity {
	private final String TAG = "almorsey";
	private ASyncGet docsGet;
	private boolean cancelled = false;
	private Document doc;
	private String teaseTitle = "";
	private ArrayList<ResourceDownloader> rds = new ArrayList<>();
	private EditText urlEditText;
	private CheckBox saveResourcesCheckBox;
	private Button button;
	private ScrollView outputScrollView;
	private TextView outputTextView;
	private Pattern buttonsPattern = Pattern.compile("target\\d+:(.+?)#,cap\\d+:\"(.+?)\"");

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.download);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		urlEditText = (EditText) findViewById(R.id.urlEditText);
		saveResourcesCheckBox = (CheckBox) findViewById(R.id.saveResourcesCheckBox);
		button = (Button) findViewById(R.id.downloadButton);
		outputTextView = (TextView) findViewById(R.id.outputTextView);
		outputScrollView = (ScrollView) findViewById(R.id.outputScrollView);

		urlEditText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					startDownload(v);
					return true;
				}
				return false;
			}
		});
	}

	private void newline(String string) {
		outputTextView.setText(outputTextView.getText() + "\n" + string);
		outputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
	}

	public void startDownload(View v) {
		if (button.getText().toString().equals(getString(R.string.download))) {
			if (docsGet != null) docsGet.cancel(true);
			String url = urlEditText.getText().toString().trim();
			Pattern pattern = Pattern.compile("^https?://milovana\\.com/webteases/show(flash|tease)\\.php\\?id=(\\d+)$");
			Matcher matcher = pattern.matcher(url);
			if (matcher.find()) {
				button.setText(R.string.cancel);
				cancelled = false;
				outputTextView.setText(R.string.getting_source);
				if (matcher.group(1).equals("flash")) {
					docsGet = new ASyncGet(matcher);
					docsGet.execute(
							new String[]{"https://milovana.com/webteases/getscript.php?id=" + matcher.group(2), ASyncGet.BUFFERED_READER_METHOD},
							new String[]{matcher.group(0), ASyncGet.JSOUP_METHOD});
				} else if (matcher.group(1).equals("tease"))
					docsGet = new ASyncGet(new ArrayList<org.jsoup.nodes.Document>(), matcher.group(0), 1);
			} else {
				outputTextView.setText(R.string.invalid_url);
			}
		} else {
			cancelled = true;
			docsGet.cancel(true);
			for (ResourceDownloader rd : rds) {
				rd.cancel(true);
			}
			button.setText(R.string.download);
		}
	}

	private Document makeBaseXML(String teaseId, String teaseUrl, String authorName, String authorUrl)
			throws ParserConfigurationException {
		newline("Loaded:" + teaseTitle);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element teaseElement = doc.createElement("Tease");
		teaseElement.setAttribute("scriptVersion", "v0.1");
		teaseElement.setAttribute("id", teaseId);
		Element titleElement = doc.createElement("Title");
		titleElement.setTextContent(teaseTitle);
		teaseElement.appendChild(titleElement);
		Element tagsElement = doc.createElement("Tags");
		teaseElement.appendChild(tagsElement);
		Element urlElement = doc.createElement("Url");
		urlElement.setTextContent(teaseUrl);
		teaseElement.appendChild(urlElement);
		Element authorElement = doc.createElement("Author");
		authorElement.setAttribute("id", authorUrl.substring(authorUrl.lastIndexOf("=") + 1));
		Element authorNameElement = doc.createElement("Name");
		authorNameElement.setTextContent(authorName);
		authorElement.appendChild(authorNameElement);
		Element authorUrlElement = doc.createElement("Url");
		authorUrlElement.setTextContent("https://milovana.com/" + authorUrl);
		authorElement.appendChild(authorUrlElement);
		teaseElement.appendChild(authorElement);
		Element mediaDirElement = doc.createElement("MediaDirectory");
		mediaDirElement.setTextContent(parseLocation(teaseTitle));
		teaseElement.appendChild(mediaDirElement);
		Element settingsElement = doc.createElement("Settings");
		Element aspwsElement = doc.createElement("AutoSetPageWhenSeen");
		aspwsElement.setTextContent("false");
		settingsElement.appendChild(aspwsElement);
		teaseElement.appendChild(settingsElement);
		Element varsElement = doc.createElement("Variables");
		teaseElement.appendChild(varsElement);
		Element pagesElement = doc.createElement("Pages");
		teaseElement.appendChild(pagesElement);
		doc.appendChild(teaseElement);
		return doc;
	}

	private void makeTeaseXML(ArrayList<org.jsoup.nodes.Document> docs) {
		org.jsoup.nodes.Document doc1 = docs.get(0);
		String teaseUrl = doc1.location();
		teaseUrl = teaseUrl.substring(0, teaseUrl.indexOf('&'));
		String teaseId = teaseUrl.substring(48);
		teaseTitle = doc1.getElementById("tease_title").text();
		String authorName = "";
		Matcher m = Pattern.compile("^(.+?) by (.+?)$").matcher(teaseTitle);
		if (m.find()) {
			teaseTitle = m.group(1);
			authorName = m.group(2);
		}
		String authorUrl = doc1.getElementsByClass("tease_author").get(0).child(0).attr("href");
		try {
			doc = makeBaseXML(teaseId, teaseUrl, authorName, authorUrl);
			Element pagesE = (Element) doc.getElementsByTagName("Pages").item(0);
			for (int i = 0; i < docs.size(); i++) {
				org.jsoup.nodes.Document doci = docs.get(i);
				Element pageE = doc.createElement("Page");
				Element textE = doc.createElement("Text");
				Element imgE = doc.createElement("Image");
				pageE.setAttribute("id", i == 0 ? "start" : String.valueOf(i + 1));
				textE.setTextContent(doci.getElementsByClass("text").get(0).text());
				pageE.appendChild(textE);
				org.jsoup.nodes.Element e = doci.getElementsByClass("tease_pic").get(0);
				imgE.setAttribute("id", (i + 1) + ".jpg");
				pageE.appendChild(imgE);
				if (saveResourcesCheckBox.isChecked()) {
					ResourceDownloader rd = new ResourceDownloader();
					rds.add(rd);
					rd.execute((i + 1) + ".jpg", e.absUrl("src"), teaseTitle);
				}
				if (i + 1 < docs.size()) {
					Element buttonE = doc.createElement("Button");
					buttonE.setTextContent("Continue");
					buttonE.setAttribute("target", String.valueOf(i + 2));
					pageE.appendChild(buttonE);
				}
				pagesE.appendChild(pageE);
			}
			if (rds.size() == 0)
				done();
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "makeTeaseXML: ", e);
		}
	}

	private ArrayList<Element> evalAction(String style, String action) {
		ArrayList<Element> elements = new ArrayList<>();
		if (style.equals("go")) {
			Element e = doc.createElement("Button");
			e.setTextContent("Continue");
			e.setAttribute("target", evalTarget(action.substring(7)));
			elements.add(e);
		}
		if (style.equals("yn")) {
			Element y = doc.createElement("Button");
			y.setTextContent("Yes");
			int index = action.indexOf(',');
			y.setAttribute("target", evalTarget(action.substring(4, index)));
			elements.add(y);
			Element n = doc.createElement("Button");
			n.setTextContent("No");
			n.setAttribute("target", evalTarget(action.substring(index + 5, action.length())));
			elements.add(n);
		}
		if (style.equals("delay")) {
			Element e = doc.createElement("Delay");
			int i = action.indexOf(',');
			e.setAttribute("seconds", toSeconds(action.substring(5, i)));
			action = action.substring(i + 8);
			i = action.startsWith("range") ? getCorresponding(action, 5) + 1 : action.indexOf('#');
			e.setAttribute("target", evalTarget(action.substring(0, i)));
			i = action.indexOf("style");
			if (i != -1)
				e.setAttribute("style", action.substring(i + 6));
			elements.add(e);
		}
		if (style.equals("buttons")) {
			Matcher m = buttonsPattern.matcher(action);
			while (m.find()) {
				Element e = doc.createElement("Button");
				e.setAttribute("target", m.group(1));
				e.setTextContent(m.group(2));
				System.out.println(m.group(1) + "\t" + m.group(2));
				elements.add(e);
			}
		}
		if (style.equals("vert")) {
			Pattern p = Pattern.compile("e\\d+:(buttons|delay|go|yn)\\((.+?)\\)");
			Matcher m = p.matcher(action);
			while (m.find()) {
				String a = action.substring(m.start(), m.end());
				a = a.substring(3);
				elements.addAll(evalAction(a.substring(0, a.indexOf('(')), a.substring(a.indexOf('(') + 1)));
			}
		}
		return elements;
	}

	private String evalTarget(String target) {
		Pattern p = Pattern.compile("range\\(from:(\\d+),to:(\\d+),:'(.+?)'\\)");
		Matcher m = p.matcher(target);
		if (m.find())
			target = String.format("%s(%s..%s)", m.group(3), m.group(1), m.group(2));
		else if (target.endsWith("#"))
			target = target.substring(0, target.length() - 1);
		return target;
	}

	private String toSeconds(String string) {
		String weight = string.substring(string.length() - 3);
		int num = Integer.parseInt(string.substring(0, string.indexOf(weight.charAt(0))));
		int multiple = 1;
		if (weight.equals("hrs"))
			multiple = 3600;
		else if (weight.equals("min"))
			multiple = 60;
		return String.valueOf(num * multiple);
	}

	private Element evalPage(Document doc, String page) {
		String pageId = page.substring(0, page.indexOf("#"));
		Element pageE = doc.createElement("Page");
		pageE.setAttribute("id", pageId);
		page = page.substring(page.indexOf("(") + 1, page.length() - 1);
		try {
			if (page.startsWith("text")) {
				int posStart = page.indexOf("'") + 1;
				int posEnd = page.indexOf("'", posStart);
				String text = page.substring(posStart, posEnd);
				if (!text.isEmpty()) {
					Element e = doc.createElement("Text");
					org.jsoup.nodes.Document textDoc = Jsoup.parse(text);
					e.setTextContent(textDoc.text());
					pageE.appendChild(e);
				}
				page = page.substring(posEnd + 2);
			}
			if (page.startsWith("media")) {
				int posStart = page.indexOf('"') + 1;
				int posEnd = page.indexOf('"', posStart);
				Element e = doc.createElement("Image");
				e.setAttribute("id", parseLocation(page.substring(posStart, posEnd)));
				pageE.appendChild(e);
				page = page.substring(posEnd + 3);
			}
			if (page.startsWith("action")) {
				int posStart = page.indexOf('(') + 1;
				int posEnd = getCorresponding(page, posStart - 1);
				ArrayList<Element> actions = evalAction(page.substring(7, posStart - 1),
						page.substring(posStart, posEnd));
				for (Element e : actions) {
					pageE.appendChild(e);
				}
				page = page.substring(posEnd + 2);
			}
			if (page.startsWith("hidden:sound")) {
				int posStart = page.indexOf("'") + 1;
				int posEnd = page.indexOf("'", posStart);
				Element e = doc.createElement("Audio");
				e.setAttribute("id", parseLocation(page.substring(posStart, posEnd)));
				pageE.appendChild(e);
				page = page.substring(posEnd + 3);
			}
			if (page.startsWith("instruc")) {
				int posStart = page.indexOf('(') + 1;
				String style = page.substring(page.indexOf(':') + 1, posStart - 1);
				if (!style.isEmpty()) {
					if (style.equals("set") || style.equals("unset")) {
						int posEnd = getCorresponding(page, posStart - 1);
						pageE.setAttribute(style, page.substring(posStart, posEnd).replaceAll("(action\\d+:|#)", ""));
						page = page.substring(posEnd + 2);
					} else if (style.equals("delay")) {
						ArrayList<Element> es = evalAction("delay", page.substring(posStart, page.indexOf(')')));
						for (Element e : es) {
							pageE.appendChild(e);
						}
						page = page.substring(page.indexOf(')') + 2, posStart);
					}
				}
			}
			if (page.startsWith("hidden")) {
				int posStart = page.indexOf('(') + 1;
				String style = page.substring(page.indexOf(':') + 1, posStart - 1);
				if (!style.isEmpty()) {
					int posEnd = getCorresponding(page, posStart - 1);
					pageE.setAttribute(style, page.substring(posStart, posEnd).replaceAll("(action\\d+:|#)", ""));
					page = page.substring(posEnd + 2);
				}
			}
			if (!page.isEmpty())
				newline(page);
		} catch (StringIndexOutOfBoundsException e) {
			return pageE;
		}
		return pageE;
	}

	private int getCorresponding(String string, int start) {
		char correspondingTo = string.charAt(start);
		char negativeCorrespondingTo = 0;
		if (correspondingTo == '(')
			negativeCorrespondingTo = ')';
		if (negativeCorrespondingTo != ')') {
			System.err.println(correspondingTo);
			return start + 1;
		}
		int opening = 0;
		boolean checking = true;
		for (int i = start + 1; i < string.length(); i++) {
			if (string.charAt(i) == '"')
				checking = !checking;
			if (!checking)
				continue;
			if (string.charAt(i) == negativeCorrespondingTo)
				opening--;
			if (string.charAt(i) == correspondingTo)
				opening++;
			if (opening == -1)
				return i;
		}
		return start + 1;
	}

	private void makeFlashXML(Matcher matcher, String scriptDoc, org.jsoup.nodes.Document teaseDoc) {
		if (scriptDoc == null) return;
		if (scriptDoc.isEmpty()) outputTextView.setText(R.string.tease_404);
		else {
			org.jsoup.nodes.Element titleNode = teaseDoc.getElementsByClass("title").get(0);
			String titleAndBy = titleNode.text();
			teaseTitle = titleAndBy.substring(0, titleAndBy.lastIndexOf(" by "));
			String teaseBy = titleAndBy.substring(titleAndBy.lastIndexOf(" by ") + 4);
			String authorUrl = titleNode.children().get(0).attr("href");
			String authorId = authorUrl.substring(authorUrl.indexOf("=") + 1);
			try {
				doc = makeBaseXML(matcher.group(2), matcher.group(0), teaseBy, authorUrl);
				String[] pages = scriptDoc.split("\\n");
				ArrayList<String> files = new ArrayList<>();
				Element pagesElement = (Element) doc.getElementsByTagName("Pages").item(0);
				for (String pageString : pages) {
					Element page = evalPage(doc, pageString.trim());
					NodeList children = page.getChildNodes();
					if (saveResourcesCheckBox.isChecked())
						for (int i = 0; i < children.getLength(); i++) {
							Node child = children.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								Node idNode = child.getAttributes().getNamedItem("id");
								String id = "";
								if (idNode != null)
									id = idNode.getNodeValue();
								if ((child.getNodeName().equals("Image") || child.getNodeName().equals("Audio"))
										&& !id.isEmpty()) {
									if (!files.contains(id)) {
										ResourceDownloader rd = new ResourceDownloader();
										rds.add(rd);
										rd.execute(id, authorId, matcher.group(2), teaseTitle);
										files.add(id);
									}
								}
							}
						}
					pagesElement.appendChild(page);
				}
				if (rds.size() == 0) done();
				doc.normalize();
			} catch (ParserConfigurationException | TransformerFactoryConfigurationError e) {
				Log.e(TAG, "makeFlashXML: ", e);
				newline("Couldn't save file");
				newline(e.getLocalizedMessage());
			}
		}
	}

	private String parseLocation(String x) {
		return x.replaceAll("[\\\\/\\*?<>\"#]", "");
	}

	private void done() {
		if (cancelled)
			return;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(MainActivity.TEASES_DIR + parseLocation(teaseTitle) + ".xml"));
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(fos));
			button.callOnClick();
			newline("Done");
		} catch (TransformerException | TransformerFactoryConfigurationError e) {
			e.printStackTrace();
			outputTextView.setText(R.string.unsavable);
			newline(e.getLocalizedMessage());
		} catch (FileNotFoundException e) {
			Log.e(TAG, "done: ", e);
		} finally {
			try {
				if (fos != null) fos.close();
			} catch (IOException e) {
				Log.e(TAG, "done: ", e);
			}
		}
	}

	private class ASyncGet extends AsyncTask<String[], Void, Object> {

		static final String JSOUP_METHOD = "JSOUP", BUFFERED_READER_METHOD = "BFRD";
		Pattern normalTeasePattern = Pattern.compile("id=\\d+&p=(\\d+)");
		ArrayList<org.jsoup.nodes.Document> docs;
		String url;
		int pageNum;
		Matcher matcher;

		ASyncGet(Matcher matcher) {
			docs = null;
			this.matcher = matcher;
		}

		ASyncGet(ArrayList<org.jsoup.nodes.Document> docs, String url, int pageNum) {
			this.docs = docs;
			this.url = url;
			this.pageNum = pageNum;
			execute(new String[]{url + "&p=" + pageNum, ASyncGet.JSOUP_METHOD});
		}

		private String getUrlSource(String url) {
			BufferedReader in = null;
			try {
				URL link = new URL(url);
				URLConnection urlConn = link.openConnection();
				in = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
				StringBuilder a = new StringBuilder();
				for (String inputLine = in.readLine(); inputLine != null; inputLine = in.readLine()) {
					a.append(inputLine);
					a.append("\n");
				}
				in.close();
				return a.toString().replaceAll("\\r?\\n(?!.+?#page\\(text)", "");
			} catch (UnknownHostException e) {
				return "UnknownHostException";
			} catch (IOException e) {
				Log.e(TAG, "getUrlSource: ", e);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						Log.e(TAG, "getUrlSource: ", e);
					}
			}
			return "";
		}

		protected Object doInBackground(String[]... args) {
			try {
				Object[] objs = new Object[args.length];
				for (int i = 0; i < objs.length; i++) {
					Object obj = null;
					if (args[i][1].equals(JSOUP_METHOD)) {
						obj = Jsoup.connect(args[i][0]).timeout(7000).get();
					} else if (args[i][1].equals(BUFFERED_READER_METHOD)) {
						obj = getUrlSource(args[i][0]);
						if (obj.equals("UnknownHostException"))
							throw new UnknownHostException();
						if (obj.equals(""))
							throw new NullPointerException();
					}
					objs[i] = obj;
				}
				return objs;
			} catch (NullPointerException e) {
				return "Could not get source";
			} catch (UnknownHostException e) {
				Log.e(TAG, "doInBackground: ", e);
				return "Network error";
			} catch (SocketTimeoutException e) {
				return "Network timeout";
			} catch (IOException e) {
				Log.e(TAG, "doInBackground: ", e);
				return e.getMessage();
			}
		}

		protected void onPostExecute(Object val) {
			try {
				if (val instanceof Object[]) {
					Object[] valArr = (Object[]) val;
					for (Object obj : valArr)
						if (obj instanceof org.jsoup.nodes.Document) {
							org.jsoup.nodes.Document doc = (org.jsoup.nodes.Document) obj;
							String location = doc.location();
							Matcher m = normalTeasePattern.matcher(location);
							if (m.find())
								newline("Loaded page " + m.group(1));
						}
				} else newline(val.toString());
			} catch (CancellationException e) {
				Log.d(TAG, "CancellationException");
			}
			if (docs != null) try {
				Object[] value = (Object[]) val;
				org.jsoup.nodes.Document doc = (org.jsoup.nodes.Document) value[0];
				docs.add(doc);
				if (doc.getElementById("continue") != null) {
					docsGet = new ASyncGet(docs, url, pageNum + 1);
				} else makeTeaseXML(docs);
			} catch (ClassCastException e) {
				button.callOnClick();
			} catch (CancellationException e) {
				newline("Cancelled");
			}
			else try {
				Object[] value = (Object[]) val;
				String scriptDoc = (String) value[0];
				org.jsoup.nodes.Document teaseDoc = (org.jsoup.nodes.Document) value[1];
				makeFlashXML(matcher, scriptDoc, teaseDoc);
			} catch (ClassCastException e) {
				button.callOnClick();
			} catch (CancellationException e) {
				newline("Cancelled");
			}
		}
	}

	private class ResourceDownloader extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... args) {
			String url, mediaDir, name;
			name = url = mediaDir = "";
			if (args.length == 4) {
				name = args[0];
				int dirIndex = name.lastIndexOf("/");
				mediaDir = MainActivity.TEASES_DIR + parseLocation(args[3]) + "/";
				if (dirIndex > 0) {
					mediaDir += parseLocation(name.substring(0, dirIndex + 1));
					name = name.substring(dirIndex + 1);
				}
				url = String.format("https://milovana.com/media/get.php?folder=%s/%s&name=%s", args[1], args[2], name);
			} else if (args.length == 3) {
				name = args[0];
				url = args[1];
				mediaDir = MainActivity.TEASES_DIR + parseLocation(args[2]) + "/";
			}

			File file = null;
			String ioError;
			try {
				file = new File(mediaDir + parseLocation(name));
				File mediaFile = new File(mediaDir);
				if (!mediaFile.exists())
					mediaFile.mkdir();
				if (!file.exists())
					file.createNewFile();
				else
					throw new FileNotFoundException();
				downloadFile(url, file.getAbsolutePath());
				return "Downloaded " + name;
			} catch (FileNotFoundException e) {
				return "File '" + name + "' exists";
			} catch (IOException e) {
				Log.d(TAG, "doInBackground: " + file.getPath());
				Log.e(TAG, "doInBackground: ", e);
				ioError = e.getMessage();
			}
			return "Failed to download " + name + (ioError.isEmpty() ? "" : "\n" + ioError);
		}

		private void downloadFile(String source, String destination) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(source);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					input = connection.getInputStream();
					output = new FileOutputStream(destination);

					byte data[] = new byte[4096];
					int count;
					while ((count = input.read(data)) != -1)
						if (isCancelled()) input.close();
						else output.write(data, 0, count);
				}
			} catch (IOException e) {
				Log.e(TAG, "downloadFile: ", e);
			} finally {
				try {
					if (output != null) output.close();
					if (input != null) input.close();
				} catch (IOException ignored) {
				}
				if (connection != null) connection.disconnect();
			}
		}

		protected void onPostExecute(String s) {
			try {
				newline(s);
				rds.remove(this);
				if (rds.size() == 0) done();
			} catch (CancellationException e) {
				Log.e(TAG, "onPostExecute: ", e);
			}
		}

	}

}
