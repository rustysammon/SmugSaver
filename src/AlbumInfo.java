import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/*
 * AlbumInfo - a simple class used to store information about a 
 * SmugMug album after parsing it out of a DOM Node.  Intended for
 * use with the SmugSaver program.
 * Author: Rusty Sammon (rustysammon@yahoo.com)
 * Date: 12/25/2006
 * Designed using JRE 1.5.0_10, but might work on other ones as well 
 */
public class AlbumInfo implements Comparable<AlbumInfo> {
	private String id;
	private String title;
	private String imageCount;
	private String lastUpdated;
	
	public AlbumInfo(Node node) {
		readNode(node);		
	}
	
	public void readNode(Node node) {
		id = node.getAttributes().getNamedItem("id").getNodeValue();
		
		NodeList albumInfo = node.getChildNodes();
		for (int j=0; j<albumInfo.getLength(); j++) {
			if (albumInfo.item(j).getNodeName() == "Title") {
				title = albumInfo.item(j).getTextContent();
				//System.out.println("Title: " + title);
			}
			if (albumInfo.item(j).getNodeName() == "ImageCount") {
				imageCount = albumInfo.item(j).getTextContent();
				//System.out.println("  ImageCount: " + imageCount);
			}
			if (albumInfo.item(j).getNodeName() == "LastUpdated") {
				lastUpdated = albumInfo.item(j).getTextContent();
				//System.out.println("  LastUpdated: " + lastUpdated);
			}
		}
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getImageCount() {
		return imageCount;
	}

	public void setImageCount(String imageCount) {
		this.imageCount = imageCount;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	@Override
	public int compareTo(AlbumInfo arg0) {
		return( arg0.title.compareTo( title ));
	}

}
