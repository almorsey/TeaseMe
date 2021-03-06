package almorsey.teaseme;

import java.util.ArrayList;
import java.util.Arrays;

class Page{
	private String id;
	private ArrayList<String> sets;

	Page(String id, ArrayList<String> sets){
		this.id = id;
		this.sets = sets;
	}

	private static Page stringToPage(String pageString){
		int index = pageString.indexOf('=');
		String id = pageString.substring(0, index);
		pageString = pageString.substring(index + 2, pageString.length() - 1);
		ArrayList<String> sets = new ArrayList<>();
		sets.addAll(Arrays.asList(pageString.split(",")));
		return new Page(id, sets);
	}

	static ArrayList<Page> stringToPages(String pagesString){
		ArrayList<Page> pages = new ArrayList<>();
		if(pagesString.length() > 2){
			pagesString = pagesString.substring(1, pagesString.length() - 1);
			String[] pageStrings = pagesString.split("]");
			for(String pageString : pageStrings){
				if(pageString.startsWith(", ")) pageString = pageString.substring(2);
				pages.add(stringToPage(pageString + "]"));
			}
		}
		return pages;
	}

	ArrayList<String> getSets(){
		return sets;
	}

	public String getId(){
		return id;
	}

	public String toString(){
		return id + "=" + Arrays.toString(sets.toArray());
	}

}
