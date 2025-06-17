import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CatchFrog extends JFrame {
    public static void main(String[] args) {
        new MainMenu();
    }
}


class ScoreStorage {
    private static final Logger logger = Logger.getLogger(ScoreStorage.class.getName());
    private static final String FILE_PATH = "highscores.txt";

    public static void saveScores(ArrayList<Integer> scores) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (int score : scores) {
                writer.println(score);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save scores", e);
        }
    }

    public static ArrayList<Integer> loadScores() {
        ArrayList<Integer> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    scores.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "No scores loaded (file may not exist)", e);
        }
        return scores;
    }

    public static void clearScores() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH));
            writer.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to clear scores", e);
        }
    }
}

class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("Catch Frog");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 1));

        JLabel title = new JLabel("Catch Frog", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JButton startButton = new JButton("Start Game");
        JButton highScoreButton = new JButton("High Scores");
        JButton exitButton = new JButton("Exit");

        startButton.addActionListener(e -> {
            dispose();
            new GameFrame();
        });

        highScoreButton.addActionListener(e -> new HighScoreMenu(ScoreStorage.loadScores(), 0));

        exitButton.addActionListener(e -> System.exit(0));

        add(title);
        add(startButton);
        add(highScoreButton);
        add(exitButton);

        setVisible(true);
    }
}

class GameFrame extends JFrame {
    ArrayList<Integer> scoreHistory;
    int retryCount;

    public GameFrame() {
        this(ScoreStorage.loadScores(), 1);
    }

    public GameFrame(ArrayList<Integer> scoreHistory, int retryCount) {
        this.scoreHistory = scoreHistory;
        this.retryCount = retryCount;

        setTitle("Catch Frog Game");
        setSize(1280, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        GamePanel panel = new GamePanel(this, scoreHistory, retryCount);
        add(panel);
        setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    int retryCount;
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean upPressed = false;
    boolean downPressed = false;

    int velocity;
    int baseVelocity = 20;
    int baseFrogs = 120;
    int baseWalls = 10;

    int playerX = 100, playerY = 100, playerSize = 30;
    Timer timer;
    Timer gameTimer;
    int timeLeft = 10;
    int score = 0;

    ArrayList<Rectangle> walls = new ArrayList<>();
    ArrayList<Point> frogs = new ArrayList<>();
    ArrayList<Integer> scoreHistory;
    JFrame parent;

    public GamePanel(JFrame parent, ArrayList<Integer> scoreHistory, int retryCount) {
        this.parent = parent;
        this.scoreHistory = scoreHistory;
        this.retryCount = retryCount;

        setBackground(Color.GRAY);
        setFocusable(true);
        addKeyListener(this);

        this.velocity = (int) (baseVelocity * (1 + retryCount * 0.15));
        spawnWalls();
        spawnFrogs();

        timer = new Timer(16, this);
        timer.start();

        gameTimer = new Timer(1000, e -> {
            timeLeft--;
            if (timeLeft <= 0) {
                timer.stop();
                gameTimer.stop();
                scoreHistory.add(score);
                ScoreStorage.saveScores(scoreHistory);
                new GameOverMenu(score, scoreHistory, parent, retryCount);
            }
            repaint();
        });
        gameTimer.start();
    }

    void spawnWalls() {
        walls.clear();
        Random rand = new Random();

        int jumlahWall = baseWalls + retryCount;
        int maxWidth = 200;
        int minWidth = 100;
        int wallHeight = 20;

        for (int i = 0; i < jumlahWall; i++) {
            int width = rand.nextInt(maxWidth - minWidth + 1) + minWidth;
            int x = rand.nextInt(1280 - width);
            int y = rand.nextInt(720 - wallHeight);

            Rectangle newWall = new Rectangle(x, y, width, wallHeight);

            Rectangle playerStartZone = new Rectangle(playerX - 50, playerY - 50, 150, 150);
            if (!newWall.intersects(playerStartZone)) {
                walls.add(newWall);
            } else {
                i--;
            }
        }
    }

    void spawnFrogs() {
        frogs.clear();
        Random rand = new Random();
        int jumlahFrogs = (int) (baseFrogs * (1 + retryCount * 0.3));
        for (int i = 0; i < jumlahFrogs; i++) {
            frogs.add(new Point(rand.nextInt(1280), rand.nextInt(720)));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLUE);
        g.fillRect(playerX, playerY, playerSize, playerSize);

        g.setColor(Color.GREEN);
        for (Point p : frogs) {
            g.fillOval(p.x, p.y, 30, 30);
        }

        g.setColor(Color.BLACK);
        for (Rectangle r : walls) {
            g.fillRect(r.x, r.y, r.width, r.height);
        }

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

        frogs.removeIf(p -> {
            Rectangle FrogsRect = new Rectangle(p.x, p.y, 30, 30);
            if (playerRect.intersects(FrogsRect)) {
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
    public GameOverMenu(int score, ArrayList<Integer> history, JFrame parent, int retryCount) {
        setTitle("Game Over");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(6, 1));

        JLabel scoreLabel = new JLabel("Your Score: " + score, JLabel.CENTER);
        JLabel retryLabel = new JLabel("Retry ke-" + retryCount, JLabel.CENTER);
        JButton retryButton = new JButton("Retry speed +15%");
        JButton highScoresButton = new JButton("High Scores");
        JButton exitButton = new JButton("Exit");

        retryButton.addActionListener(e -> {
            dispose();
            parent.dispose();
            new GameFrame(history, retryCount + 1);
        });

        highScoresButton.addActionListener(e -> new HighScoreMenu(history, retryCount));
        exitButton.addActionListener(e -> System.exit(0));

        add(scoreLabel);
        add(retryLabel);
        add(retryButton);
        add(highScoresButton);
        add(exitButton);

        setVisible(true);
    }
}

class HighScoreMenu extends JFrame {
    private final JTextArea scoreArea;

    public HighScoreMenu(ArrayList<Integer> scores, int retryCount) {
        setTitle("High Scores");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        scores.sort(Collections.reverseOrder());

        scoreArea = new JTextArea();
        scoreArea.setEditable(false);

        StringBuilder sb = new StringBuilder("High Scores:\n");
        sb.append("Retry terakhir: ").append(retryCount).append("\n\n");
        int rank = 1;
        for (int s : scores) {
            sb.append(rank++).append(". ").append(s).append("\n");
        }

        scoreArea.setText(sb.toString());
        add(new JScrollPane(scoreArea), BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel(scores);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createButtonPanel(ArrayList<Integer> scores) {
        JPanel panel = new JPanel(new FlowLayout());

        JButton clearButton = new JButton("Clear Scores");
        JButton closeButton = new JButton("Close");

        clearButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Apakah Anda yakin ingin menghapus seluruh skor?",
                    "Konfirmasi Hapus",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                scores.clear();
                ScoreStorage.clearScores();
                scoreArea.setText("High Scores:\n");
            }
        });

        closeButton.addActionListener(e -> dispose());

        panel.add(clearButton);
        panel.add(closeButton);
        return panel;
    }
}