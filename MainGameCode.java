import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/* Carol, Cindy, Diana
 * ISP Main Game
 * January, 2024
 * 
 * This is a trivia and adventure game mash-up.
 * Answer Java trivia questions while dodging deadly obstacles.
 * There are 3 levels, each of increasing difficulty. 
 * Player control supports arrow keys and WASD.
 * Have fun and don't die!
 * 
 * Music credits:
 * Music by primalhousemusic from Pixabay 
 * Music by Sergii Pavkin from Pixabay 
 * Music by Kris Klavenes from Pixabay
 * Music by SPmusic from Pixabay 
 * Music by Magnetic_Trailer from Pixabay 
 * 
 * IMPORTANT NOTE: Do not DRAG the intro screen or MINIMIZE the JFrame while the intro music is playing!!
 * (If you do, the sound system will malfunction and the clip will overlap itself.)
 * If you need to move the JFrame while on the intro screen, mute the sound first, then move it.
 * The sound works fine when you're done moving around and want to unmute to begin playing with music.
 */

public class MainGameCode implements ActionListener {
	//Animation variables
	//Each sprite is 100x97 pixels
	static final int spriteW = 100;
	static final int spriteH = 97;
	static final int spriteMAXFRAME = 8;

	//Main game setup variables
	JFrame frame;
	DrawingPanel dPanel;
	MyKeyListener key = new MyKeyListener();
	MyMouseListener mouse = new MyMouseListener();
	int WINW = 1200;
	int WINH = 600;
	int mouseX, mouseY;
	Player player;
	String playerName = "";
	ArrayList<Obstacle> obstacles = new ArrayList<Obstacle>(); //Arraylist that stores and controls all obstacles
	int obNum = 20; //Number of obstacles
	int[] levelRequirements = {2, 6, 12}; //Accumulated question answered (checking if switch to another level)
	int currLevel = 0; //Current level (0: level 1, 1: level 2...)
	boolean gameOver = false;
	boolean gameWin = false;
	boolean gameLost = false;
	boolean onIntro = false;
	Portal p = new Portal(WINW + 600,0);
	Color blue = new Color(3, 69, 252, 50); //Transparent blue (RGB)
	Color red = new Color(252, 3, 3, 50); 
	Color green = new Color(3, 252, 3, 50); 
	Color purple = new Color(90, 66, 245); 

	//Chest variables
	Chest chests = new Chest(WINW - 50, 10);
	Random rand = new Random(); //for randomizing chest location
	int previousChest; //tracks the location of previous chest

	//Music setup
	AudioInputStream audioStream;
	Clip clip;
	int count = 0;
	ArrayList<File> music = new ArrayList<File>();
	boolean playIntro = true; //for setting up intro screen music

	//Pop up for trivia
	JFrame triFrame;
	JPanel triPanel, btnPanel, checkPanel;
	JLabel question;
	JRadioButton c1, c2, c3, c4;
	ButtonGroup choices;
	JButton check;
	static ArrayList<String> normalQuestion = new ArrayList<String>();  //Arraylist of all trivia questions
	static HashMap<String, ArrayList> qa = new HashMap<String, ArrayList>();  //Hashmap storing all questions and answers
	int qNumber = 0;  //Question number, starts at 0
	int correctAns = 0;  //Index of the correct answer for each question
	int rightAnswers = 0;  //Number of the correct answer that the player answered
	int lives = 10;

	//Intro screen global variables
	JFrame introFrame;
	IntroDrawingPanel inPanel;
	BufferedImage introbg;
	JFrame nameFrame;
	JPanel namePanel;
	JTextField textfield;
	JButton submit;

	//End and win scene
	JFrame endFrame, winFrame;
	EndDrawingPanel endPanel;
	WinDrawingPanel winPanel;

	//Images setup
	BufferedImage heart, checkMark, questionMark; //Status bar
	BufferedImage volcano, forest, ocean, robot, chestImg, portal, blackbg, glitch, white;
	Image robotImg, lavaBallImg, birdImg, fishImg, explodeImg; 
	BufferedImage lavaBall, bird, fish;
	ArrayList<BufferedImage> bgImg = new ArrayList<BufferedImage>();
	ArrayList<BufferedImage> obImg = new ArrayList<BufferedImage>();
	BufferedImage volumeOn, volumeOff; 

	//Timer variables
	Timer timer;
	int T_SPEED = 20;
	Timer obTimer;
	int OB_SPEED = 20;

