package com.example.tarefa1_matheusrodrigues;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.View;
import com.example.racelibrary.CarData;
import com.example.racelibrary.CalculationUtils;

import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;


public class Car implements Runnable, Raceable {
    private final String name;
    private double x;
    private double y;
    private double speed;
    private final int sensorRange;
    private final View trackView;
    private double angle;
    private Point centerOfMass;
    private final ImageView carImageView;
    private final Activity activity;
    private final List<Car> cars;
    private final View centerOfMassView;
    private boolean running = false;
    private double distance = 0;  // Variável para armazenar a distância percorrida
    private int penalty = 0;  // Variável para armazenar as penalidades
    private int priority = Thread.NORM_PRIORITY; // Prioridade padrão
    private volatile boolean paused = false;



    public Car(Activity activity, String name, double x, double y, int sensorRange, View trackView, ImageView carImageView, List<Car> cars) {
        this.activity = activity;
        this.name = name;
        this.x = x;
        this.y = y;
        this.speed = generateRandomSpeed();
        this.sensorRange = sensorRange;
        this.trackView = trackView;
        this.angle = 0;
        this.centerOfMass = new Point((int) x, (int) y);
        this.carImageView = carImageView;
        this.cars = cars;

        this.centerOfMassView = new View(activity);
        this.centerOfMassView.setLayoutParams(new ConstraintLayout.LayoutParams(5, 5));
        this.centerOfMassView.setBackgroundColor(Color.BLACK);
        this.centerOfMassView.setVisibility(View.INVISIBLE);
        ((ConstraintLayout) activity.findViewById(R.id.main)).addView(centerOfMassView);

        Thread carThread = new Thread(this);
        carThread.setPriority(priority);
        carThread.start();
    }

