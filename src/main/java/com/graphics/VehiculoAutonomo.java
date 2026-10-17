package com.graphics;

public class VehiculoAutonomo {
    private static final float RADIO_LLEGADA = 3;    // m: a esta distancia se considera alcanzado un punto
    private static final float GIRO_MAXIMO = 2.2f;   // rad/s: lo más rápido que puede girar

    float x, z;                  // posición en el mundo
    float angulo;                // orientación (0 mira hacia -Z, igual que el auto del jugador)
    final float velocidad;       // m/s, constante
    final float r, g, b;         // color

    private final float[][] ruta;   // puntos {x, z} que recorre, en orden y en bucle
    private int objetivo = 1;       // índice del punto al que se dirige

    VehiculoAutonomo(float[][] ruta, float velocidad, float r, float g, float b) {
        this.ruta = ruta;
        this.velocidad = velocidad;
        this.r = r;
        this.g = g;
        this.b = b;
        x = ruta[0][0];
        z = ruta[0][1];
        angulo = anguloHacia(ruta[1][0] - x, ruta[1][1] - z);    // arranca mirando al segundo punto
    }

    void actualizar(float deltaTime) {
        float dx = ruta[objetivo][0] - x;
        float dz = ruta[objetivo][1] - z;
        if (dx * dx + dz * dz < RADIO_LLEGADA * RADIO_LLEGADA) {
            objetivo = (objetivo + 1) % ruta.length;             // llegó: sigue con el próximo (y vuelve al primero)
            dx = ruta[objetivo][0] - x;
            dz = ruta[objetivo][1] - z;
        }
        float falta = diferencia(anguloHacia(dx, dz), angulo);   // cuánto le falta girar
        float giroMaximo = GIRO_MAXIMO * deltaTime;
        angulo += Math.max(-giroMaximo, Math.min(giroMaximo, falta));
        x -= (float) Math.sin(angulo) * velocidad * deltaTime;   // avanza hacia donde mira
        z -= (float) Math.cos(angulo) * velocidad * deltaTime;
    }

    private static float anguloHacia(float dx, float dz) {
        return (float) Math.atan2(-dx, -dz);                     // el frente del auto es -Z
    }

    private static float diferencia(float meta, float actual) {
        return (float) Math.atan2(Math.sin(meta - actual), Math.cos(meta - actual));   // entre -π y π
    }
}