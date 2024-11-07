

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isPlaying;
    private Paint paint;
    private float angle = 0; // Controla a posição do carro
    private int radius = 100; // Raio da pista circular
    private int centerX; // Centro da pista
    private int centerY; // Centro da pista
    private int numberOfCars = 2; // Número de carros

    public GameView(Context context) {
        super(context);
        paint = new Paint();
        // Defina o centro da pista
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            control();
        }
    }

    private void update() {
        angle += 0.05; // Aumenta o ângulo para mover o carro
        if (angle >= 2 * Math.PI) {
            angle = 0; // Reinicie o ângulo se ultrapassar 360 graus
        }
    }

    private void draw() {
        Canvas canvas = getHolder().lockCanvas();
        canvas.drawColor(Color.BLACK); // Limpa a tela

        // Desenhe a pista (circular)
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius, paint); // Desenhar a pista

        // Desenhe os carros em posições circulares
        for (int i = 0; i < numberOfCars; i++) {
            float carX = centerX + radius * (float) Math.cos(angle + (i * Math.PI / numberOfCars));
            float carY = centerY + radius * (float) Math.sin(angle + (i * Math.PI / numberOfCars));

            // Desenhar o carro (substitua com a imagem do carro)
            paint.setColor(Color.RED);
            canvas.drawRect(carX - 10, carY - 5, carX + 10, carY + 5, paint); // Carro como um retângulo
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void control() {
        try {
            thread.sleep(17); // Controle de FPS
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

