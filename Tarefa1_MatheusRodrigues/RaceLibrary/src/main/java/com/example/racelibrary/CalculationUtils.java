package com.example.racelibrary;

import java.util.Random;

import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class CalculationUtils {

    /**
     * Gera uma velocidade aleatória no intervalo [20, 30].
     * @return A velocidade aleatória gerada.
     */
    public static double generateRandomSpeed() {
        Random random = new Random();
        return 20 + random.nextInt(11); // Gera um valor entre 20 e 30
    }

    // Faz uma varredura na área ao redor do carro para encontrar pixels brancos dentro do alcance do sensor
    public static List<Point> scanForWhitePixels(Bitmap bitmap, double x, double y, double angle, int sensorRange) {
        List<Point> whitePixels = new ArrayList<>();
        Point carCenter = new Point((int) x, (int) y);
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

    // Calcula o ângulo entre dois pontos.
    public static double calculateAngle(double deltaX, double deltaY) {
        return Math.atan2(deltaY, deltaX);
    }

    // Calcula a distância entre dois pontos.
    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    // Ajusta o ângulo para evitar colisões.
    public static double adjustAngleToAvoidCollision(double angleToTarget, boolean avoidToLeft) {
        return angleToTarget + (avoidToLeft ? -Math.PI / 6 : Math.PI / 6);
    }

    // Verifica se uma cor de pixel é branca.
    public static boolean isWhitePixel(int color) {
        return Color.red(color) == 255 && Color.green(color) == 255 && Color.blue(color) == 255;
    }

    // Calcula a posição frontal do carro com base nas coordenadas atuais e no ângulo.
    public static Point getFrontPosition(double x, double y, double angle) {
        double angleRad = Math.toRadians(angle);
        int frontX = (int) (x + Math.cos(angleRad) * 10);
        int frontY = (int) (y + Math.sin(angleRad) * 10);
        return new Point(frontX, frontY);
    }

    //
    public static double normalizeAngle(double angle2) {
        double angle = angle2;
        while (angle >= 360) {
            angle -= 360;
        }
        while (angle < 0) {
            angle += 360;
        }
        return angle;

    }
}
