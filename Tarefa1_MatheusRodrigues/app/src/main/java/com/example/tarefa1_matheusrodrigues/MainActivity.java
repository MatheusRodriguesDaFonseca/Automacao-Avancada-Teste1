package com.example.tarefa1_matheusrodrigues;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.racelibrary.CarData;
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

import android.os.Process;

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

    private static final int NUM_EXECUCOES = 3;
    private boolean isStarted = false; // Flag para verificar se a corrida já foi iniciada


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
        if (!isStarted) {
            isStarted = true; // Marcar que a corrida foi iniciada

            // Iniciar medições
            medirLeituraSensor(NUM_EXECUCOES);
            medirCentroDeMassa(NUM_EXECUCOES);
            medirMovimentacaoCarros(NUM_EXECUCOES);
            testarEscalonabilidadeComUnicoNucleo(NUM_EXECUCOES);
        }


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

    // CÁLCULO DA LEITURA DO SENSOR
    public void medirLeituraSensor(int numExecucoes) {
        long[] temposExecucao = new long[numExecucoes];
        long lastStartTime = 0;
        long[] periodos = new long[numExecucoes - 1];

        for (int i = 0; i < numExecucoes; i++) {
            long startTime = System.nanoTime();

            // Simulação de leitura do sensor
            lerDadosDoSensor();

            long endTime = System.nanoTime();
            temposExecucao[i] = (endTime - startTime) / 1000000; // Converter para milissegundos
            if (i > 0) {
                periodos[i - 1] = (startTime - lastStartTime) / 1000000; // Período em milissegundos
            }
            lastStartTime = startTime;
        }
        // Calcular e imprimir valores com o deadline sendo o tempo de execução
        calcularEImprimirValores("Leitura do Sensor", numExecucoes, temposExecucao, periodos);
    }

    // Método para medir o Cálculo do Centro de Massa
    public void medirCentroDeMassa(int numExecucoes) {
        long[] temposExecucao = new long[numExecucoes];
        long lastStartTime = 0;
        long[] periodos = new long[numExecucoes - 1];
        List<SensorData> dadosSensores = new ArrayList<>();

        for (Car car : cars) {
            dadosSensores.add(lerDadosDoSensor(car));
        }

        for (int i = 0; i < numExecucoes; i++) {
            long startTime = System.nanoTime();

            // Cálculo do centro de massa
            calcularCentroDeMassa(dadosSensores);

            long endTime = System.nanoTime();
            temposExecucao[i] = (endTime - startTime) / 1000; // Converter para microsegundos

            if (i > 0) {
                periodos[i - 1] = (startTime - lastStartTime) / 1000; // Período em microsegundos
            }
            lastStartTime = startTime;
        }
        calcularEImprimirValores("Cálculo do Centro de Massa", numExecucoes, temposExecucao, periodos);
    }



    // Método para medir a Movimentação dos Carros com Distância
    public void medirMovimentacaoCarros(int numExecucoes) {
        long[] temposExecucao = new long[numExecucoes];
        long lastStartTime = 0;
        long[] periodos = new long[numExecucoes - 1];

        for (int i = 0; i < numExecucoes; i++) {
            long startTime = System.nanoTime();
            Log.d("Movimentação dos Carros", "Início da execução " + (i + 1));

            // Movimentação dos carros e medição de distância
            for (Car car : cars) {
                double distanciaInicial = calcularDistanciaPercorrida(car);
                moverCarros(car, 0.1); // deltaTime = 0.1
                double distanciaPercorrida = calcularDistanciaPercorrida(car) - distanciaInicial;
                Log.d("Movimentação dos Carros", "Carro " + car.getName() + " - Distância Percorrida: " + distanciaPercorrida);

                if (verificarAtraso(car, distanciaPercorrida, startTime)) {
                    ajustarVelocidade(car);
                }
            }

            long endTime = System.nanoTime();
            temposExecucao[i] = (endTime - startTime) / 1000; // Converter para microsegundos

            if (i > 0) {
                periodos[i - 1] = (startTime - lastStartTime) / 1000; // Período em microsegundos
            }
            lastStartTime = startTime;

            Log.d("Movimentação dos Carros", "Fim da execução " + (i + 1));
        }
        calcularEImprimirValores("Movimentação dos Carros", numExecucoes, temposExecucao, periodos);
    }



    // Método para calcular e imprimir Ji, Ci, Pi e Di
    public void calcularEImprimirValores(String tarefa, int numExecucoes, long[] temposExecucao, long[] periodos) {
        long soma = 0;
        long jitter = 0;
        long maxTempoExecucao = 0;

        for (long tempo : temposExecucao) {
            soma += tempo;
            if (tempo > maxTempoExecucao) {
                maxTempoExecucao = tempo; // Encontrar o tempo de execução máximo para usar como deadline
            }
        }

        long media = soma / temposExecucao.length;

        for (long tempo : temposExecucao) {
            jitter += Math.abs(tempo - media);
        }
        jitter /= temposExecucao.length;

        // Calcular Período Médio (Pi)
        long somaPeriodos = 0;
        for (long periodo : periodos) {
            somaPeriodos += periodo;
        }
        long periodoMedio = (numExecucoes > 1) ? somaPeriodos / periodos.length : 0;

        System.out.println(tarefa + " - Tempo de Computação (Ci): " + media + " µs");
        //System.out.println(tarefa + " - Jitter (Ji): " + jitter + " µs");
        //System.out.println(tarefa + " - Período Médio (Pi): " + periodoMedio + " µs");
        System.out.println(tarefa + " - Deadline (Di): " + maxTempoExecucao + " µs");
    }



    // Método Simulado para leitura do sensor
    private void lerDadosDoSensor() {
        try {
            // Simulação de tempo de leitura
            Thread.sleep((long) (Math.random() * 100)); // Pausa de 0 a 100 ms
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Método Simulado para calcular o centro de massa
    private Point calcularCentroDeMassa(List<SensorData> dadosSensores) {
        double somaMassa = 0;
        double somaPosX = 0;
        double somaPosY = 0;

        for (SensorData dados : dadosSensores) {
            somaMassa += dados.mass;
            somaPosX += dados.positionX * dados.mass;
            somaPosY += dados.positionY * dados.mass;
        }

        double centroX = somaPosX / somaMassa;
        double centroY = somaPosY / somaMassa;

        return new Point(centroX, centroY);
    }

    // Método Simulado para ler dados do sensor
    private SensorData lerDadosDoSensor(Car car) {
        SensorData dados = new SensorData();
        dados.mass = Math.random() * 1000; // Simulação de dados
        dados.positionX = car.getX();
        dados.positionY = car.getY();
        return dados;
    }

    // Classe Simulada para os dados do sensor
    class SensorData {
        public double mass;
        public double positionX;
        public double positionY;
    }

    // Classe Simulada para a posição do ponto
    class Point {
        public double x;
        public double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Método para verificar se um carro está atrasado
    private boolean verificarAtraso(Car car, double distanciaPercorrida, long startTime) {
        double tempoDecorrido = (System.nanoTime() - startTime) / 1000000; // Converter para milissegundos
        double velocidadeEsperada = distanciaPercorrida / tempoDecorrido;
        boolean atrasado = velocidadeEsperada < car.velocity;
        if (atrasado) {
            Log.d("Carro Atrasado", "Carro " + car.getName() + " está atrasado. Velocidade Esperada: " + velocidadeEsperada + ", Velocidade Atual: " + car.velocity);
        }
        return atrasado;
    }

    // Método para ajustar a velocidade de um carro atrasado
    private void ajustarVelocidade(Car car) {
        car.velocity *= 1.1; // Aumenta a velocidade em 10%
        Log.d("Ajuste de Velocidade", "Ajustando velocidade do carro " + car.getName() + " para " + car.velocity);
    }

    // Método para calcular a distância percorrida por um carro
    private double calcularDistanciaPercorrida(Car car) {
        return Math.sqrt(Math.pow(car.positionX, 2) + Math.pow(car.positionY, 2));
    }

    // Método Simulado para movimentar um carro
    private void moverCarros(Car car, double deltaTime) {
        car.positionX += car.velocity * deltaTime * Math.cos(car.direction);
        car.positionY += car.velocity * deltaTime * Math.sin(car.direction);
    }

    // Configura o aplicativo para usar apenas um núcleo do processador.
    public void configurarUnicoNucleo() {
        int coreNumber = 15; // Uso so primeiro núcleo
        int mask = 1 << coreNumber; // Cria uma máscara para o núcleo escolhido
        Process.setThreadPriority(Process.myTid(), mask); // Aplica a máscara à thread atual para usar apenas um núcleo
    }

    // Método para Testar Escalonabilidade
    public void testarEscalonabilidadeComUnicoNucleo(int numExecucoes) {
        // Configurar para usar um único núcleo
        configurarUnicoNucleo();

        // Executar medições para cada tarefa
        medirLeituraSensor(numExecucoes);
        medirCentroDeMassa(numExecucoes);
        medirMovimentacaoCarros(numExecucoes);
    }



}

