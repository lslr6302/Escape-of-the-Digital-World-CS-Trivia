import java.awt.Rectangle;

/*
 * Cindy Gao & Carol Meng
 * Jan. 10, 2024
 * ICS4U ISP game - Obstacle class.
 */

class Obstacle extends Rectangle {

	private int xv = 2;  //x velocity
	private int winw, winh;  //window width, window height
	private static int obstacleWidth, obstacleHeight;  //obstacle size

	//Constructor
	Obstacle(int WINH, int WINW) {
		//Call back to the Rectangle class
		super(WINW + (int)(Math.random() * WINW), (int)(Math.random() * (WINH - obstacleHeight)), obstacleWidth, obstacleHeight);  // x, y, width, height
		winw = WINW;
		winh = WINH;
	}

	/**
	 * This method moves the obstacle and resets its position
	 */
	void move() {
		x -= xv; //Move the obstacle from right to left
		if (x + width <= 0) x = winw + (int)(Math.random() * winw); //Reset obstacle position if it reaches the left side of the screen
	}

	/**
	 * Detect when the player collides with an obstacle
	 * @param p		Player object
	 * @param o		Obstacle object
	 * @return		true if intersects, false if not intersecting
	 */
	public boolean checkCollision(Player p, Obstacle o) {
		if (o.intersects(p)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the current width of the obstacle
	 * @return	int width of obstacle
	 */
	public int getObsWidth() {
		return obstacleWidth;
	}

	/**
	 * Set the width of the obstacle
	 * @param width		int new obstable width
	 */
	public void setWidth(int width) {
		obstacleWidth = width;
		super.width = width; //Update the Rectangle width when setting the obstacle width
	}

	/**
	 * Get the current height of the obstacle
	 * @return 	int height of obstacle
	 */
	public int getObsHeight() {
		return obstacleHeight;
	}

	/**
	 * Set the height of the obstacle
	 * @param height	int new obstacle height
	 */
	public void setHeight(int height) {
		obstacleHeight = height;
		super.height = height; // Update the Rectangle height when setting the obstacle height
	}
	
	/**
	 * Get the obstacle's horizontal speed
	 * @return		int horizontal speed of the obstacle
	 */
	public int getXV() {
		return this.xv;
	}

	/**
	 * Set the obstacle's horizontal speed
	 * @param xv	int new speed to be set
	 */
	public void setXV(int xv) {
		this.xv = xv;
	}
}