    // Use o método da CalculationUtils para gerar a velocidade aleatória
    public double generateRandomSpeed() {
        return CalculationUtils.generateRandomSpeed();
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getSpeed() {
        return this.speed;
    }

    public int getSensorRange() {
        return this.sensorRange;
    }

    public double getAngle() {
        return this.angle;
    }

    public Point getCenterOfMass() {
        return this.centerOfMass;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPaused() {
        return paused;
    }

    // Método para definir a posição Y
    public void setY(double y) {
        this.y = y;
    }

    // Método para definir a posição X
    public void setX(double x) {
        this.x = x;
    }

    public int getPenalty(){
        return this.penalty;
    }

    public double getDistance(){
        return this.distance;
    }

    public double setDistance(double distance){
        return this.distance = distance;
    }

    public int setPenalty(int penalty){
        return this.penalty = penalty;
    }

    public boolean isRunning(){
        return running;
    }

    // Faz uma varredura na área ao redor do carro para encontrar pixels brancos dentro do alcance do sensor
    public List<Point> scanForWhitePixels() {
        Bitmap bitmap = getBitmapFromView(trackView);
        return CalculationUtils.scanForWhitePixels(bitmap, x, y + (carImageView.getWidth() / 2), angle, sensorRange);
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public Point calculateCenterOfMass() {
        List<Point> whitePixels = scanForWhitePixels();
        if (whitePixels.isEmpty()) {
            return null;
        }
        int sumX = 0;
        int sumY = 0;
        for (Point p : whitePixels) {
            sumX += p.x;
            sumY += p.y;
        }
        int centerX = sumX / whitePixels.size();
        int centerY = sumY / whitePixels.size();
        centerOfMass = new Point(centerX, centerY);
        return centerOfMass;
    }

    // Método para mover o carro para o próximo ponto
    // OtherCars A lista de outros carros para evitar colisões.
    public void moveTowards(Point target, List<Car> otherCars) {
        try {
            if (target != null) {
                double deltaX = target.x - x;
                double deltaY = target.y - y;
                double angleToTarget = CalculationUtils.calculateAngle(deltaX, deltaY);
                boolean shouldAvoid = false;
                boolean avoidToLeft = false;

                for (Car otherCar : otherCars) {
                    if (otherCar != this) {
                        double distance = CalculationUtils.calculateDistance(otherCar.getX(), otherCar.getY(), x, y);
                        double angleToOtherCar = CalculationUtils.calculateAngle(otherCar.getY() - y, otherCar.getX() - x);
                        double angleDifference = Math.abs(Math.toDegrees(angleToOtherCar - angleToTarget));
                        if (distance < sensorRange && angleDifference < 45) {
                            shouldAvoid = true;
                            avoidToLeft = angleToOtherCar > angleToTarget;
                            break;
                        }
                    }
                }

                if (shouldAvoid) {
                    angleToTarget = CalculationUtils.adjustAngleToAvoidCollision(angleToTarget, avoidToLeft);
                }

                double nextX = x + Math.cos(angleToTarget) * speed;
                double nextY = y + Math.sin(angleToTarget) * speed;

                // Verificar se está entrando na região crítica
                boolean enteringCriticalRegion = ((MainActivity) activity).isInCriticalRegion(nextX, nextY);
                boolean currentlyInCriticalRegion = ((MainActivity) activity).isInCriticalRegion(x, y);

                if (enteringCriticalRegion && !currentlyInCriticalRegion) {
                    try {
                        ((MainActivity) activity).semaphore.acquire(); // Adquire o semáforo ao entrar na região crítica
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Bitmap bitmap = getBitmapFromView(trackView);
                int pixelColor = bitmap.getPixel((int) nextX, (int) nextY);
                if (CalculationUtils.isWhitePixel(pixelColor)) {
                    x = nextX;
                    y = nextY;
                    angle = Math.toDegrees(angleToTarget);
                    distance += speed;  // Incrementar a distância percorrida
                } else {
                    penalty++;  // Incrementar a penalidade em caso de colisão
                    activity.runOnUiThread(() -> ((MainActivity) activity).updatePenaltyText(penalty));
                    boolean newTargetFound = false;
                    for (int angleOffset = -45; angleOffset <= 45; angleOffset += 15) {
                        double adjustedAngle = angleToTarget + Math.toRadians(angleOffset);
                        double adjustedX = x + Math.cos(adjustedAngle) * speed;
                        double adjustedY = y + Math.sin(adjustedAngle) * speed;
                        int adjustedPixelColor = bitmap.getPixel((int) adjustedX, (int) adjustedY);
                        if (CalculationUtils.isWhitePixel(adjustedPixelColor)) {
                            x = adjustedX;
                            y = adjustedY;
                            angle = Math.toDegrees(adjustedAngle);
                            newTargetFound = true;
                            distance += speed;  // Incrementar a distância percorrida
                            break;
                        }
                    }
                    if (!newTargetFound) {
                        recheckPath(bitmap);
                    }
                }

                // Verificar se está saindo da região crítica
                if (currentlyInCriticalRegion && !enteringCriticalRegion) {
                    ((MainActivity) activity).semaphore.release(); // Libera o semáforo ao sair da região crítica
                }

                // Detectar colisões com outros carros
                for (Car otherCar : otherCars) {
                    if (otherCar != this) {
                        double distanceToOtherCar = CalculationUtils.calculateDistance(otherCar.getX(), otherCar.getY(), x, y);
                        if (distanceToOtherCar < 5) {  // Supondo um raio de colisão de 5 pixels
                            penalty++;  // Incrementar a penalidade em caso de colisão com outro carro
                            activity.runOnUiThread(() -> ((MainActivity) activity).updatePenaltyText(penalty));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para verificar se o carro está na região crítica
    // Envolve cálculos e serve para ajustar a trajetória do carro em caso de colisão ou obstáculo detectado no caminho.
    private void recheckPath(Bitmap bitmap) {
        for (int angleOffset = -45; angleOffset <= 45; angleOffset += 15) {
            double adjustedAngle = Math.toRadians(angle) + Math.toRadians(angleOffset);
            double adjustedX = x + Math.cos(adjustedAngle) * speed;
            double adjustedY = y + Math.sin(adjustedAngle) * speed;
            int adjustedPixelColor = bitmap.getPixel((int) adjustedX, (int) adjustedY);
            if (Color.red(adjustedPixelColor) == 255 && Color.green(adjustedPixelColor) == 255 && Color.blue(adjustedPixelColor) == 255) {
                x = adjustedX;
                y = adjustedY;
                angle = Math.toDegrees(adjustedAngle);
                break;
            }
        }
    }


    // Método para obter a posição frontal do carro
    public Point getFrontPosition() {
        return CalculationUtils.getFrontPosition(x, y, angle);
    }

    public Point getCenterOfMassPosition() {
        Point centerMass = calculateCenterOfMass();
        return (centerMass != null) ? new Point(centerMass.x, centerMass.y - (carImageView.getHeight() / 2)) : getFrontPosition();
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public synchronized void startRace() {
        Log.d("Car", "Iniciando corrida para " + name);
        running = true;  // Define o carro como em execução
        notify();  // Notifica a thread para começar a corrida
    }

    @Override
    public void run() {
        Log.d("Car", "Thread iniciada para " + name + " com prioridade " + priority);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!running) {  // Espera até o startRace() ser chamado
                    synchronized (this) {
                        wait();
                    }
                }
                if (paused) {  // Pausa a execução se necessário
                    synchronized (this) {
                        wait();
                    }
                }
                updateCarPosition();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e("CarRunnable", "Erro no método run: ", e);
            }
        }
    }

    public void updateCarPosition() {
        Point target = getCenterOfMassPosition();
        moveTowards(target, cars);
        activity.runOnUiThread(() -> {
            carImageView.setX((float) x);
            carImageView.setY((float) y);
            carImageView.setRotation((float) angle);
            Point centerOfMassPosition = getCenterOfMassPosition();
            if (centerOfMassPosition != null) {
                centerOfMassView.setX(centerOfMassPosition.x);
                centerOfMassView.setY(centerOfMassPosition.y);
                centerOfMassView.setVisibility(View.INVISIBLE);  //tornar centro de massa transparente
            } else {
                centerOfMassView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notify();
    }

    public void stopRunning() {
        running = false;
        resume(); // Para garantir que se o thread estiver pausado, ele possa ser interrompido
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}