	//Scoreboard global variables 
	File scoreFile = new File("score.txt");
	FileWriter out;
	BufferedWriter writeFile;
	FileReader in;
	BufferedReader readFile;
	ArrayList<String> allInfo = new ArrayList<String>();
	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> scores = new ArrayList<String>();
	String line;
	JFrame scoreboard;
	ScoreDrawingPanel scPanel;

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					new MainGameCode();
				} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
					e.printStackTrace();
				}
			}
		});
	}

	MainGameCode()throws UnsupportedAudioFileException, IOException, LineUnavailableException{
		introSetup();
		setupFrames();

		//Obstacles setup
		for (int i = 0; i < obNum; i++) {
			obstacles.add(new Obstacle(WINH, WINW));
		}

		setupOtherFeatures(); 

		//Timer for obstacles
		obTimer = new Timer(OB_SPEED, new ActionListener() {
			public void actionPerformed(ActionEvent e){
				for (Obstacle ob : obstacles) {
					if(!chests.pause) ob.move();

					//check collision between player and obstacles
					if (ob.checkCollision(player, ob)) {
						ob.x = WINW;
						soundEffect("res/Hit.wav");
						dPanel.runExplosion(); 
						lives--;
						player.setLives(lives);					
					}

					//check collision between player and chests
					if (chests.checkCollision(player, chests)) {
						soundEffect("res/UnlockChest.wav");
						chests.pause = true;
						previousChest = chests.x;//keep track of the location of the previous location	
						chests.x = WINW + chests.width;//hide the chest after collision is detected

						//pop up trivia question
						setTriviaPage();
						triFrame.setVisible(true);
					}
				}

				if (currLevel == 3) {
					//check collision between player and portal at the end of the game
					if (p.checkCollision(player, p)) {
						soundEffect("res/warp.wav");
						gameOver = true;
					}
					//hide the chest off screen
					chests.x = WINW + chests.width;
				}

				//End game when player runs out of lives
				if(player.getLives() <= 0) {
					gameOver = true; 
					gameLost = true;
				}

				if(gameOver) {
					obTimer.stop();

					//Show final scene for winners and failed attempters
					if(currLevel == 3 && player.getLives() > 0 && rightAnswers >= 7) {
						//Show winner scene
						try {
							gameWin = true;
							showWinScene();
						} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
							e1.printStackTrace();
						}
					}else {
						//Show failed attempt scene
						try {
							gameLost = true;
							showLoseScene();	
						} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
							e1.printStackTrace();
						}
					}
				}
				dPanel.repaint();
			}

		});
	}//end constructor





	//ALL SETUP METHODS
	//ALL SETUP METHODS
	/**
	 * Set up the intro screen with rules, volume and start buttons, and pop up window to enter player's name
	 */
	public void introSetup() throws UnsupportedAudioFileException, IOException, LineUnavailableException {

		//Set mouse location off screen at the start
		mouseX = -100;
		mouseY = -100;

		//Intro setup
		introFrame = new JFrame("Welcome");
		introFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		introFrame.setResizable(false);
		inPanel = new IntroDrawingPanel();
		inPanel.setPreferredSize(new Dimension(WINW, WINH));
		introFrame.add(inPanel);
		introFrame.pack();
		introFrame.setLocationRelativeTo(null);
		introFrame.setVisible(true);
		introbg = loadImage("res/intro.jpg");
		introFrame.addMouseListener(mouse);
		introFrame.addMouseMotionListener(mouse);
		volumeOn = loadImage("res/volume on.png");
		volumeOff = loadImage("res/volume off.png");
		nameFrame = new JFrame("Enter Name");
		nameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		nameFrame.setResizable(false);
		namePanel = new JPanel();
		namePanel.setPreferredSize(new Dimension(300, 100));
		namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.PAGE_AXIS));
		namePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		JLabel prompt = new JLabel("Enter your name here: ");
		prompt.setAlignmentX(Component.CENTER_ALIGNMENT);
		namePanel.add(prompt);
		textfield = new JTextField(10);
		textfield.setAlignmentX(Component.CENTER_ALIGNMENT);
		namePanel.add(textfield);
		submit = new JButton("Submit");
		submit.setAlignmentX(Component.CENTER_ALIGNMENT);
		submit.setActionCommand("submit");
		submit.addActionListener(this);
		namePanel.add(submit);
		nameFrame.add(namePanel);
		nameFrame.pack();
		nameFrame.setLocationRelativeTo(null);

		onIntro = true;
	}

	/**
	 * Set up main frame and trivia popup frame
	 */
	public void setupFrames(){

		//Frame setup
		frame = new JFrame("Escape of the Digital World: CS Trivia");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false); //No cheating by resizing
		dPanel = new DrawingPanel();
		lives = 10;
		player = new Player(0, WINH - 50);
		player.setLives(lives);

		//Pop up setup
		triFrame = new JFrame("CS Trivia");
		triFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		triFrame.setResizable(false);
		triPanel = new JPanel();
		triPanel.setLayout(new BoxLayout(triPanel, BoxLayout.PAGE_AXIS));
		triPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); 

		btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(0, 2)); 
		checkPanel = new JPanel();
		question = new JLabel();
		question.setAlignmentX(Component.CENTER_ALIGNMENT);

		//Setup unique question font
		question.setFont(new Font("Algerian", Font.BOLD, 20)); 

		//Pop up question and choices set up
		choices = new ButtonGroup();
		check = new JButton("CHECK");
		check.addActionListener(this);
		c1 = new JRadioButton();
		c2 = new JRadioButton();
		c3 = new JRadioButton();
		c4 = new JRadioButton();
		InputMap im = c1.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);  //the following 4 lines of code disable the use of arrow key to select choices
		im.put(KeyStroke.getKeyStroke("RIGHT"), "none");
		im.put(KeyStroke.getKeyStroke("LEFT"), "none");
		im.put(KeyStroke.getKeyStroke("UP"), "none");
		im.put(KeyStroke.getKeyStroke("DOWN"), "none");
		choices.add(c1);
		choices.add(c2);
		choices.add(c3);
		choices.add(c4);
		triPanel.add(question);
		btnPanel.add(c1);
		btnPanel.add(c2);
		btnPanel.add(c3);
		btnPanel.add(c4);
		checkPanel.add(check);
		triFrame.setContentPane(triPanel);
		triFrame.add(btnPanel);
		triFrame.add(checkPanel);
		triFrame.pack();
		triFrame.setLocationRelativeTo(null);
		getFile();  //get data from the trivia.txt

		//End scene
		endFrame = new JFrame("GAME OVER");
		endFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		endFrame.setResizable(false);
		endPanel = new EndDrawingPanel();
		endFrame.setContentPane(endPanel);
		endFrame.addMouseListener(mouse);
		endFrame.addMouseMotionListener(mouse);
		endFrame.pack();
		endFrame.setLocationRelativeTo(null);
		endFrame.setVisible(false);

		//Win scene
		winFrame = new JFrame("PROUD SURVIVOR");
		winFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		winFrame.setResizable(false);
		winPanel = new WinDrawingPanel();
		winFrame.setContentPane(winPanel);
		winFrame.addMouseListener(mouse);
		winFrame.addMouseMotionListener(mouse);
		winFrame.pack();
		winFrame.setLocationRelativeTo(null);
		winFrame.setVisible(false);
	}

	/**
	 * Setup music and images and finish main frame setup
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public void setupOtherFeatures() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		if(playIntro) {
			//Background music backlog if player wants to play with music
			music.add(new File("res/portalMusic.wav")); //clipPortal (for INTRO)
			music.add(new File("res/tension.wav")); //clip
			music.add(new File("res/tension_orchestra.wav")); //clip2
			music.add(new File("res/portalMusic.wav")); //clipPortal
			music.add(new File("res/winBgMusic.wav")); //clipWin
			music.add(new File("res/loseBgMusic.wav")); //clipEnd 
		}

		setupImages();
		player.img = robotImg;

		frame.setContentPane(dPanel);
		frame.addKeyListener(key);
		frame.addMouseListener(mouse);
		frame.addMouseMotionListener(mouse);
		frame.pack();
		frame.setLocationRelativeTo(null);
	}
	//Intro screen's drawing panel class





	//ALL MAIN GAME FUNCTION METHODS
	//ALL MAIN GAME FUNCTION METHODS
	/**
	 * Show the winner scene if player wins the game
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public void showWinScene() throws UnsupportedAudioFileException, IOException, LineUnavailableException{
		frame.setVisible(false);
		winFrame.setVisible(true);

		//Play winner music → close old clip & update audio stream
		clip.stop();
		clip.close();

		audioStream = AudioSystem.getAudioInputStream(music.get(4));
		clip = AudioSystem.getClip();
		clip.open(audioStream);

		clip.loop(Clip.LOOP_CONTINUOUSLY);
		clip.start();

		//Update scoreboard
		writeToFile();
		readScoreInfo();
		showScoreboard();
	}

	/**
	 * Show the game over scene is player loses the game
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public void showLoseScene() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		frame.setVisible(false);
		endFrame.setVisible(true);
		soundEffect("res/gameOver.wav");

		//Play winner music → close old clip & update audio stream
		clip.stop();
		clip.close();

		audioStream = AudioSystem.getAudioInputStream(music.get(5));
		clip = AudioSystem.getClip();
		clip.open(audioStream);

		clip.loop(Clip.LOOP_CONTINUOUSLY);
		clip.start();

		//Update scoreboard
		writeToFile();
		readScoreInfo();
		showScoreboard();
	}

	/**
	 * Change music at different phases of the game
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public void levelMusic() throws UnsupportedAudioFileException, IOException, LineUnavailableException {

		if(playIntro) { 
			if(currLevel == 2) { //Play level 2 music
				clip.stop();
				clip.close();

				audioStream = AudioSystem.getAudioInputStream(music.get(2)); //Update audio stream with new music file from backlog
				clip = AudioSystem.getClip();
				clip.open(audioStream);

				clip.loop(Clip.LOOP_CONTINUOUSLY);
				clip.start();	
			}else if(currLevel == 3) { //Play level 3 music
				clip.stop();
				clip.close();

				audioStream = AudioSystem.getAudioInputStream(music.get(3));
				clip = AudioSystem.getClip();
				clip.open(audioStream);

				clip.loop(Clip.LOOP_CONTINUOUSLY);
				clip.start();	
			}
		}
	}

	/**
	 * Play sound effects (from clip object) in the game
	 * @param filename	String name of the desired wav file
	 */
	public void soundEffect(String filename){
		Clip sound = null; 
		try {
			sound = sound(filename);
			sound.start();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Loading one-time sound effects from files into clip objects
	 * @param filename		String name of the file (with "\" if loading from a resource folder)
	 * @return				Clip object to play the sound
	 */
	Clip sound(String filename) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		File file = new File(filename);
		AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
		final Clip clip = AudioSystem.getClip();
		clip.open(audioStream);		
		return clip;
	}

	/**
	 * Load all images used in the game
	 */
	public void setupImages() {
		//Load all buffered images
		volcano = loadImage("res/Volcano.jpeg");
		forest = loadImage("res/Deforestation.jpg");
		ocean = loadImage("res/Ocean.png");
		robot = loadImage("res/robot.png");
		chestImg = loadImage("res/Treasure Chest.png");
		lavaBall = loadImage("res/lavaBall.png");
		fish = loadImage("res/fish.png");
		bird = loadImage("res/bird.png");
		portal = loadImage("res/portal.png");
		blackbg = loadImage("res/black background.jpg");
		glitch = loadImage("res/glitch.jpg");
		white = loadImage("res/white.jpg");
		heart = loadImage("res/heart.png");
		checkMark = loadImage("res/checkMark.png");
		questionMark = loadImage("res/questionMark.png");
		explodeImg = loadImage("res/explosion-spriteSheet.png");

		//Resize buffered images and load them into images 
		robotImg = robot.getScaledInstance(35, 50, Image.SCALE_DEFAULT);
		lavaBallImg = lavaBall.getScaledInstance(35, 35, Image.SCALE_DEFAULT);
		birdImg = bird.getScaledInstance(55, 55, Image.SCALE_DEFAULT);
		fishImg = fish.getScaledInstance(75, 75, Image.SCALE_DEFAULT);

		//Add BufferedImages into arraylists for background & object image change
		bgImg.add(volcano);
		bgImg.add(forest);
		bgImg.add(ocean);
		bgImg.add(blackbg);
		obImg.add(lavaBall);
		obImg.add(bird);
		obImg.add(fish);
	}

	/**
	 * Loading buffered images from file name
	 * @param filename		String name of the file
	 * @return				BufferedImage img loaded from the file
	 */
	static BufferedImage loadImage(String filename) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(filename));
		} catch (IOException e) {
			System.out.println(e.toString());
			JOptionPane.showMessageDialog(null, "An image failed to load: " + filename , "ERROR", JOptionPane.ERROR_MESSAGE);
		}
		return img;
	}

	/**
	 * This method reads the data from trivia.txt, and stores them in arraylist and hashmap for later usage
	 */
	public static void getFile() {
		//setup for read file
		File tri = new File("trivia.txt");
		FileReader in;
		BufferedReader readFile;
		String lineOfTxt;
		//Temporary variables needed
		ArrayList<String> tmp = new ArrayList<String>();
		String question = "";
		int cnt = 0;

		try {
			//more setup
			in = new FileReader(tri);
			readFile = new BufferedReader(in);

			//read files
			while(!((lineOfTxt = readFile.readLine()) == null)) {
				//trivia.txt contains 1 line of question, followed by 4 lines of choices
				//the following code will store the question in an arraylist
				//and store the choices in an temporary arraylist
				//then put string of question: arraylist of answers into the hashmap
				switch (cnt%5) {
				case 0:
					question = lineOfTxt;
					normalQuestion.add(lineOfTxt);
					break;
				case 1:
				case 2:
				case 3:
					tmp.add(lineOfTxt);
					break;
				case 4:
					tmp.add(lineOfTxt);
					qa.put(question, tmp);
					tmp = new ArrayList<String>();
					question = "";
					break;
				}
				cnt++;
			}

			//close the readers
			readFile.close();
			in.close();
		} catch(IOException e) {
			System.out.println("Something went wrong" + e.getMessage());
		}
	}//end of method getFile()

	/**
	 * This method sets up the trivia pop up page
	 */
	public void setTriviaPage() {
		//set up the question and answers (JLabel and radio buttons)
		String tmpQ = normalQuestion.get(qNumber);
		String tmp1 = (String) qa.get(tmpQ).get(0);
		String tmp2 = (String) qa.get(tmpQ).get(1);
		String tmp3 = (String) qa.get(tmpQ).get(2);
		String tmp4 = (String) qa.get(tmpQ).get(3);
		question.setText(tmpQ);
		c1.setText(tmp1);
		c2.setText(tmp2);
		c3.setText(tmp3);
		c4.setText(tmp4);
		//get the correct answer for each question, reset the answer string
		if (tmp1.charAt(0) == '|') {
			correctAns = 0; 
			c1.setText(tmp1.substring(1));
		}
		else if (tmp2.charAt(0) == '|') {
			correctAns = 1;
			c2.setText(tmp2.substring(1));
		}
		else if (tmp3.charAt(0) == '|') {
			correctAns = 2;
			c3.setText(tmp3.substring(1));
		}
		else if (tmp4.charAt(0) == '|') {
			correctAns = 3;
			c4.setText(tmp4.substring(1));
		}
		//other side set ups
		choices.clearSelection();
		qNumber++;
		//read for display
		triFrame.pack();
		triFrame.setLocationRelativeTo(null);
	}//end of method setTriviaPage()

	/**
	 * This method check if the player's selection is correct or not
	 */
	public void checkCorrect() {
		//if the answer matches the user selection
		if ((correctAns == 0 && c1.isSelected()) || (correctAns == 1 && c2.isSelected())|| (correctAns == 2 && c3.isSelected())|| (correctAns == 3 && c4.isSelected())) {
			rightAnswers++;
			soundEffect("res/win-sound.wav");
			JOptionPane.showMessageDialog(triPanel, "Correct Answer! ^_^");
			triFrame.dispose();
			chests.pause = false;
			doNewLevel();  //check if need to switch a level
			resetChest();

		} else {
			//if the selection is incorrect
			if (c1.isSelected() || c2.isSelected() || c3.isSelected() || c4.isSelected()) {
				soundEffect("res/wrong-sound.wav");
				JOptionPane.showMessageDialog(triPanel, "Incorrect Answer. T_T");
				triFrame.dispose();
				chests.pause = false; 
				doNewLevel();
				resetChest();
			} else {  //if the player selected nothing
				JOptionPane.showMessageDialog(triPanel, "Nothing has been answered. -_-");
			}
		}
	}//end of the method checkCorrect()

	/**
	 * Resets the chest location after each time the player collides with the chest and finishes the trivia question
	 */
	private void resetChest() {
		//if the previous chest was located in the left half of the screen, generate new location in the right half
		//if the previous chest was located in the right half of the screen, generate new location in the left half
		//this avoids two consecutive chests appearing close to each other or overlapping each other
		if (previousChest < WINW/2) {
			chests.x = rand.nextInt(WINW/2) + (WINW/2 - 30);
			chests.y = rand.nextInt(WINH - 80) + 50;//avoid the chest overlapping the status bar
		} else {
			chests.x = rand.nextInt(WINW/2);
			chests.y = rand.nextInt(WINH - 80) + 50;//avoid the chest overlapping the status bar
		}

	}//end resetChest()

	/**
	 * This method check if need to switch to a new level
	 */
	public void doNewLevel() {
		if (qNumber == levelRequirements[currLevel]) {  //if the number of question answered meets the requirements
			if(currLevel < 2) currLevel++;
			else {
				currLevel = 3;
				finishGame();
			}
			for (Obstacle ob: obstacles) {
				ob.setXV(ob.getXV()+1);  //increase obstacle speed
			}
			try {
				levelMusic(); //change music based on the level
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}//end of method doNewLevel()

	/**
	 * Displays the end of the game with the portal 
	 */
	public void finishGame() {
		//Set everything off the screen
		for(Obstacle ob: obstacles) {
			ob.x = WINW + ob.width;
		}
		chests.pause = true;

		//Show portal in the middle of the screen
		p.x = WINW/2 - 300;

		//Set player into bottom left corner
		player.x = 0;
		player.y = WINH - 50;

	}//end finishGame()

	/**
	 * Writes player's name and score into a score file
	 * Player's name is written on one line, and the score is written on the next line
	 */
	public void writeToFile() {
		//write score info to file 
		try {
			out = new FileWriter(scoreFile, true);
			writeFile = new BufferedWriter(out);
			writeFile.write(playerName);
			writeFile.newLine();
			writeFile.write(String.valueOf(rightAnswers));
			writeFile.newLine();
			writeFile.close();
			out.close();
		} catch (IOException exc) {
			System.out.println("Writing to file was not successful");
		}
	}//end writeToFile()

	/**
	 * Reads all of the players' names and scores from the score file
	 * Saves data into ArrayLists 
	 * Sorts the scores from highest to lowest 
	 */
	public void readScoreInfo() {
		//clearing the ArrayLists so that when player chooses to replay, the ArrayLists won't have duplicating data stored
		allInfo.clear();
		names.clear();
		scores.clear();
		try {
			in = new FileReader(scoreFile);	
			readFile = new BufferedReader(in);
			while ((line = readFile.readLine()) != null) {
				allInfo.add(line);
			}
			readFile.close();
			in.close();
		} catch (FileNotFoundException exc) {
			System.out.println("Problem finding file");
		} catch (IOException exc) {
			System.out.println("Problem reading file");
		}

		//transfer data from allInfo to names and scores
		for (int i = 0; i < allInfo.size(); i += 2) {
			names.add(allInfo.get(i));
		}
		for (int i = 1; i < allInfo.size(); i += 2) {
			scores.add(allInfo.get(i));
		}

		//sort scores from highest to lowest using a selection sort
		for (int index = 0; index < scores.size(); index++) {
			for (int subIndex = index; subIndex < scores.size(); subIndex++) {
				int num1 = Integer.parseInt(scores.get(index));
				int num2 = Integer.parseInt(scores.get(subIndex));
				String temp;
				if (num2 > num1) {
					scores.set(index, String.valueOf(num2));
					scores.set(subIndex, String.valueOf(num1));
					temp = names.get(index);
					names.set(index, names.get(subIndex));
					names.set(subIndex, temp);
				}
			}
		}

	}//end readScoreInfo()

	/**
	 * Sets up scoreboard for displaying at the end of the game
	 */
	public void showScoreboard() {
		scoreboard = new JFrame("Scoreboard");
		scoreboard.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		scoreboard.setResizable(false);
		scPanel = new ScoreDrawingPanel();
		scPanel.setPreferredSize(new Dimension(400, 100));
		scoreboard.add(scPanel);
		scoreboard.pack();
	}//end showScoreboard()

	/**
	 * Increase obstacle size as levels progress
	 */
	public void levelUpObstacles() {
		if(currLevel == 0) {
			for(Obstacle ob: obstacles) {
				ob.setHeight(35);
				ob.setWidth(35);
			}
		}
		if(currLevel == 1) {
			for(Obstacle ob: obstacles) {
				ob.setHeight(55);
				ob.setWidth(55);
			}
		}
		if(currLevel == 2) {
			for(Obstacle ob: obstacles) {
				ob.setHeight(75);
				ob.setWidth(75);
			}
		}
	}

	/**
	 * Reset all necessary variables to replay the game
	 */
	public void reset() { 
		clip.close();
		clip.stop();

		//Reset player and level stats
		player.x = 0;
		player.y = WINH - 50;
		playerName = "";
		currLevel = 0;

		//Reset chests and trivia popup
		chests.x = WINW - 50;
		chests.y = 10;
		qNumber = 0;
		correctAns = 0;
		rightAnswers = 0;

		//Rest all obstacle speeds
		for (Obstacle ob: obstacles) {
			ob.setXV(2);
			ob.x = WINW + (int)(Math.random()*WINW);
			ob.y = (int)(Math.random()*WINH);
		}

		//Reset portal position to off-screen
		p.x = WINW + 600;
		p.y = 0;

		chests.pause = false; //TURN OFF THE BOOLEAN SO OBSTACLES PROPERLY APPEAR
		gameWin = false;
		gameOver = false;

		playIntro = true;
		count = 0;
		inPanel.repaint();
	}





	//ALL PRIVATE CLASSES (DRAWING PANELS AND EVENT LISTENERS)
	//ALL PRIVATE CLASSES (DRAWING & EVENT LISTENING)
	//Main game's drawing panel class
	private class DrawingPanel extends JPanel{
		private boolean explosionRunning = false;
		private int exploNum = 0; //Row of explosion
		private int frameNum = 0; //Frame of sprite in the explosion row

		DrawingPanel(){
			this.setPreferredSize(new Dimension (WINW, WINH));
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//Draw background
			g2.drawImage(bgImg.get(currLevel), 0, 0, WINW, WINH, null);

			//Draw portal
			g2.drawImage(portal, p.x, p.y, p.width, p.height, null);

			//Draw player (and player explosion)
			if (explosionRunning) {
				g.drawImage(explodeImg,							
						player.x, player.y, player.x + player.width, player.y + player.height,  //destination
						frameNum * spriteW, exploNum * spriteH, (frameNum+1) * spriteW, (exploNum+1) * spriteH,						
						null);
			} else {
				g2.drawImage(player.img, player.x, player.y, null);
			}

			//Draw obstacles
			for (Obstacle ob: obstacles) {
				if(currLevel < 3) g2.drawImage(obImg.get(currLevel), ob.x, ob.y, ob.width, ob.height, null);
			}

			//Draw chest
			g2.drawImage(chestImg, chests.x, chests.y, chests.width, chests.height, null);

			//Status bar
			g2.setColor(new Color(220, 220, 220, 80));
			g2.fillRect(0, 0, 340, 50);
			g2.drawImage(heart, 10, 10, 30, 30, null);
			g2.drawImage(checkMark, 90, 10, 30, 30, null);
			g2.drawImage(questionMark, 170, 10, 30, 30, null);

			g2.setColor(Color.white);
			g2.setFont(new Font("Courier New", Font.PLAIN, 20));
			g2.drawString("X"+Integer.toString(lives), 46, 20);
			g2.drawString("X"+Integer.toString(rightAnswers), 126, 20);
			g2.drawString("X"+Integer.toString(12-qNumber), 206, 20);

			if (currLevel < 3) {				
				g2.setFont(new Font("Algerian", Font.PLAIN, 40));
				g2.drawString("Lv."+Integer.toString(currLevel+1), 255, 40);
			} else {
				g2.setFont(new Font("Algerian", Font.PLAIN, 40));
				g2.drawString("Lv.?", 255, 40);
				g2.drawString("SEEK", 160, 300);
				g2.drawString("LIGHT...", 950, 300);
			}

			//Levels
			levelUpObstacles();
		}

		/**
		 * Display explosion effects when the player collides with an obstacle
		 */
		void runExplosion() {
			class MyTimerListener implements ActionListener {
				public void actionPerformed(ActionEvent e) {
					dPanel.repaint();
					frameNum++;

					//Run through all frames in one row for each explosion 
					if (frameNum == spriteMAXFRAME) { 
						((Timer)e.getSource()).stop();
						explosionRunning = false;			
						frameNum = 0;
						dPanel.repaint();
					}
				}
			}

			Timer myTimer = new Timer(35, new MyTimerListener());  //Specifies how fast animation runs
			explosionRunning = true;
			myTimer.start();	
		}
	}

	//Drawing panel class for end scene
	private class EndDrawingPanel extends JPanel{
		EndDrawingPanel(){
			this.setPreferredSize(new Dimension (WINW - 400, WINH));
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//Draw background
			g2.drawImage(glitch, 0, 0, WINW, WINH, null);

			//Draw GAME OVER
			Font font = new Font("Algerian", Font.PLAIN, 80);
			g2.setPaint(Color.RED);
			g2.setFont(font);
			g2.drawString("GAME OVER", endPanel.getWidth()/2 - 220, endPanel.getHeight()/4);

			//Draw ending message
			font = new Font("Courier New", Font.ITALIC, 20);
			g2.setFont(font);
			g2.setPaint(Color.WHITE);
			g2.drawString("You failed to escape the digital world.", 160, 225);
			g2.drawString("You are now doomed to forever stay here... ", 145, 250);
			g2.drawString("And never be able to contact the living world again.", 95, 275);

			//Draw replay message
			font = new Font("Courier New", Font.BOLD, 20);
			g2.setFont(font);
			g2.setPaint(Color.WHITE);
			g2.drawString("Do you want to attempt life again?", 190, 410);

			//Draw replay & exit message
			font = new Font("Courier New", Font.BOLD, 20);
			g2.setFont(font);
			g2.setPaint(Color.WHITE);
			g2.drawString("REPLAY", 275, 475);
			g2.drawString("NO", 475, 475);

			//Draw border around replay, end, and scoreboard option
			g2.setPaint(Color.GREEN);
			g2.drawRect(265, 450, 90, 40); //Replay button
			g2.setPaint(green);
			g2.fillRect(265, 450, 90, 40); 

			g2.setPaint(Color.RED);
			g2.drawRect(465, 450, 40, 40); //Exit button
			g2.setPaint(red);
			g2.fillRect(465, 450, 40, 40);

			//Draw scoreboard button
			g2.setPaint(purple);
			g2.fillRect(endPanel.getWidth()/2-70, 325, 115, 20); 
			font = new Font("Courier New", Font.BOLD, 15);
			g2.setPaint(Color.WHITE);
			g2.setFont(font);
			g2.drawString("SEE RANKING", endPanel.getWidth()/2-65, 340);
		}
	}

	//Drawing panel class for win scene
	private class WinDrawingPanel extends JPanel{
		WinDrawingPanel(){
			this.setPreferredSize(new Dimension (WINW - 400, WINH));
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//Draw background
			g2.drawImage(white, 0, 0, WINW, WINH, null);

			//Draw CONGRATULATIONS
			Font font = new Font("Algerian", Font.PLAIN, 70);
			g2.setPaint(Color.BLACK);
			g2.setFont(font);
			g2.drawString("CONGRATULATIONS,", endPanel.getWidth()/2 - 315, endPanel.getHeight()/4);
			g2.drawString("SURVIVOR!", endPanel.getWidth()/2 - 170, endPanel.getHeight()/4 + 65);

			//Draw ending message
			font = new Font("Courier New", Font.ITALIC, 20);
			g2.setFont(font);
			g2.setPaint(Color.BLACK);
			g2.drawString("You successfully escaped the digital world.", 145, 270);
			g2.drawString("Go on and explore your life beyond pixelated screens.", 75, 300);
			g2.drawString("You are a free soul now!", 250, 330);

			//Draw replay message
			font = new Font("Courier New", Font.BOLD, 20);
			g2.setFont(font);
			g2.setPaint(Color.BLACK);
			g2.drawString("Want to try your hand at death again?", 175, 420);

			//Draw replay & exit option
			font = new Font("Courier New", Font.BOLD, 20);
			g2.setFont(font);
			g2.setPaint(Color.BLACK);
			g2.drawString("REPLAY", 295, 475);
			g2.drawString("NO", 475, 475);

			//Draw border around replay and end option 
			g2.setPaint(Color.RED);
			g2.drawRect(285, 450, 90, 40); //Replay button
			g2.setPaint(red);
			g2.fillRect(285, 450, 90, 40); 

			g2.setPaint(Color.BLUE);
			g2.drawRect(465, 450, 40, 40); //Exit button
			g2.setPaint(blue);
			g2.fillRect(465, 450, 40, 40); 

			//Draw scoreboard button
			g2.setPaint(purple);
			g2.fillRect(endPanel.getWidth()/2-75, 360, 150, 20); 
			font = new Font("Courier New", Font.BOLD, 15);
			g2.setPaint(Color.WHITE);
			g2.setFont(font);
			g2.drawString("SHOW SCOREBOARD", endPanel.getWidth()/2-70, 375);
		}
	}

	//Scoreboard's drawing panel class
	public class ScoreDrawingPanel extends JPanel {
		ScoreDrawingPanel() {
			this.setPreferredSize(new Dimension(400, 100));
			this.setBackground(Color.BLACK);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//display the top 3 players along with their scores
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font("Courier New",Font.BOLD, 20));
			g2d.drawString("Top Players", 5, 20);
			g2d.drawString("Top Scores", 200, 20);
			g2d.setFont(new Font("Courier New",Font.PLAIN, 20));
			g2d.drawLine(0,25,400,25);
			g2d.drawLine(200, 0, 200, 100);

			//display player who scored top 1
			//if player's name is too long, only display a portion of the name
			if (names.get(0).length() > 16) {
				g2d.drawString(names.get(0).substring(0, 16), 5, 40);
			} else {
				g2d.drawString(names.get(0), 5, 40);
			}
			g2d.drawString(scores.get(0), 200, 42);

			//if there is more than one player stored in the ArrayList, display player who scored top 2
			//if player's name is too long, only display a portion of the name
			if (names.size() >= 2) {
				if (names.get(1).length() > 16) {
					g2d.drawString(names.get(1).substring(0, 16), 5, 60);
				} else {
					g2d.drawString(names.get(1), 5, 60);
				}
				g2d.drawString(scores.get(1), 200, 60);
			}

			//if there are more than two players, display player who scored top 3
			//if player's name is too long, only display a portion of the name
			if (names.size() >= 3) {
				if (names.get(2).length() > 16) {
					g2d.drawString(names.get(2).substring(0, 16), 5, 80);
				} else {
					g2d.drawString(names.get(2), 5, 80);
				}
				g2d.drawString(scores.get(2), 200, 80);
			}
		}
	}//end ScoreDrawingPanel class

	private class IntroDrawingPanel extends JPanel {
		IntroDrawingPanel() {
			this.setPreferredSize(new Dimension(WINW, WINH));
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//background 
			g2d.drawImage(introbg, 0, 0, WINW, WINH, null);

			//title
			g2d.setFont(new Font("Algerian", Font.PLAIN, 50));
			g2d.setColor(Color.WHITE);
			g2d.drawString("Escape of the Digital World", 280, 50);

			//rules 
			g2d.setFont(new Font("Courier New", Font.BOLD, 20));
			g2d.setColor(Color.WHITE);
			g2d.drawString("Rules of the game", 50, 80);
			g2d.drawString("- Collect chests to unlock the key to escape the digital world", 40, 100);
			g2d.drawString("- Each chest contains a question. "
					+ "If you get the question correct, "
					+ "you can collect the chest", 40, 120);
			g2d.drawString("- If you get the question wrong, you fail to collect the chest", 40, 140);
			g2d.drawString("- Use the arrow keys to move the player", 40, 160);
			g2d.drawString("- Avoid obstacles as any collision with obstacles will decrease your lives by one", 40, 180);
			g2d.drawString("- You only have ten lives", 40, 200);
			g2d.drawString("- Your ultimate goal is to get the key to escape the world", 40, 220);
			g2d.drawString("- To win, collect the chests and answer 7 out of 12 questions correctly", 40, 240);
			g2d.drawString("- You lose when you have no more lives or if you answered less than 7 questions correctly", 40, 260);
			g2d.drawString("Good Luck", 40, 300);

			//start button 
			g2d.setColor(Color.YELLOW);
			g2d.fillRect(550, 500, 100, 50);
			g2d.setColor(Color.BLACK);
			g2d.drawString("Start", 570, 530);

			//volume button 
			g2d.setColor(Color.YELLOW);
			g2d.fillRect(550, 350, 100, 100);

			//when music is off
			if (!playIntro) {
				g2d.drawImage(volumeOff, 550, 350, 100, 100, null);
				if(clip.isRunning()) {
					clip.stop();
					clip.close();
				}
			}
			//when music is on
			if (playIntro) { 
				g2d.drawImage(volumeOn, 555, 355, 90, 90, null);

				try {
					audioStream = AudioSystem.getAudioInputStream(music.get(0));
					clip = AudioSystem.getClip();
					clip.open(audioStream);
				} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
					e.printStackTrace();
				}

				clip.loop(Clip.LOOP_CONTINUOUSLY);
				clip.start();	
			}
		}
	}//end introDrawingPanel class
	//Key listener for enabling player movement

	private class MyKeyListener extends KeyAdapter{
		public void keyPressed(KeyEvent e) {
			int event = e.getKeyCode();

			//Key controls (arrow keys + WASD)
			if(event == 37 || event == 65) { //Left(37), A(65)
				if(player.x - 10 >= 0) player.x -= 10; 
			}
			if(event == 38 || event == 87) { //Up(38), W(87)
				if(player.y - 10 >= 0) player.y -= 10;
			}
			if(event == 39 || event == 68) { //Right(39), D(68)
				if (player.x + player.width + 10 <= dPanel.getWidth()) player.x += 10; 
			}
			if(event == 40 || event == 83) { //Down(40), S(83)
				if (player.y + player.height + 10 <= dPanel.getHeight()) player.y += 10;  
			}
			dPanel.repaint();
		}
	}

	private class MyMouseListener extends MouseAdapter {

		public void mouseMoved(MouseEvent e) {
			//Find cursor position
			mouseX = e.getX();
			mouseY = e.getY();
		}

		public void mouseClicked(MouseEvent e) {

			if(gameWin) { //FOR GAME WIN

				//Mouse boundaries for REPLAY button
				if(mouseX >= 290 && mouseX <= 380) {
					if(mouseY >= 470 && mouseY <= 520) {
						//Stop music, run setup, and reset variables						
						clip.stop();
						clip.close();
						winFrame.dispose();
						scoreboard.dispose();

						try {
							introSetup(); 
						} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e2) {
							e2.printStackTrace();
						}
						setupFrames();
						try {
							setupOtherFeatures();
						} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
							e1.printStackTrace();
						}

						reset();
					}
				}

				//Mouse boundaries for NO button
				if(mouseX >= 465 && mouseX <= 510) {
					if(mouseY >= 470 && mouseY <= 520) {
						//Exit music & application
						winFrame.dispose();
						clip.stop();
						clip.close();
						System.exit(0);
					}
				}

				//Show scoreboard popup
				//If mouse click occurred in this range, display scoreboard window
				if (mouseX >= 330 && mouseX <= 480) {
					if (mouseY >= 385 && mouseY <= 405) {
						scoreboard.setVisible(true);
					}
				}

			}else { //FOR NOT GAME WIN

				if(onIntro) { //FOR GAME OVER & ON INTRO SCREEN
					//Intro screen start button
					//If mouse click occurred in this range, display the enter player's name window
					if (mouseX >= 555 && mouseX <= 650) {
						if (mouseY >= 530 && mouseY <= 580) {
							nameFrame.setVisible(true);
						}
					}

					//Intro screen music 
					//If mouse click occurred in this range, turn music on or off
					if (mouseX > 555 && mouseX < 660) {
						if (mouseY > 380 && mouseY < 480) {
							count++;

							if(count % 2 == 1) playIntro = false;
							else playIntro = true;

							inPanel.repaint();
						}
					}

				}else if(gameLost){ //FOR GAME OVER & LOST
					//Mouse boundaries for REPLAY button
					if(mouseX >= 265 && mouseX <= 360) {
						if(mouseY >= 470 && mouseY <= 520) {
							//Stop music, run setup, and reset variables
							clip.stop();
							clip.close();
							scoreboard.dispose();
							endFrame.dispose();

							try {
								introSetup();
							} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e2) {
								e2.printStackTrace();
							}
							setupFrames();
							try {
								setupOtherFeatures();
							} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
								e1.printStackTrace();
							}

							reset();	
						}
					}

					//Mouse boundaries for NO button
					if(mouseX >= 465 && mouseX <= 510) {
						if(mouseY >= 470 && mouseY <= 520) {
							//Exit music & application
							endFrame.dispose();
							clip.stop();
							clip.close();
							System.exit(0);
						}
					}

					//Show scoreboard popup
					//If mouse click occurred in this range, display scoreboard window
					if (mouseX >= 335 && mouseX <= 450) {
						if (mouseY >= 355 && mouseY <= 375) {
							scoreboard.setVisible(true);
						}
					}
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String eventName = e.getActionCommand();

		//If the submit button on the enter player name window is clicked, store player's name, close the intro, and run the main game
		if (eventName.equals("submit")) {
			onIntro = false;

			playerName = textfield.getText();
			frame.setVisible(true);
			introFrame.setVisible(false);
			nameFrame.setVisible(false);
			obTimer.start();

			//Close intro screen music and update audiostream with level 1 music
			if(playIntro) {
				clip.stop();
				clip.close();
				try {
					audioStream = AudioSystem.getAudioInputStream(music.get(1));
					clip = AudioSystem.getClip();
					clip.open(audioStream);
				} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
					e1.printStackTrace();
				}
				clip.loop(Clip.LOOP_CONTINUOUSLY);
				clip.start();
			}
			obTimer.restart();
		}

		//Process answer for trivia popup when player presses check
		if (eventName.equals("CHECK")) {
			checkCorrect();
		}
	}
}
