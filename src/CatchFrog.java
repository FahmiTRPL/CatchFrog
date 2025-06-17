import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.*;

public class CatchFrog extends JFrame {
    public static void main(String[] args) {
        new MainMenu();
    }
}

class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("Catch Frog");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1));

        JLabel title = new JLabel("Catch Frog", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JButton startButton = new JButton("Start Game");
        JButton exitButton = new JButton("Exit");

        startButton.addActionListener(e -> {
            dispose();
            new GameFrame();
        });

        exitButton.addActionListener(e -> System.exit(0));

        add(title);
        add(startButton);
        add(exitButton);

        setVisible(true);
    }
}

class GameFrame extends JFrame {
    ArrayList<Integer> scoreHistory = new ArrayList<>();

    public GameFrame() {
        setTitle("Catch Frog Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        GamePanel panel = new GamePanel(this, scoreHistory);
        add(panel);
        setVisible(true);
    }
}


class GamePanel extends JPanel implements ActionListener, KeyListener {
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean upPressed = false;
    boolean downPressed = false;

    int playerX = 100, playerY = 100, playerSize = 30;
    int velocity = 20;
    Timer timer;
    Timer gameTimer;
    int timeLeft = 10;
    int score = 0;

    ArrayList<Rectangle> walls = new ArrayList<>();
    ArrayList<Point> coins = new ArrayList<>();
    ArrayList<Integer> scoreHistory;
    JFrame parent;

    public GamePanel(JFrame parent, ArrayList<Integer> scoreHistory) {
        this.parent = parent;
        this.scoreHistory = scoreHistory;
        setBackground(Color.GRAY);
        setFocusable(true);
        addKeyListener(this);
        spawnWalls();
        spawnCoins();

        timer = new Timer(16, this);
        timer.start();

        gameTimer = new Timer(1000, e -> {
            timeLeft--;
            if (timeLeft <= 0) {
                timer.stop();
                gameTimer.stop();
                scoreHistory.add(score); // Save score
                new GameOverMenu(score, scoreHistory, parent);
            }
            repaint();
        });
        gameTimer.start();
    }

    void spawnWalls() {
        walls.add(new Rectangle(200, 150, 100, 20));
        walls.add(new Rectangle(400, 300, 150, 20));
        walls.add(new Rectangle(300, 450, 200, 20));
    }

    void spawnCoins() {
        Random rand = new Random();
        for (int i = 0; i < 60; i++) {
            coins.add(new Point(rand.nextInt(750), rand.nextInt(550)));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw player
        g.setColor(Color.BLUE);
        g.fillRect(playerX, playerY, playerSize, playerSize);

        // Draw coins
        g.setColor(Color.GREEN);
        for (Point p : coins) {
            g.fillOval(p.x, p.y, 30, 30);
        }

        // Draw walls
        g.setColor(Color.BLACK);
        for (Rectangle r : walls) {
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        // Draw score and timer
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Time: " + timeLeft, 10, 40);
    }

    public void actionPerformed(ActionEvent e) {
        int oldX = playerX;
        int oldY = playerY;

        if (leftPressed) playerX -= velocity;
        if (rightPressed) playerX += velocity;
        if (upPressed) playerY -= velocity;
        if (downPressed) playerY += velocity;

        Rectangle playerRect = new Rectangle(playerX, playerY, playerSize, playerSize);

        for (Rectangle r : walls) {
            if (playerRect.intersects(r)) {
                playerX = oldX;
                playerY = oldY;
                playerRect.setBounds(playerX, playerY, playerSize, playerSize);
                break;
            }
        }

        // Hitbox coin 30x30 (sesuai oval)
        coins.removeIf(p -> {
            Rectangle coinRect = new Rectangle(p.x, p.y, 30, 30);
            if (playerRect.intersects(coinRect)) {
                score++;
                return true;
            }
            return false;
        });

        repaint();
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
    }

    public void keyTyped(KeyEvent e) {}
}

class GameOverMenu extends JFrame {
    public GameOverMenu(int score, ArrayList<Integer> history, JFrame parent) {
        setTitle("Game Over");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 1));

        JLabel scoreLabel = new JLabel("Game Over! Your Score: " + score, JLabel.CENTER);
        JButton retryButton = new JButton("Retry");
        JButton highScoresButton = new JButton("High Scores");
        JButton exitButton = new JButton("Exit");

        retryButton.addActionListener(e -> {
            dispose();
            parent.dispose();
            GameFrame newGame = new GameFrame();
            newGame.scoreHistory = history;  // BAWA DATA LAMA
        });

        highScoresButton.addActionListener(e -> new HighScoreMenu(history));
        exitButton.addActionListener(e -> System.exit(0));

        add(scoreLabel);
        add(retryButton);
        add(highScoresButton);
        add(exitButton);

        setVisible(true);
    }
}

class HighScoreMenu extends JFrame {
    public HighScoreMenu(ArrayList<Integer> scores) {
        setTitle("High Scores");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        scores.sort(Collections.reverseOrder());

        JTextArea scoreArea = new JTextArea();
        scoreArea.setEditable(false);

        StringBuilder sb = new StringBuilder("High Scores:\n");
        int rank = 1;
        for (int s : scores) {
            sb.append(rank++).append(". ").append(s).append("\n");
        }

        scoreArea.setText(sb.toString());
        add(new JScrollPane(scoreArea), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        add(closeButton, BorderLayout.SOUTH);

        setVisible(true);
    }
}
