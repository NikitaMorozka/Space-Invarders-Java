package application;

import java.io.BufferedReader;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class Main extends Application {
	private static final String SAVE_FILE = "game_save.txt";
	private Timeline timeline;

//сохранение результата игры
	
	private void saveGame() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE))) {
			writer.write(playerName + "\n");
			writer.write(score + "\n");
			writer.write(player.posX + " " + player.posY + " " + player.exploding + " " + player.destroyed + "\n");
			for (Bomb bomb : Bombs) {
				writer.write(bomb.posX + " " + bomb.posY + " " + bomb.exploding + " " + bomb.destroyed + "\n");
			}
			writer.write("SHOTS\n");
			for (Shot shot : shots) {
				writer.write(shot.posX + " " + shot.posY + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//загрузка результата игры

	private void loadGame() {
		try (BufferedReader reader = new BufferedReader(new FileReader(SAVE_FILE))) {
			playerName = reader.readLine();
			score = Integer.parseInt(reader.readLine());
			String[] playerData = reader.readLine().split(" ");
			player = new Rocket(Integer.parseInt(playerData[0]), Integer.parseInt(playerData[1]), PLAYER_SIZE,
					PLAYER_IMG);
			player.exploding = Boolean.parseBoolean(playerData[2]);
			player.destroyed = Boolean.parseBoolean(playerData[3]);
			Bombs.clear();
			String line;
			while (!(line = reader.readLine()).equals("SHOTS")) {
				String[] bombData = line.split(" ");
				Bomb bomb = new Bomb(Integer.parseInt(bombData[0]), Integer.parseInt(bombData[1]), PLAYER_SIZE,
						BOMBS_IMG[RAND.nextInt(BOMBS_IMG.length)]);
				bomb.exploding = Boolean.parseBoolean(bombData[2]);
				bomb.destroyed = Boolean.parseBoolean(bombData[3]);
				Bombs.add(bomb);
			}

			shots.clear();
			while ((line = reader.readLine()) != null) {
				String[] shotData = line.split(" ");
				Shot shot = new Shot(Integer.parseInt(shotData[0]), Integer.parseInt(shotData[1]));
				shots.add(shot);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//данные игрового поля 
	private String playerName;
	private static final Random RAND = new Random();
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int PLAYER_SIZE = 60;
	static final Image PLAYER_IMG = new Image(
			"file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\playerShip1_orange.png");
	static final Image EXPLOSION_IMG = new Image(
			"file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\explosion.png");
	static final int EXPLOSION_W = 128;
	static final int EXPLOSION_ROWS = 3;
	static final int EXPLOSION_COL = 3;
	static final int EXPLOSION_H = 128;
	static final int EXPLOSION_STEPS = 2;
	static final Image BOMBS_IMG[] = {
			new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\shipBlue_manned.png"),
			new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\shipPink_manned.png"),
			new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\shipYellow_manned.png"),
			new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\shipGreen_manned.png"),
			new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\icon\\shipBeige_manned.png"), };

	final int MAX_BOMBS = 10, MAX_SHOTS = MAX_BOMBS * 2;
	boolean gameOver = false;
	// установка флагов
	private boolean moveLeftKeyPressed = false;
	private boolean moveRightKeyPressed = false;
	private boolean moveUpKeyPressed = false;
	private boolean moveDownKeyPressed = false;
	private boolean spaceKeyPressed = false;
	private boolean enterKeyPressed = false;
	private boolean escapeKeyPressed = false;
	private boolean menuKeyPressed = false;
	private boolean recordSaved = false;

	private GraphicsContext gc;

	Rocket player;
	List<Shot> shots;
	List<Universe> univ;
	List<Bomb> Bombs;

	private void updateRecords() {
		Map<String, Integer> recordsMap = new HashMap<>();
		List<String> records = readRecords();

		// Заполнение Map из существующих записей
		for (String record : records) {
			String[] parts = record.split(":");
			if (parts.length == 2) {
				try {
					String playerName = parts[0];
					int playerScore = Integer.parseInt(parts[1]);
					recordsMap.put(playerName, Math.max(playerScore, recordsMap.getOrDefault(playerName, 0)));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}

		// Проверка и обновление рекорда для текущего игрока
		recordsMap.put(playerName, Math.max(score, recordsMap.getOrDefault(playerName, 0)));

		// Запись обновленных рекордов обратно в файл
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORD_FILE))) {
			for (Map.Entry<String, Integer> entry : recordsMap.entrySet()) {
				writer.write(entry.getKey() + ":" + entry.getValue());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// главное меню
	public void start(Stage stage) throws Exception {
		Pane startPane = new Pane();// Создаем начальный экран
		Scene startScene = new Scene(startPane, WIDTH, HEIGHT);
		Image backgroundImage = new Image("file:C:\\Users\\nikit\\eclipse-workspace\\Space 2\\\\icon\\background.jpg"); // Создаем
																														// объект
																														// BackgroundImage
		BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				new BackgroundSize(WIDTH, HEIGHT, false, false, false, false));
		startPane.setBackground(new Background(background)); // задний фон для панели
		Text text = new Text("Welcome to Space Invaders !"); // Текст на главном меню
		text.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 40));
		text.setFill(Color.WHITE);
		text.setStrokeWidth(2);
		text.setStroke(Color.PURPLE);
		text.setX(75);
		text.setY(75);

		// поле ввода имени игрока
		Label lbl = new Label();
		TextField textField = new TextField();
		textField.setOnAction(e -> {
			playerName = textField.getText();
		});
		textField.setPrefColumnCount(35);
		FlowPane inputPane = new FlowPane(30, 40, textField, lbl);
		inputPane.relocate(200, 150);

		// кнопка просмотра рекордов
		Button scoreRecord = new Button("Score Records");
		scoreRecord.setOnAction(e -> {
			showRecords(stage);
		});
		scoreRecord.setPrefSize(400, 80);
		scoreRecord.relocate(200, 340);
		customizeButton(scoreRecord, Color.web("#33b5e5"), Color.WHITE, 20); // цвет кнопок

		// кнопка запуска игры
		Button startButton = new Button("Start Game");
		startButton.setOnAction(e -> {
			playerName = textField.getText();
			if (!playerName.isEmpty()) {
				startGame(stage); // Запускаем игру при нажатии кнопки
				textField.clear();
			}
		});
		startButton.setPrefSize(400, 80);
		startButton.relocate(200, 220);
		customizeButton(startButton, Color.web("#ff5733"), Color.WHITE, 20); // цвет кнопок

		startPane.getChildren().addAll(text, scoreRecord, inputPane, startButton);
		stage.setScene(startScene);// отображение начальной сцены
		stage.setTitle("Space Invaders");
		stage.show();
	}

	// Настройка фона кнопки
	private void customizeButton(Button button, Color backgroundColor, Color textColor, int fontSize) {
		button.setStyle("-fx-background-color: #" + backgroundColor.toString().substring(2) + "; " + "-fx-text-fill: #"
				+ textColor.toString().substring(2) + "; " + "-fx-font-size: " + fontSize + "px; "
				+ "-fx-background-radius: 10px; " + "-fx-border-radius: 10px;");
	}

	// сохранение рекорда в txt документы - имя : счет
	private int score;
	private static final String RECORD_FILE = "Save.txt";

	private void saveRecord(String name, int score) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORD_FILE, true))) {
			writer.write(name + ":" + score);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Чтение рекордов из файла
	private List<String> readRecords() {
		List<String> records = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(RECORD_FILE))) {
			String line;
			while ((line = reader.readLine()) != null) {
				records.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
	}

	// Запуск игры
	private void startGame(Stage stage) {
		// Создаем холст и графический контекст
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		gc = canvas.getGraphicsContext2D();
		// Создаем и запускаем таймлайн для анимации
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
		Scene scene = new Scene(new StackPane(canvas));

		// Управление - обработка кнопок
		scene.setOnKeyPressed(e -> {
			switch (e.getCode()) {
			case A -> moveLeftKeyPressed = true;
			case D -> moveRightKeyPressed = true;
			case W -> moveUpKeyPressed = true;
			case S -> moveDownKeyPressed = true;
			case SPACE -> spaceKeyPressed = true;
			case F1 -> {
				saveGame();
			}
			case F2 -> {
				loadGame();
			}
			case ENTER -> {
				enterKeyPressed = true;
				if (gameOver) {
					gameOver = false;
					setup();
				}
			}
			case ESCAPE -> {
				escapeKeyPressed = !escapeKeyPressed;
				if (escapeKeyPressed) {
					timeline.pause();
				} else {
					timeline.play();
				}
			}
			case M -> {
				menuKeyPressed = true;
				if (gameOver) {
					gameOver = false;
					timeline.stop();
					try {
						start(stage);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			}
		});
		scene.setOnKeyReleased(e -> {
			switch (e.getCode()) {
			case A -> moveLeftKeyPressed = false;
			case D -> moveRightKeyPressed = false;
			case W -> moveUpKeyPressed = false;
			case S -> moveDownKeyPressed = false;
			case SPACE -> spaceKeyPressed = false;
			case ENTER -> enterKeyPressed = false;
			case M -> menuKeyPressed = false;
			}
		});
		setup();

		stage.setScene(scene);// Создаем и устанавливаем сцену для игры
		stage.setTitle("Space Invaders");
		stage.show();
		canvas.requestFocus();// Фокусируем сцену, чтобы получить события клавиатуры

	}

	// границы передвижения по экрану
	private void moveUpPressed() {
		player.posY -= 30; // скорость передвижения
		if (player.posY < 0) {
			player.posY = 0;
		}
	}

	private void moveDownPressed() {
		player.posY += 30; //
		if (player.posY > HEIGHT - PLAYER_SIZE) {
			player.posY = HEIGHT - PLAYER_SIZE;
		}
	}

	private void movePlayerLeft() {
		player.posX -= 30;
		if (player.posX < 0) {
			player.posX = 0;
		}
	}

	private void movePlayerRight() {
		player.posX += 30;
		if (player.posX > WIDTH - PLAYER_SIZE) {
			player.posX = WIDTH - PLAYER_SIZE;
		}
	}

	// setup the game
	private void setup() {
		univ = new ArrayList<>();
		shots = new ArrayList<>();
		Bombs = new ArrayList<>();
		player = new Rocket(WIDTH / 2, HEIGHT - PLAYER_SIZE, PLAYER_SIZE, PLAYER_IMG);
		score = 0;
		recordSaved = false; // Reset the flag when the game is set up
		IntStream.range(0, MAX_BOMBS).mapToObj(i -> this.newBomb()).forEach(Bombs::add);
	}

	// Графика, применение механик игры
	private void run(GraphicsContext gc) {
		if (escapeKeyPressed) {
			return; // Прекращаем выполнение метода, если игра на паузе
		}
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setFont(Font.font(20));
		gc.setFill(Color.WHITE);
		if (gameOver) {
			gc.setFont(Font.font(35));
			gc.setFill(Color.YELLOW);
			gc.fillText(" \n Press Enter to play", WIDTH / 2, HEIGHT / 2.5);
			gc.fillText(" \n Press M to exit the main menu", WIDTH / 2, HEIGHT / 2);
			if (gameOver && !recordSaved) {
				updateRecords();
				recordSaved = true;
			}
			return;
		}
		gc.setFont(Font.font(20));
		gc.setFill(Color.WHITE);
		gc.fillText("Score: " + score, WIDTH / 2, 20);
		if (moveLeftKeyPressed) {
			movePlayerLeft();
		}
		if (moveRightKeyPressed) {
			movePlayerRight();
		}
		if (moveDownKeyPressed) {
			moveDownPressed();
		}
		if (moveUpKeyPressed) {
			moveUpPressed();
		}
		if (spaceKeyPressed && shots.size() < MAX_SHOTS) {// Стреляем, если нажата клавиша SPACE
			shots.add(player.shoot());
		}
		univ.forEach(Universe::draw);
		player.update();
		player.draw();
		Bombs.stream().peek(Rocket::update).peek(Rocket::draw).forEach(e -> {
			if (player.colide(e) && !player.exploding) {
				player.explode();
			}
		});
		for (int i = shots.size() - 1; i >= 0; i--) {
			Shot shot = shots.get(i);
			if (shot.posY < 0 || shot.toRemove) {
				shots.remove(i);
				continue;
			}
			shot.update();
			shot.draw();
			for (Bomb bomb : Bombs) {
				if (shot.colide(bomb) && !bomb.exploding) {
					score++;
					bomb.explode();
					shot.toRemove = true;
				}
			}
		}
		for (int i = Bombs.size() - 1; i >= 0; i--) {
			if (Bombs.get(i).destroyed) {
				Bombs.set(i, newBomb());
			}
		}
		gameOver = player.destroyed;
		if (RAND.nextInt(10) > 2) {
			univ.add(new Universe());
		}
		for (int i = 0; i < univ.size(); i++) {
			if (univ.get(i).posY > HEIGHT)
				univ.remove(i);
		}
	}

	// Анимации выстрелов
	public class Rocket {
		int posX, posY, size;
		boolean exploding, destroyed;
		Image img;
		int explosionStep = 0;

		public Rocket(int posX, int posY, int size, Image image) {
			this.posX = posX;
			this.posY = posY;
			this.size = size;
			img = image;
		}

		public Shot shoot() {
			return new Shot(posX + size / 2 - Shot.size / 2, posY - Shot.size);
		}

		public void update() {
			if (exploding)
				explosionStep++;
			destroyed = explosionStep > EXPLOSION_STEPS;
		}

		public void draw() {
			if (exploding) {
				gc.drawImage(EXPLOSION_IMG, explosionStep % EXPLOSION_COL * EXPLOSION_W,
						(explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1, EXPLOSION_W, EXPLOSION_H, posX, posY, size,
						size);
			} else {
				gc.drawImage(img, posX, posY, size, size);
			}
		}

		public boolean colide(Rocket other) {
			int d = distance(this.posX + size / 2, this.posY + size / 2, other.posX + other.size / 2,
					other.posY + other.size / 2);
			return d < other.size / 2 + this.size / 2;
		}

		public void explode() {
			exploding = true;
			explosionStep = -1;
		}
	}

	// Скорость бомб
	public class Bomb extends Rocket {
		int SPEED = (score / 6) + 2;

		public Bomb(int posX, int posY, int size, Image image) {
			super(posX, posY, size, image);
		}

		public void update() {
			super.update();
			if (!exploding && !destroyed)
				posY += SPEED;
			if (posY > HEIGHT)
				destroyed = true;
		}
	}

	// Выстрелы
	public class Shot {
		public boolean toRemove;
		int posX, posY, speed = 10;
		static final int size = 6;

		public Shot(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}

		public void update() {
			posY -= speed;
		}

		public void draw() {
			gc.setFill(Color.RED);
			if (score > 100) {
				gc.setFill(Color.YELLOWGREEN);
				speed = 50;
				gc.fillRect(posX - 5, posY - 10, size + 10, size + 30);
			} else {
				gc.fillOval(posX, posY, size, size);
			}
		}

		public boolean colide(Rocket Rocket) {
			int distance = distance(this.posX + size / 2, this.posY + size / 2, Rocket.posX + Rocket.size / 2,
					Rocket.posY + Rocket.size / 2);
			return distance < Rocket.size / 2 + size / 2;
		}
	}

	// Движение частиц (звезды)
	public class Universe {
		int posX, posY;
		private int h, w, r, g, b;
		private double opacity;

		public Universe() {
			posX = RAND.nextInt(WIDTH);
			posY = 0;
			w = RAND.nextInt(5) + 1;
			h = RAND.nextInt(5) + 1;
			r = RAND.nextInt(100) + 150;
			g = RAND.nextInt(100) + 150;
			b = RAND.nextInt(100) + 150;
			opacity = RAND.nextFloat();
			if (opacity < 0)
				opacity *= -1;
			if (opacity > 0.5)
				opacity = 0.5;
		}

		public void draw() {
			if (opacity > 0.8)
				opacity -= 0.01;
			if (opacity < 0.1)
				opacity += 0.01;
			gc.setFill(Color.rgb(255, 255, 255));
			gc.fillOval(posX, posY, w, h);
			posY += 20;
		}
	}

	// Появление бомб (кораблей)
	Bomb newBomb() {
		return new Bomb(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, BOMBS_IMG[RAND.nextInt(BOMBS_IMG.length)]);
	}

	int distance(int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
	}

	// Визуализация окна рекордов, обработка рекордов
	private void showRecords(Stage stage) {
		VBox recordsPane = new VBox();
		recordsPane.setAlignment(Pos.CENTER); // Центрируем содержимое
		Scene recordsScene = new Scene(recordsPane, WIDTH, HEIGHT);

		List<String> records = readRecords();
		List<Pair> pairs = new ArrayList<>();

		for (String record : records) {
			String[] parts = record.split(":");
			if (parts.length == 2) {
				try {
					String name = parts[0];
					int score = Integer.parseInt(parts[1]);
					pairs.add(new Pair(score, name));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		pairs.sort(Comparator.comparingInt(Pair::getPoint).reversed());
		if (pairs.size() > 10) {
			pairs = pairs.subList(0, 10);
		}
		StringBuilder recordsText = new StringBuilder("Score Records:\n");
		for (Pair pair : pairs) {
			recordsText.append(pair.getName()).append(": ").append(pair.getPoint()).append("\n");
		}

		Image backgroundImage = new Image(
				"file:C:\\\\Users\\\\nikit\\\\eclipse-workspace\\\\Space 2\\\\icon\\background.jpg"); // Создаем объект
																										// BackgroundImage
		BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				new BackgroundSize(WIDTH, HEIGHT, false, false, false, false));

		recordsPane.setBackground(new Background(background));

		Text recordsDisplay = new Text(recordsText.toString());
		recordsDisplay.setFont(Font.font("verdana", FontWeight.NORMAL, FontPosture.REGULAR, 30));
		recordsDisplay.setFill(Color.WHITE);
		recordsDisplay.setX(50);
		recordsDisplay.setY(50);

		Button backButton = new Button("Back");
		backButton.setOnAction(e -> {
			try {
				start(stage);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

		customizeButton(backButton, Color.web("#ff5733"), Color.WHITE, 20);
		recordsPane.getChildren().addAll(recordsDisplay, backButton);
		stage.setScene(recordsScene);
		stage.setTitle("Score Records");
		stage.show();
	}

	// часть кода для сортировки списка рекордов
	private static class Pair {
		private final int point;
		private final String name;

		public Pair(int point, String name) {
			this.point = point;
			this.name = name;
		}

		public int getPoint() {
			return point;
		}

		public String getName() {
			return name;
		}
	}

}
