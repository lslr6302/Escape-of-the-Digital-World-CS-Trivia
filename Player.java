import java.awt.*;

/* Cindy Gao
 * January 10, 2024
 * This is the player class (where user interacts with the game). 
 */

public class Player extends Rectangle{
	Image img;
	private int lives;

	Player (int x, int y){
		this.x = x;
		this.y = y;
		width = 32;
		height = 45;
	}

	/**
	 * Get the number of lives the player has left
	 * @return		int number of lives
	 */
	public int getLives(){
		return lives;
	}

	/**
	 * Set the number of lives the player has left
	 * @param lives		int number of lives
	 */
	public void setLives(int lives) {
		this.lives = lives;
	}
}
