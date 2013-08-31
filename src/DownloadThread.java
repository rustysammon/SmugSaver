import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/*
 * DownloadThread - Performs the downloading of images for the SmugSaver
 * application.
 * Author: Rusty Sammon (rustysammon@yahoo.com)
 * Date: 12/25/2006
 * Designed using JRE 1.5.0_10, but might work on other ones as well 
 */
public class DownloadThread extends Thread {
	
	/*
	 * inputs to the download process - 
	 * these values are provided by the SmugSaver thread
	 */
	private String[] selectedAlbums;  //list of albums to download
	private File baseDirectory;     //where to save downloaded albums
	private ArrayList<AlbumInfo> albumInfoList;  //info about albums
	private String sessionID;
	private String urlBase;
		
	/*
	 * outputs/feedback from the download process - 
	 * these values are sent back to the SmugSaver thread
	 */
	private String feedbackText;
	private int totalImageCount = 0;	  //total number of images downloaded so far, used for progressBar
	
	public String getFeedbackText() {
		return feedbackText;
	}

	public int getImageCount() {
		return totalImageCount;
	}
	
	//XML parsing via DOM (document object model)
	private DocumentBuilderFactory docBuilderFactory;
	private DocumentBuilder docBuilder;	
	
	/*
	 * Constructor requires setting all the inputs for the download
	 * process.  There must be a better way to do this...
	 */
	public DownloadThread (String[] selectedAlbums,
						   File baseDirectory,
						   ArrayList<AlbumInfo> albumInfoList,
						   String sessionID,
						   String urlBase) {
		super();
		
		//store all the inputs that we'll need later in the 
		//downloading process
		this.selectedAlbums = selectedAlbums;
		this.baseDirectory = baseDirectory;
		this.albumInfoList = albumInfoList;
		this.sessionID = sessionID;
		this.urlBase = urlBase;		
		
		//Initialize document parsing stuff
		try {
			docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			System.out.println("Couldn't create document builder for parsing XML");
			pce.printStackTrace();
		}
	}
	
	/*
	 * For the specified album, download the list of images from SmugMug
	 */
	private ArrayList<String> getImageURLs (String albumTitle) {
		
		//Find id number of the album to download
		String albumID = "";
		for (AlbumInfo albumInfo : albumInfoList) {
			if (albumTitle.equals(albumInfo.getTitle())) {
				albumID = albumInfo.getId();
				break;
			}
		}
		if (albumID.length() == 0) {
			System.out.println("Invalid album title specified: " + albumTitle);
			assert (false);
		}
		
		//Contact Smugmug to get list of images in this album
		try {
			URL getImagesUrl = new URL(urlBase +
					"?method=smugmug.images.get" +
					"&SessionID=" + sessionID +
					"&AlbumID=" + albumID +
			"&Heavy=1");
			//System.out.println("Attempting to get images with:");
			//System.out.println("  " + getImagesUrl);
			InputStream stream = getImagesUrl.openStream();

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
			NodeList imageNodes = document.getElementsByTagName("OriginalURL");
			ArrayList<String> imageURLs = new ArrayList<String>();
			for (int i=0; i<imageNodes.getLength(); i++) {
				String originalURL = imageNodes.item(i).getTextContent();
				imageURLs.add(originalURL);
				//System.out.println(originalURL);
			}
			return imageURLs;	
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

    public void run() {
    	//System.out.println("DownloadThread started");

    	//do the download
    	int albumCount = 0;
    	totalImageCount = 0;  //total number of images downloaded so far
    	for (String albumName : selectedAlbums) {
    		albumCount++;
    		//System.out.println("selected album: " + albumName.toString());

    		//Create the directory for the album, if necessary
    		File directory = new File(baseDirectory.toString()+ File.separator +
    				albumName.toString());
    		if (!directory.exists()) {
    			//create new directory for album
    			if (!directory.mkdir()) {
    				feedbackText = "Could not create directory for album " + albumName;
    				return;
    			}
    		}

    		//get the URLS of the original size images
    		ArrayList<String> imageURLs;
    		imageURLs = getImageURLs(albumName.toString());

    		//download the images
    		int imageCount = 0;
    		for (String imageURL : imageURLs) {
    			//determine the name of the image
    			imageCount++;
    			String imageName = "Image" + String.format("%04d", totalImageCount);
    			int lastSlashIndex = imageURL.lastIndexOf('/');
    			if (lastSlashIndex >= 0 && lastSlashIndex < imageURL.length() - 1) {
    				imageName = imageURL.substring(lastSlashIndex + 1);
    			}

    			//provide feedback to the user regarding progess in download
    			feedbackText = "Album #" + albumCount + " out of " + 
    			selectedAlbums.length + " -- " + albumName.toString() + "\n" +
    			"Image #" + imageCount + " out of " + imageURLs.size() + " in album " +
    			" -- " + imageName;	

    			//do the download
    			String localFilename = directory + File.separator + imageName;
    			System.out.println("localFilename: " + localFilename);
    			FileDownload.download(imageURL, localFilename);	
    			totalImageCount++;
    			
    			//pause to allow other threads to do stuff
    			yield();
    		}	
    	}
    }
}
