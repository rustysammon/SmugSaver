import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.Timer;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;  
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.Integer;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/

/*
 * SmugSaver - a simple Java app that lets you download all the high
 * resolution photos in a public Smugmug album.  Good for sharing pictures
 * with your friends.
 * Author: Rusty Sammon (rustysammon@yahoo.com)
 * Created: 12/25/2006
 * Modified: 8/27/2008
 * Designed using JRE 1.5.0_10, but might work on other ones as well 
 */
public class SmugSaver extends javax.swing.JFrame {
	private JLabel usernameLabel;
	private JTextField usernameField;
	private JButton loginButton;
	private JButton pickDestinationButton;
	private JProgressBar progressBar;
	private JList pickAlbumList;
	private JScrollPane pickAlbumScroller;
	private DefaultComboBoxModel pickAlbumListModel;
	private JLabel pickAlbumLabel;
	private JLabel destinationLabel;
	private JTextField destinationField;
	private JButton downloadButton;
	private JTextArea feedbackTextArea;

	private static final String apiKey = "7dxQY67xVAd4UY7sSVE5x8uAocZAEgA3";
	private static final String urlBase = "http://api.smugmug.com/hack/rest/1.1.1/";
	private static final String defaultsFilename = "defaults.txt";
	private static final String usernameLeader = "username = ";
	private static final String directoryLeader = "directory = ";
	private String sessionID;
	private ArrayList<AlbumInfo> albumInfoList = new ArrayList<AlbumInfo>();
	
	//XML parsing via DOM (document object model)
	private DocumentBuilderFactory docBuilderFactory;
	private DocumentBuilder docBuilder;
	
	//example request via REST
	//http://api.smugmug.com/hack/rest/1.1.1/?method=smugmug.images.get&SessionID=8fsda fsljkaer&AlbumID=27
	
