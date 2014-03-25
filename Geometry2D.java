public interface Geometry2D {
	/** Type flag for Geometry2D points */
	public static final int POINT = 0;

	/** Type flag for Geometry2D line segments */
	public static final int SEGMENT = 1;

	/** Type flag for Geometry2D rectangles */
	public static final int RECTANGLE = 2;

	/** Type flag for Geometry2D circles */
	public static final int CIRCLE = 3;

	/**
	 * Returns the type of of an object that implements the Geometry2D
	 * interface.
	 */
	public int getType();
}
