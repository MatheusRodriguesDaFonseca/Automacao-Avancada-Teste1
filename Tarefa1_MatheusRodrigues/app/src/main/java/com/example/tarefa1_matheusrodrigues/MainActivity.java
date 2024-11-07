package com.example.tarefa1_matheusrodrigues;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.widget.TextView;

import java.util.concurrent.Semaphore;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private List<Car> cars;  // Lista de carros
    private List<ImageView> carImageViews;  // Lista de ImageViews dos carros
    private List<View> centerOfMassViews; // Lista de Views do centro de massa para cada carro
    private ConstraintLayout mainLayout;  // Layout principal
    private EditText carrosInput;  // Campo de entrada para o número de carros
    private final Handler handler = new Handler();  // Handler para animação
    private ImageView carImageView;
    private final List<Raceable> raceableCars = new ArrayList<>();
    private TextView penaltyText;
    private TextView statusText;
    Semaphore semaphore = new Semaphore(1);
    private ImageView criticalRegionView; // ImageView para desenhar a região crítica
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialização das listas
        mainLayout = findViewById(R.id.main);
        carrosInput = findViewById(R.id.carros_input);
        cars = new ArrayList<>();
        carImageViews = new ArrayList<>();
        centerOfMassViews = new ArrayList<>();
        statusText = findViewById(R.id.status_text);

        // Inicialização da TextView de penalidade
        penaltyText = findViewById(R.id.penalty_text);

        //Inicializar elementos do layout
        criticalRegionView = findViewById(R.id.critical_region_view);

        // Inicialize o Firestore
        db = FirebaseFirestore.getInstance();

        // Desenhar a região crítica
        criticalRegionView.post(this::drawCriticalRegion);

        // Botão de Start
        ImageButton startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRace();
            }
        });

        // Botão de Pause
        ImageButton pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseRace();
            }
        });


        // Botão de Finish
        ImageButton finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRace();
            }
        });

        // Botão de Reset
        ImageButton resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> resetCarStates());

        loadCarStates();

    }

    private void startRace() {
        boolean safetyCarExists = false;

        // Verificar se o Safety Car já existe
        for (Car car : cars) {
            if (car.getName().equals("SafetyCar")) {
                safetyCarExists = true;
                break;
            }
        }

        // Inicializar o Safety Car se ainda não existir
        if (!safetyCarExists) {
            Car safetyCar = createSafetyCar();
            raceableCars.add(safetyCar);
            safetyCar.startRace();
        }

        // Iniciar todos os carros carregados
        for (Car car : cars) {
            if (!car.isRunning()) {
                car.startRace();
            }
        }

        // Criar novos carros com base na entrada do usuário
        String inputText = carrosInput.getText().toString();
        int numCars = inputText.isEmpty() ? 0 : Integer.parseInt(inputText);
        int carsToAdd = numCars - 1; // Subtrai 1 para o Safety Car

        if (carsToAdd > 0) {
            for (int i = 0; i < carsToAdd; i++) {
                int delay = i * 900;
                final int carIndex = cars.size() + i; // Use cars.size() para garantir que os índices não se repitam
                handler.postDelayed(() -> {
                    Car car = createCar(carIndex);
                    raceableCars.add(car);
                    car.startRace();
                }, delay);
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateCarSpeeds();
                    handler.postDelayed(this, 2000);
                }
            }, 2000);
        }
    }



    // Método para criar o Safety Car
    private Car createSafetyCar() {
        double initialX = 480;
        double initialY = 430;
        double randomSpeed = 15 + Math.random() * 10; // Defina a velocidade entre 15 e 25
        ImageView carImageView = new ImageView(this);
        carImageView.setImageResource(R.drawable.safety); // Recurso de imagem do Safety Car
        int sensorRange = 50;
        Car car = new Car(this, "SafetyCar", initialX, initialY, sensorRange, findViewById(R.id.pista), carImageView, cars);
        car.setSpeed(randomSpeed);
        car.setPriority(Thread.MAX_PRIORITY); // Definindo a prioridade máxima
        cars.add(car);
        int carSize = 37;
        carImageView.setLayoutParams(new ConstraintLayout.LayoutParams(carSize, carSize));
        carImageView.setX((float) car.getX());
        carImageView.setY((float) car.getY());
        mainLayout.addView(carImageView);
        carImageViews.add(carImageView);
        return car;
    }




    private Car createCar(int index) {
        double initialX = 480;
        double initialY = 430;
        double randomSpeed = 1 + Math.random() * 20;
        ImageView carImageView = new ImageView(this);
        carImageView.setImageResource(R.drawable.car);
        int sensorRange = 50;
        Car car = new Car(this, "Carro" + (index + 1), initialX, initialY, sensorRange, findViewById(R.id.pista), carImageView, cars);
        car.setSpeed(randomSpeed);
        cars.add(car); // Adiciona o carro na lista de carros
        int carSize = 37;
        carImageView.setLayoutParams(new ConstraintLayout.LayoutParams(carSize, carSize));
        carImageView.setX((float) car.getX());
        carImageView.setY((float) car.getY());
        mainLayout.addView(carImageView);
        carImageViews.add(carImageView);
        return car;
    }


    private void updateCarPositions() {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            ImageView carImageView = carImageViews.get(i);
            View comView = centerOfMassViews.get(i);

            // Obtém a posição do centro de massa
            Point centerOfMass = car.getCenterOfMassPosition();
            if (centerOfMass != null) {
                comView.setX(centerOfMass.x - (float) comView.getWidth() / 2);
                comView.setY(centerOfMass.y - (float) comView.getHeight() / 2);
                comView.setVisibility(View.VISIBLE);
            } else {
                comView.setVisibility(View.INVISIBLE);
            }

            // Move o carro em direção ao centro de massa
            car.moveTowards(centerOfMass, cars); // Passa a lista de carros para evitar colisões

            // Atualiza a posição da ImageView do carro
            carImageView.setX((float) car.getX() - 40);
            carImageView.setY((float) car.getY() - 20);

            // Rotaciona a ImageView do carro para acompanhar o ângulo
            carImageView.setRotation((float) car.getAngle());

            // Desenhar a distância e o alcance do sensor
            drawSensorRange(car); // Função para desenhar a distância
        }
    }

    private void drawSensorRange(Car car) {
        ImageView sensorCanvas = findViewById(R.id.sensor_canvas); // Acessa o ImageView que usaremos para desenhar

        // Criar um bitmap e um canvas para desenhar
        Bitmap bitmap = Bitmap.createBitmap(sensorCanvas.getWidth(), sensorCanvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Desenhar o círculo de alcance do sensor
        Paint paint = new Paint();
        paint.setColor(Color.TRANSPARENT); // Cor do círculo
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        // Ponto central (posição do carro)
        int carCenterX = (int) car.getX();
        int carCenterY = (int) car.getY();

        // Desenhar o círculo com o raio do sensor
        canvas.drawCircle(carCenterX, carCenterY, car.getSensorRange(), paint);

        // Desenhar o campo de varredura (cone) do sensor
        double angleRad = Math.toRadians(car.getAngle());
        double halfConeAngle = Math.PI / 2; // 45 graus para cada lado
        int sensorRange = car.getSensorRange();

        // Calcular os pontos do cone
        Point tip = new Point(carCenterX + (int) (sensorRange * Math.cos(angleRad)),
                carCenterY + (int) (sensorRange * Math.sin(angleRad)));

        Point left = new Point(carCenterX + (int) (sensorRange * Math.cos(angleRad - halfConeAngle)),
                carCenterY + (int) (sensorRange * Math.sin(angleRad - halfConeAngle)));

        Point right = new Point(carCenterX + (int) (sensorRange * Math.cos(angleRad + halfConeAngle)),
                carCenterY + (int) (sensorRange * Math.sin(angleRad + halfConeAngle)));

        // Desenhar o cone do sensor
        paint.setColor(Color.TRANSPARENT); // Cor do cone
        paint.setStyle(Paint.Style.FILL);
        Path path = new Path();
        path.moveTo(carCenterX, carCenterY); // Ponto de origem
        path.lineTo(tip.x, tip.y); // Ponto da frente do sensor
        path.lineTo(left.x, left.y); // Ponto esquerdo do cone
        path.lineTo(carCenterX, carCenterY); // Volta para o centro
        path.lineTo(right.x, right.y); // Ponto direito do cone
        path.lineTo(tip.x, tip.y); // Conecta ao ponto da frente
        canvas.drawPath(path, paint);

        // Encontrar o ponto mais distante dentro do alcance do sensor
        Point mostDistantPoint = null;
        List<Point> whitePixels = car.scanForWhitePixels();
        double maxDistance = 0;
        for (Point p : whitePixels) {
            double distance = Math.hypot(p.x - carCenterX, p.y - carCenterY);
            if (distance > maxDistance) {
                maxDistance = distance;
                mostDistantPoint = p;
            }
        }

        if (mostDistantPoint != null) {
            // Desenhar o ponto mais distante
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mostDistantPoint.x, mostDistantPoint.y, 10, paint); // Ponto verde no destino
        }

        // Atualiza a imagem da tela com o desenho
        sensorCanvas.setImageBitmap(bitmap);
    }


    private void updateCarSpeeds() {
        for (Car car : cars) {
            double newSpeed = generateRandomSpeed();
            car.setSpeed(newSpeed);
        }
    }

    private double generateRandomSpeed() {
        Random random = new Random();
        return 20 + random.nextInt(11); // Gera um valor entre 15 e 25
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Car car : cars) {
            car.stopRunning();
        }
    }

    public void updatePenaltyText(int penalty) {
        runOnUiThread(() -> penaltyText.setText("Penalidade: " + penalty));
    }

    // Região crítica: por exemplo, entre x = 400 e x = 500
    public boolean isInCriticalRegion(double x, double y) {
        try {
            return (x > 200 && x < 400 && y > 55 && y < 140); // Use as mesmas coordenadas do drawCriticalRegion
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Em caso de exceção, retornar falso por segurança
        }
    }

    private void drawCriticalRegion() {
        int width = criticalRegionView.getWidth();
        int height = criticalRegionView.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);

        int pontoTamanho = 2; // Tamanho dos pontos
        for (int x = 200; x < 400; x += 10) {
            for (int y = 55; y < 140; y += 10) { // Ajuste as coordenadas para desenhar na região desejada
                canvas.drawCircle(x, y, pontoTamanho, paint);
            }
        }

        criticalRegionView.setImageBitmap(bitmap);
    }

    // Método para pausar a corrida
    private void pauseRace() {
        for (Car car : cars) {
            car.stopRunning();
            saveCarState(car); // Salva o estado de cada carro
        }
        runOnUiThread(() -> {
            statusText.setText("Corrida Pausada");
            statusText.setVisibility(View.VISIBLE); // Tornar a TextView visível
            for (ImageView carImageView : carImageViews) {
                carImageView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void finishRace() {
        for (Car car : cars) {
            car.stopRunning();
            saveCarState(car); // Salva o estado de cada carro
        }
        runOnUiThread(() -> {
            statusText.setText("Corrida Finalizada");
            statusText.setVisibility(View.VISIBLE); // Tornar a TextView visível
            for (ImageView carImageView : carImageViews) {
                carImageView.setVisibility(View.VISIBLE);
            }
        });
    }




    private void saveCarState(Car car) {
        CarData carData = new CarData(
                car.getName(), car.getX(), car.getY(), car.getSpeed(), car.getAngle(),
                car.getSensorRange(), car.getDistance(), car.getPenalty(), car.getName().equals("SafetyCar")
        );

        db.collection("cars")
                .document(car.getName())
                .set(carData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Carro salvo com sucesso!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Erro ao salvar carro", e));
    }



    private void loadCarStates() {
        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean hasCars = false;
                        for (DocumentSnapshot document : task.getResult()) {
                            CarData carData = document.toObject(CarData.class);
                            if (carData != null) {
                                createCarFromData(carData, false); // Cria os carros sem iniciá-los
                                hasCars = true;
                            }
                        }
                        if (hasCars) {
                            Log.d("Firestore", "Carros carregados, prontos para iniciar.");
                        } else {
                            Log.d("Firestore", "Nenhum carro salvo encontrado.");
                        }
                    } else {
                        Log.w("Firestore", "Erro ao carregar estados dos carros", task.getException());
                    }
                });
    }



    private void createCarFromData(CarData carData, boolean shouldStartRace) {
        ImageView carImageView = new ImageView(this);
        int carImageResource = carData.isSafetyCar() ? R.drawable.safety : R.drawable.car;
        carImageView.setImageResource(carImageResource);

        Car car = new Car(
                this, carData.getName(), carData.getX(), carData.getY(), carData.getSensorRange(),
                findViewById(R.id.pista), carImageView, cars
        );
        car.setSpeed(carData.getSpeed());
        car.setAngle(carData.getAngle());
        car.setDistance(carData.getDistance());
        car.setPenalty(carData.getPenalty());

        cars.add(car);
        raceableCars.add(car);

        int carSize = 37;
        carImageView.setLayoutParams(new ConstraintLayout.LayoutParams(carSize, carSize));
        carImageView.setX((float) car.getX());
        carImageView.setY((float) car.getY());
        mainLayout.addView(carImageView);
        carImageViews.add(carImageView);

        if (shouldStartRace) {
            car.startRace(); // Inicia o carro apenas se indicado
        }
    }





    private void resetCarStates() {
        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            db.collection("cars").document(document.getId()).delete();
                        }
                        Log.d("Firestore", "Todos os carros foram apagados.");
                        // Reiniciar o estado dos carros na interface
                        runOnUiThread(() -> {
                            cars.clear();
                            carImageViews.clear();
                            mainLayout.removeAllViewsInLayout(); // Remove todas as views de carros sem limpar a interface
                            statusText.setText("Carros foram resetados");
                            statusText.setVisibility(View.VISIBLE);
                        });
                    } else {
                        Log.w("Firestore", "Erro ao apagar carros", task.getException());
                    }
                });
    }



}

