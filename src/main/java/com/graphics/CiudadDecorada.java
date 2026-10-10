package com.graphics;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class CiudadDecorada extends CiudadIluminada {
    private final List<float[]> destinos = new ArrayList<>();   // cada uno: {x, z}
    private int entregaActual = 0;                               // cuántas se llevan completadas
    private float tiempoTotal = 0;

    private static final float SUELO_PARQUE = 0.37f;
    private static final int ROJO = 0, AMARILLO = 1, VERDE = 2;   // posición de cada luz, de arriba hacia abajo
    private static final float CICLO = 12;                         // segundos de una secuencia completa
    private static final float[][] COLORES = {
        {1.0f, 0.1f, 0.1f},    // rojo
        {1.0f, 0.75f, 0.1f},   // amarillo
        {0.1f, 0.9f, 0.2f}     // verde
    };

    private float tiempoSemaforo = 0;
    private boolean mostrarMapa = true;

    public CiudadDecorada() {
        for (int fila = MAPA.length - 1; fila >= 0; fila--) {    // de sur a norte
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 2) {                  // parque
                    destinos.add(new float[] { centro(columna) - 10, centro(fila) });   // calle vecina del oeste
                }
            }
        }
    }

    @Override
    protected void escena() {
        super.escena();     // ciudad, auto y farolas
        if (!vistaMapa) {
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        dibujarDestino();
    }

    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }

        if (key == GLFW_KEY_R) {
            entregaActual = 0;                                       // se puede completar toda la actividad otra vez
        }
    }

    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime);                       // conserva la conducción
        tiempoSemaforo = (tiempoSemaforo + deltaTime) % CICLO;
        tiempoTotal += deltaTime;
        comprobarEntrega();
    }

    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];

                if (tipo == 2) {                                          // parque
                    dibujarParque(x, z);
                }

                if (tipo == 1) {                                          // edificio
                    dibujarVentanas(x, z, alturaEdificio(fila, columna));
                }

                if (fila % 4 == 1 && columna % 4 == 1) {            // índices 1, 5 y 9 (siempre son manzanas)
                    dibujarSemaforo(x - 4.25f, z + 4.25f);          // esquina suroeste, sobre la acera
                    pasoPeatonal(x - 10, z + 4); 
                }
            }
        }
    }

    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) {
            vector("uEmision", 1.0f, 0.85f, 0.5f);       // de noche las ventanas brillan
        }
        for (float y = 2; y < altura - 0.5f; y += 3) {
            for (float d = -2; d <= 2; d += 2) {
                caja(x + d, y, z - 3.52f, 1.2f, 1.2f, 0.06f, 0.15f, 0.27f, 0.38f);   // norte
                caja(x + d, y, z + 3.52f, 1.2f, 1.2f, 0.06f, 0.15f, 0.27f, 0.38f);   // sur
                caja(x - 3.52f, y, z + d, 0.06f, 1.2f, 1.2f, 0.15f, 0.27f, 0.38f);   // oeste
                caja(x + 3.52f, y, z + d, 0.06f, 1.2f, 1.2f, 0.15f, 0.27f, 0.38f);   // este
            }
        }
        vector("uEmision", 0, 0, 0);                     // restablece la emisión
    }

   private void pasoPeatonal(float x, float z) {
        for (int d = -3; d <= 3; d++) {                           // siete franjas, una cada metro
            caja(x + d, 0.045f, z, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f);
        }
    }

    private void dibujarParque(float x, float z) {
        arbol(x + 2.5f, z - 2.5f);
        arbol(x - 2.5f, z + 2.5f);
        arbol(x + 2.5f, z + 2.5f);
        banco(x, z);
    }

    private void arbol(float x, float z) {
        caja(x, SUELO_PARQUE + 1.0f, z, 0.5f, 2.0f, 0.5f, 0.35f, 0.22f, 0.12f);   // tronco: de 0 a 2 m
        caja(x, SUELO_PARQUE + 3.0f, z, 3.0f, 2.0f, 3.0f, 0.13f, 0.42f, 0.17f);   // copa grande: de 2 a 4 m
        caja(x, SUELO_PARQUE + 4.6f, z, 1.8f, 1.2f, 1.8f, 0.18f, 0.50f, 0.22f);   // copa chica: de 4 a 5.2 m
    }

    private void banco(float x, float z) {
        caja(x, SUELO_PARQUE + 0.4f, z, 0.5f, 0.1f, 1.6f, 0.45f, 0.30f, 0.15f);          // asiento
        caja(x + 0.21f, SUELO_PARQUE + 0.7f, z, 0.08f, 0.5f, 1.6f, 0.45f, 0.30f, 0.15f);  // respaldo, en el lado este
        caja(x, SUELO_PARQUE + 0.175f, z - 0.65f, 0.4f, 0.35f, 0.1f, 0.2f, 0.2f, 0.22f);  // pata norte
        caja(x, SUELO_PARQUE + 0.175f, z + 0.65f, 0.4f, 0.35f, 0.1f, 0.2f, 0.2f, 0.22f);  // pata sur
    }

    private int estadoSemaforo() {
        if (tiempoSemaforo < 5) return VERDE;       // 5 s en verde
        if (tiempoSemaforo < 6) return AMARILLO;    // 1 s en amarillo
        return ROJO;                                // 6 s en rojo
    }

    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.2f, 2.8f, 0.2f, 0.2f, 0.2f, 0.22f);        // poste: de 0.3 a 3.1 m
        caja(x, 3.7f, z, 0.5f, 1.2f, 0.5f, 0.08f, 0.08f, 0.08f);      // cuerpo: de 3.1 a 4.3 m

        int encendida = estadoSemaforo();
        for (int luz = 0; luz < 3; luz++) {
            float[] c = COLORES[luz];
            float brillo = (luz == encendida) ? 1 : 0.15f;             // las apagadas, en versión oscura
            if (luz == encendida) {
                vector("uEmision", c[0], c[1], c[2]);                  // la encendida brilla con luz propia
            }
            for (int lado = -1; lado <= 1; lado += 2) {                // cara norte (-z) y cara sur (+z)
                caja(x, 4.05f - luz * 0.35f, z + lado * 0.26f, 0.28f, 0.28f, 0.06f,
                    c[0] * brillo, c[1] * brillo, c[2] * brillo);
            }
            vector("uEmision", 0, 0, 0);
        }
    }

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame();                  // primero la vista principal
        if (mostrarMapa) {
            dibujarMinimapa();
        }
    }

    private void dibujarMinimapa() {
        int menor = Math.min(ancho, alto);
        int lado = Math.min(260, menor / 3);
        int margen = Math.min(18, menor / 20);
        int x = ancho - lado - margen;         // esquina superior derecha
        int y = alto - lado - margen;

        glEnable(GL_SCISSOR_TEST);
        try {
            rellenarRecuadro(x - 3, y - 3, lado + 2 * 3, 0.80f, 0.87f, 0.94f);   // marco
            rellenarRecuadro(x, y, lado, 0.06f, 0.10f, 0.15f);                                              // fondo
            glViewport(x, y, lado, lado);
            dibujarCiudadDesdeArriba();
        } finally {
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);     // deja la pantalla como la espera el siguiente cuadro
        }

        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar el minimapa");
        }
    }

    private void rellenarRecuadro(int x, int y, int lado, float r, float g, float b) {
        glScissor(x, y, lado, lado);           // solo estos píxeles pueden cambiar
        glClearColor(r, g, b, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void dibujarCiudadDesdeArriba() {
        entero("uMapa", 1);                    // el vertex shader proyecta desde arriba
        decimal("uAlcanceMapa", LIMITE + 2);   // la ciudad entera más 2 m de margen
        vistaMapa = true;                      // las demás clases omiten los detalles pequeños
        try {
            escena();
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);                // vuelve a la perspectiva
        }
    }

    @Override
    protected String estadosExtras() {
        String estado = entregaActual >= destinos.size() ? " COMPLETADO (R: reiniciar)" : "";
        return super.estadosExtras() + " | Entregas: " + entregaActual + "/" + destinos.size() + estado;
    }

    private void comprobarEntrega() {
        if (entregaActual >= destinos.size()) {
            return;                                              // ya se completaron todas
        }
        float[] d = destinos.get(entregaActual);
        if (Math.abs(autoX - d[0]) < 2.5f && Math.abs(autoZ - d[1]) < 2.5f) {   // el auto está dentro de la plataforma
            entregaActual++;
        }
    }

    private void dibujarDestino() {
        if (entregaActual >= destinos.size()) {
            return;                                              // no queda destino activo
        }
        float[] d = destinos.get(entregaActual);
        vector("uEmision", 0.2f, 1.0f, 0.6f);                   // todo lo que sigue brilla con luz propia
        caja(d[0], 0.07f, d[1], 5, 0.04f, 5, 0.2f, 1.0f, 0.6f);            // plataforma sobre el asfalto
        float flotar = 0.4f * (float) Math.sin(tiempoTotal * 2);
        cajaGirada(d[0], 3.5f + flotar, d[1], 1.4f, 1.4f, 1.4f, 0.2f, 1.0f, 0.6f, tiempoTotal);   // cubo que sube, baja y gira
        vector("uEmision", 0, 0, 0);
    }

    public static void main(String[] args) {
        CiudadDecorada aplicacion = new CiudadDecorada();
        aplicacion.run();
    }
}
