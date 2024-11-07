package com.example.tarefa1_matheusrodrigues;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.View;
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
    private boolean running = true;
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
    }

    public double generateRandomSpeed() {
        Random random = new Random();
        return 20 + random.nextInt(11); // Gera um valor entre 15 e 25
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

    public List<Point> scanForWhitePixels() {
        List<Point> whitePixels = new ArrayList<>();
        Bitmap bitmap = getBitmapFromView(trackView);
        Point carCenter = new Point((int) x, (int) y + (carImageView.getWidth() / 2));
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double angleRad = Math.toRadians(angle);
        double halfConeRad = Math.PI / 4;
        int sensorRangeSquared = sensorRange * sensorRange;
        for (int i = -sensorRange; i <= sensorRange; i++) {
            for (int j = -sensorRange; j <= sensorRange; j++) {
                double distanceSquared = i * i + j * j;
                if (distanceSquared > sensorRangeSquared) {
                    continue;
                }
                double pixelAngle = Math.atan2(j, i);
                double angleDiff = pixelAngle - angleRad;
                if (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                if (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
                if (angleDiff >= -halfConeRad && angleDiff <= halfConeRad) {
                    int pixelX = carCenter.x + i;
                    int pixelY = carCenter.y + j;
                    if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                        int pixelColor = bitmap.getPixel(pixelX, pixelY);
                        if (Color.red(pixelColor) == 255 && Color.green(pixelColor) == 255 && Color.blue(pixelColor) == 255) {
                            whitePixels.add(new Point(pixelX, pixelY));
                        }
                    }
                }
            }
        }
        return whitePixels;
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

    public void moveTowards(Point target, List<Car> otherCars) {
        try {
            if (target != null) {
                double deltaX = target.x - x;
                double deltaY = target.y - y;
                double angleToTarget = Math.atan2(deltaY, deltaX);
                boolean shouldAvoid = false;
                boolean avoidToLeft = false;

                for (Car otherCar : otherCars) {
                    if (otherCar != this) {
                        double distance = Math.hypot(otherCar.getX() - x, otherCar.getY() - y);
                        double angleToOtherCar = Math.atan2(otherCar.getY() - y, otherCar.getX() - x);
                        double angleDifference = Math.abs(Math.toDegrees(angleToOtherCar - angleToTarget));
                        if (distance < sensorRange && angleDifference < 45) {
                            shouldAvoid = true;
                            avoidToLeft = angleToOtherCar > angleToTarget;
                            break;
                        }
                    }
                }

                if (shouldAvoid) {
                    angleToTarget += avoidToLeft ? -Math.PI / 6 : Math.PI / 6;
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
                if (Color.red(pixelColor) == 255 && Color.green(pixelColor) == 255 && Color.blue(pixelColor) == 255) {
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
                        if (Color.red(adjustedPixelColor) == 255 && Color.green(adjustedPixelColor) == 255 && Color.blue(adjustedPixelColor) == 255) {
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
                        double distanceToOtherCar = Math.hypot(otherCar.getX() - x, otherCar.getY() - y);
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

    public Point getFrontPosition() {
        double angleRad = Math.toRadians(angle);
        int frontX = (int) (x + Math.cos(angleRad) * 10);
        int frontY = (int) (y + Math.sin(angleRad) * 10);
        return new Point(frontX, frontY);
    }

    public Point getCenterOfMassPosition() {
        Point centerMass = calculateCenterOfMass();
        return (centerMass != null) ? new Point(centerMass.x, centerMass.y - (carImageView.getHeight() / 2)) : getFrontPosition();
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public void startRace() {
        Log.d("Car", "Iniciando corrida para " + name);
        Thread carThread = new Thread(this);
        carThread.setPriority(priority); // Definindo a prioridade do thread
        carThread.start();
    }

    @Override
    public void run() {
        Log.d("Car", "Thread iniciada para " + name + " com prioridade " + priority);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!running) {
                    break;
                }
                if (paused) {
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
                centerOfMassView.setVisibility(View.VISIBLE);  //tornar centro de massa transparente
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