	DownloadThread downloader;  //runs the downloading process, which could take a while
	Timer downloadMonitor;
	
	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		SmugSaver smugsaver = new SmugSaver();
		smugsaver.setVisible(true);
	}
	
	public SmugSaver() {
		super();
		initGUI();
		readDefaultsFile();
		
		//Initialize document parsing stuff
		try {
			docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			System.out.println("Couldn't create document builder for parsing XML");
			pce.printStackTrace();
		}
	}
	
	private void initGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			getContentPane().setLayout(null);
			this.setName("SmugSaver");
			this.setTitle("SmugSaver");
			this.setResizable(false);

			//username/account for logging onto the Smugmug server
			//no password necessary - this app only allows downloading of a person's public albums
			{
				usernameLabel = new JLabel();
				getContentPane().add(usernameLabel);
				usernameLabel.setText("username:");
				usernameLabel.setLayout(null);
				usernameLabel.setBounds(7, 7, 70, 28);
				usernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			}
			{
				usernameField = new JTextField();
				getContentPane().add(usernameField);
				usernameField.setText("rustysam");
				usernameField.setBounds(84, 7, 112, 28);
			}
	
			//Login button for logging on to Smugmug server
			{
				loginButton = new JButton();
				getContentPane().add(loginButton);
				loginButton.setLayout(null);
				loginButton.setText("Login Anonymously");
				loginButton.setBounds(203, 7, 175, 28);
				loginButton.addActionListener(new loginAction());
			}

			//list of albums for the specified username
			//  - list is populated when user logs
			//  - user can then select album(s) to download
			{			
				pickAlbumListModel = new DefaultComboBoxModel();
				pickAlbumListModel.addElement("fake album #1");
				pickAlbumListModel.addElement("fake album #2");
				pickAlbumList = new JList();
				pickAlbumList.setModel(pickAlbumListModel);
				//getContentPane().add(albumList);
				
				//enable scrolling of the albumlist
				pickAlbumScroller = new JScrollPane(pickAlbumList);
				pickAlbumScroller.setWheelScrollingEnabled(true);		
				pickAlbumScroller.setBounds(7, 70, 371, 200);
				pickAlbumScroller.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				getContentPane().add(pickAlbumScroller, java.awt.BorderLayout.CENTER);
			}
			{
				pickAlbumLabel = new JLabel();
				getContentPane().add(pickAlbumLabel);
				pickAlbumLabel.setText("Select album(s) to download:");
				pickAlbumLabel.setLayout(null);
				pickAlbumLabel.setBounds(7, 42, 210, 28);
			}
			
			//Destination for saving file
			{
				destinationLabel = new JLabel();
				getContentPane().add(destinationLabel);
				destinationLabel.setText("Destination directory:");
				destinationLabel.setLayout(null);
				destinationLabel.setBounds(7, 273, 133, 28);
			}
			{
				destinationField = new JTextField();
				getContentPane().add(destinationField);
				destinationField.setText("C:\\Documents and Settings\\RSammon\\My Documents\\My Pictures");
				destinationField.setBounds(7, 301, 336, 28);
			}
			{
				pickDestinationButton = new JButton();
				getContentPane().add(pickDestinationButton);
				pickDestinationButton.setText("...");
				pickDestinationButton.setBounds(350, 301, 28, 28);
				pickDestinationButton.addActionListener(new pickDestinationAction());				
			}
			
			//Button to start the download process
			{
				downloadButton = new JButton();
				getContentPane().add(downloadButton);
				downloadButton.setLayout(null);
				downloadButton.setText("Start Download");
				downloadButton.setBounds(7, 336, 371, 28);
				downloadButton.setEnabled(false);
				downloadButton.addActionListener(new downloadAction());
			}
			
			//Feedback regarding what to program is up to (eg, download process)
			{
				feedbackTextArea = new JTextArea();
				getContentPane().add(feedbackTextArea);
				feedbackTextArea.setText("First, select the username of the SmugMug account,\n" +
										 "then click the login button.");
				feedbackTextArea.setBounds(7, 371, 371, 70);
				feedbackTextArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				feedbackTextArea.setOpaque(false);
			}
			{
				progressBar = new JProgressBar();
				progressBar.setMaximum(10);
				progressBar.setValue(0);
				getContentPane().add(progressBar);
				progressBar.setBounds(7, 448, 371, 28);
				progressBar.setStringPainted(true);
			}
			pack();
			this.setSize(393, 514);
			this.addWindowListener(new windowClosingAction());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readDefaultsFile()
	{
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(defaultsFilename));
	        String line;
	        while ((line = reader.readLine()) != null) {
	        	line.trim();
	        	if (line.startsWith(usernameLeader)) {
	        		usernameField.setText(line.substring(usernameLeader.length()));
	        	} else if (line.startsWith(directoryLeader)) {
	        		destinationField.setText(line.substring(directoryLeader.length()));
	        	}
	        }
	        reader.close();
	    } catch (IOException e) {
	    	feedbackTextArea.setText("Could not read " + defaultsFilename);	 
	    	return;
	    }
	}
	
	/*
	 * Login to smugmug server, get sessionID
	 */
	private void loginToSmugmug() {
		try {			
			feedbackTextArea.setText("Attempting to login to smugmug...\n");
			feedbackTextArea.repaint();  //doesn't seem to work
			
			//http://api.smugmug.com/hack/rest/1.1.1/?method=smugmug.login.anonymously&SessionID=8fsda fsljkaer&AlbumID=27
			URL loginUrl = new URL(urlBase + 
					"?method=smugmug.login.anonymously" +
					"&APIKey=" + apiKey);				
			System.out.println("Attempting login with:");
			System.out.println("  " + loginUrl);
			InputStream stream = loginUrl.openStream();
			
			/*
			//printing out the XML to the console - good for debugging
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader buffer = new BufferedReader(streamReader);
			String inputLine;
			while ((inputLine = buffer.readLine()) != null)
				System.out.println(inputLine);
			buffer.close();	
			*/
			
			//parsing the input stream to a DOM (document object model)
			Document document;
			document = docBuilder.parse(stream);
			NodeList nodeList = document.getElementsByTagName("SessionID");
			sessionID = nodeList.item(0).getFirstChild().getTextContent();
			//System.out.println("Got SessionID = " + sessionID);
		} catch (IOException io) {
			feedbackTextArea.setText("Error - Could not login to Smugmug - no response?");
			io.printStackTrace();
			return;
		} catch (SAXException sax) {
			feedbackTextArea.setText("Error - Could not parse login response from Smugmug");
			sax.printStackTrace();	
			return;
		}
		feedbackTextArea.append("Login successful\n");
	}
	
	/*
	 * Using sessionID, get albums for username
	 */
	private void getAlbumList() {
		try {
			feedbackTextArea.append("Attempting to get list of public albums for that user...\n");
			
			String username = usernameField.getText().trim();
			URL getAlbumsUrl = new URL(urlBase +
					"?method=smugmug.albums.get" +
					"&SessionID=" + sessionID +
					"&NickName=" + username +
					"&Heavy=1");
			//System.out.println("Attempting to get albums with:");
			//System.out.println("  " + getAlbumsUrl);
			InputStream stream = getAlbumsUrl.openStream();
			
			/*
			//printing out the XML to the console - good for debugging
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader buffer = new BufferedReader(streamReader);
			String inputLine;
			while ((inputLine = buffer.readLine()) != null)
				System.out.println(inputLine);
			buffer.close();	
			*/
			
			//parsing the input stream to a DOM (document object model)				
			Document document = docBuilder.parse(stream);
			NodeList albumNodes = document.getElementsByTagName("Album");
			pickAlbumListModel.removeAllElements();
			for (int i=0; i<albumNodes.getLength(); i++) {
				AlbumInfo albumInfo = new AlbumInfo(albumNodes.item(i));
				albumInfoList.add(albumInfo);
			}
			
			//sort the albums according to their titles (comparator defined in AlbumInfo class)
			Collections.sort(albumInfoList);
			
			//display the sorted list to the user
			for (int i=0; i<albumInfoList.size(); i++) {
				pickAlbumListModel.addElement(albumInfoList.get(i).getTitle());	
			}
			
		} catch (IOException io) {
			feedbackTextArea.append("Error - Could not get album list from Smugmug - no response?\n");
			io.printStackTrace();
			return;
		} catch (SAXException sax) {
			feedbackTextArea.append("Error - Could not parse album list response from Smugmug\n");
			sax.printStackTrace();	
			return;
		}	
		feedbackTextArea.append("Got album list from Smugmug");
	}	
	
	/*
	 * Attempts to login to the Smugmug server
	 * currently, only the anonymous login (no password) is enabled
	 */
	private class loginAction implements ActionListener {
		public void actionPerformed( ActionEvent actionEvent ) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			loginToSmugmug();   //Login to smugmug server, get sessionID
			getAlbumList();     //Using sessionID, get albums for username
			downloadButton.setEnabled(true);			
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private class pickDestinationAction implements ActionListener {
		public void actionPerformed( ActionEvent actionEvent ) {
			JFileChooser fileChooser = new JFileChooser(destinationField.getText());
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = JFileChooser.CANCEL_OPTION;
			try {
				returnVal = fileChooser.showDialog(SmugSaver.this, "Select Directory");
			} catch (HeadlessException he) {}
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				destinationField.setText(file.getPath());
			}
		}
	}	
	
	/*
	 * Attempts to download specified albums from the smugmug server
	 */
	private class downloadAction implements ActionListener {
		public void actionPerformed( ActionEvent actionEvent ) {
			
			//get the directory specified by the user
			File baseDirectory = new File(destinationField.getText());
			if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
				feedbackTextArea.setText("Can't download. Invalid directory specified.\nDirectory does not exist?\n");
				return;
			}
			
			//get the user's selectiong and convert them to strings
			Object selectedAlbumObjects[] = pickAlbumList.getSelectedValues();
			if (selectedAlbumObjects.length == 0) {
				feedbackTextArea.setText("No albums are selected!\n");
				return;
			}
			String selectedAlbums[] = new String[selectedAlbumObjects.length];
			for (int i=0; i<selectedAlbumObjects.length; i++) {
				selectedAlbums[i] = selectedAlbumObjects[i].toString();
			}
			
			//verify that we have the album info in our list
			for (String albumName : selectedAlbums) {
				boolean foundAlbum = false;
				for (AlbumInfo albumInfo : albumInfoList) {
					if (albumName.toString().equals(albumInfo.getTitle())) {
						foundAlbum = true;
						break;
					}
				}
				if (!foundAlbum) {
					feedbackTextArea.setText("Invalid album selected - no album info available.\n" + 
											 "Album Name: " + albumName);
					return;
				}
			}		

			//At this point, we've got the info we need and are ready to start downloading
			//set cursor to hourglass
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			downloadButton.setText("Download in Progress");
			
			//set up the progress bar
			int totalImages = 0;
			for (String albumName : selectedAlbums) {
				for (AlbumInfo albumInfo : albumInfoList) {
					if (albumName.toString().equals(albumInfo.getTitle())) {
						totalImages += new Integer(albumInfo.getImageCount());
						break;
					}
				}
			}
			progressBar.setMaximum(totalImages);
			progressBar.setValue(0);

			downloader = new DownloadThread(selectedAlbums,
	   		  		  					    baseDirectory,
	   		  		  					    albumInfoList,
	   		  		  					    sessionID,
	   		  		  					    urlBase);
			downloader.start();			
			downloadMonitor = new Timer(1000, new downloadTimerAction());
			downloadMonitor.start();
		}
	}
	
	private class downloadTimerAction implements ActionListener {
		public void actionPerformed( ActionEvent actionEvent ) {
			//System.out.println("timer occurred");
			progressBar.setValue(downloader.getImageCount());
			feedbackTextArea.setText(downloader.getFeedbackText());
			if (downloader.getState() == Thread.State.TERMINATED) {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				downloadButton.setText("Start Download");
				downloadMonitor.stop();
			}
		}
	}
	
	private class windowClosingAction extends WindowAdapter {
		public void windowClosing(WindowEvent ev) {
			//log out of Smugmug
			try {
				URL logoutURL = new URL(urlBase +
						"?method=smugmug.logout" +
						"&SessionID=" + sessionID);			
				logoutURL.openConnection();
			} catch (IOException ioe) {
				System.out.println("Couldn't close connection to Smugmug");
			}
			
			//save default configuration
			try {
		        BufferedWriter out = new BufferedWriter(new FileWriter(defaultsFilename));
		        out.write(usernameLeader + usernameField.getText() + "\n");
		        out.write(directoryLeader + destinationField.getText() + "\n");
		        out.close();
		    } catch (IOException e) {
		    	System.out.println("Couldn't write " + defaultsFilename);
		    }
			
			System.exit(0);
		}
	}
}