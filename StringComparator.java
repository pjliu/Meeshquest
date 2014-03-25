//package cmsc420.heptatrie;

import java.util.Comparator;

public class StringComparator implements Comparator<String>
    {
    	public StringComparator() {}
    	
        public int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
}