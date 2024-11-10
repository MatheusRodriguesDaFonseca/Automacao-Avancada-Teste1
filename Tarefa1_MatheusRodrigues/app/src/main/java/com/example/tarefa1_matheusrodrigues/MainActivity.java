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

import com.example.racelibrary.CarData;
import com.example.racelibrary.CalculationUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.racelibrary.FirestoreUtils;


import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.widget.TextView;
import android.widget.Toast;

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

    /**
     * Inicia a corrida, verificando e inicializando o Safety Car,
     * iniciando carros carregados e criando novos carros com base na entrada do usuário.
     */
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

    /**
     * Desenha uma região crítica na view `criticalRegionView`.
     * A região crítica é representada por uma série de pontos verdes desenhados em uma área específica.
     */
    private void drawCriticalRegion() {
        // Obtém a largura e a altura da `criticalRegionView`
        int width = criticalRegionView.getWidth();
        int height = criticalRegionView.getHeight();

        // Cria um bitmap para desenhar na view
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Configura a pintura para desenhar os pontos
        Paint paint = new Paint();
        paint.setColor(Color.GREEN); // Define a cor dos pontos como verde
        paint.setStyle(Paint.Style.FILL); // Preenche os pontos

        int pontoTamanho = 2; // Tamanho dos pontos

        // Desenha os pontos em uma área específica definida por coordenadas x e y
        for (int x = 200; x < 400; x += 10) {
            for (int y = 55; y < 140; y += 10) {
                canvas.drawCircle(x, y, pontoTamanho, paint);
            }
        }

        // Define o bitmap resultante na `criticalRegionView`
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
        CarData carData = new CarData(car.getName(), car.getX(), car.getY(), car.getSpeed(), car.getAngle(), car.getSensorRange(), car.getDistance(), car.getPenalty(), car.getName().equals("SafetyCar"));
        FirestoreUtils.saveCarState(carData);
    }


    private void loadCarStates() {
        FirestoreUtils.loadCarStates(carData -> createCarFromData(carData, false));
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
        car.setAngle(carData.getAngle()); // Restaurar o ângulo corretamente
        car.setDistance(carData.getDistance());
        car.setPenalty(carData.getPenalty());

        cars.add(car);
        raceableCars.add(car);

        int carSize = 37;
        carImageView.setLayoutParams(new ConstraintLayout.LayoutParams(carSize, carSize));
        carImageView.setX((float) car.getX());
        carImageView.setY((float) car.getY());
        carImageView.setRotation((float) car.getAngle()); // Aplicar a rotação ao ImageView
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

                            // Exibir uma mensagem de "Jogo Resetado" usando Toast
                            Toast.makeText(MainActivity.this, "Jogo Resetado", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "Reinicie o Jogo", Toast.LENGTH_SHORT).show();


                            // Redefinir a cor de fundo do mainLayout, se necessário
                            mainLayout.setBackgroundColor(Color.WHITE); // Ajuste a cor conforme necessário

                            // Opcional: Redefinir a visibilidade do statusText
                            statusText.setVisibility(View.VISIBLE);
                        });
                    } else {
                        Log.w("Firestore", "Erro ao apagar carros", task.getException());
                    }
                });
    }


}

