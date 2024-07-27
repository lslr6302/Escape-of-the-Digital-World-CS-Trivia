/*
 * ICS4U 
 * ISP
 * Jan. 10, 2024
 */
import java.awt.*;

public class Chest extends Rectangle {
	//attributes 
	boolean pause; //true if the game is paused
	
	//constructor 
	Chest(int x, int y) {
		this.x = x;
		this.y = y;
		this.width = 30;
		this.height = 30;
	}//end constructor
	
	/**
	 * check collision between player and chest
	 * @param p		Player object 
	 * @param c		Chest object 
	 * @return		true if collision is detected, false otherwise
	 */
	public boolean checkCollision(Player p, Chest c) {
		if (p.intersects(c)) {
			return true;
		} else {
			return false;
		}
	}//end checkCollision(Player, Chest)
	
}//end Chest class
