import java.awt.*;
public class Portal extends Rectangle{
	//constructor 
	Portal(int x, int y) {
		this.x = x;
		this.y = y;
		this.width = 600;
		this.height = 600;
	}//end constructor
	
	/**
	 * check collision between player and portal 
	 * @param pl	Player object
	 * @param p		Portal object
	 * @return		true if collision is detected, false otherwise
	 */
	public boolean checkCollision(Player pl, Portal p) {
		if (pl.x >= 550 && pl.x <= 650) {
			if (pl.y >= 250 && pl.y <= 350) {
				return true;
			}
		}
		return false;
	}//end checkCollision(Player, Portal)
	
}
