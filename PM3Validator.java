//package cmsc420.pmquadtree;


//import cmsc420.pmquadtree.PMQuadtree.Black;

public class PM3Validator implements Validator {
	
	public boolean valid(final PMQuadtree.Black node) {
		return (node.getNumPoints() <= 1);
	}
}
