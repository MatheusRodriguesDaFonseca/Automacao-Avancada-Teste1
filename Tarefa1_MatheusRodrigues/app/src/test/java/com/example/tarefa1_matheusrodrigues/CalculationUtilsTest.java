package com.example.tarefa1_matheusrodrigues;

import com.example.racelibrary.CalculationUtils;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import java.util.List;

import org.junit.Test;


public class CalculationUtilsTest {

    /** * Este teste verifica se o método generateRandomSpeed da classe CalculationUtils
     * gera uma velocidade dentro do intervalo esperado de 20 a 30.
     */
    @Test
    public void testGenerateRandomSpeed() {
        double speed = CalculationUtils.generateRandomSpeed();
        assertTrue("A velocidade deve estar entre 20 e 30", speed >= 20 && speed <= 30);
    }

    /** * Este teste verifica se o método getFrontPosition da classe CalculationUtils calcula
      * corretamente a posição frontal do carro, dado um ponto (x, y) e um ângulo.
      */
    @Test
    public void testGetFrontPosition() {
        double x = 100;
        double y = 100;
        double angle = 0; // 0 graus, aponta para a direita Point frontPosition = CalculationUtils.getFrontPosition(x, y, angle); assertEquals("A posição frontal do carro deve ser calculada corretamente", new Point(110, 100), frontPosition);
    }

    /** Este teste verifica se o método normalizeAngle da classe CalculationUtils
     * * normaliza um ângulo para estar no intervalo de 0 a 360 graus.
     */
    @Test public void testNormalizeAngle() {
        // Ângulo maior que 360
        double angle1 = 370;
        double normalized1 = CalculationUtils.normalizeAngle(angle1);
        assertEquals("O ângulo deve ser normalizado para 10 graus", 10.0, normalized1, 0.001);

        // Ângulo negativo
        double angle2 = -30;
        double normalized2 = CalculationUtils.normalizeAngle(angle2);
        assertEquals("O ângulo deve ser normalizado para 330 graus", 330.0, normalized2, 0.001);

        // Ângulo dentro do intervalo
        double angle3 = 45;
        double normalized3 = CalculationUtils.normalizeAngle(angle3);
        assertEquals("O ângulo deve ser 45 graus", 45.0, normalized3, 0.001);

    }

}

