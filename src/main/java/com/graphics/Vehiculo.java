package com.graphics;

import static org.lwjgl.glfw.GLFW.*;

public class Vehiculo extends AppCuidad{
    protected float autoX;
    protected float autoZ;
    protected float angulo;
    private float velocidad;
    private boolean camaraArea = false;
    private boolean primeraPersona = false;
    private static final float RADIO_AUTO = 1.65f;

    //---------- TECLADO ------------
    @Override 
    protected void tecla(int key) {
        super.tecla(key);

        if (key == GLFW_KEY_C) {
            camaraArea = !camaraArea;
        }

        if (key == GLFW_KEY_R) {
            reiniciar();
        }

        if (key == GLFW_KEY_V) {
            primeraPersona = !primeraPersona;
        }
    }

    public Vehiculo() {
        reiniciar();
    }

    private void reiniciar() {
        autoX = centro(0);
        autoZ = centro(MAPA.length - 1);
        angulo = 0;
        velocidad = 0;
    } 

    //---------- CONDUCCION -----------
    @Override 
    protected void actualizar(float deltaTime) {
        float acelerador = 0;

        if (pulsado(GLFW_KEY_W) || pulsado(GLFW_KEY_UP)) {
            acelerador += 1;
        }

        if (pulsado(GLFW_KEY_S) || pulsado(GLFW_KEY_DOWN)) {
            acelerador -= 1;
        }

        velocidad += acelerador * 9 * deltaTime;
        float resistencia = 0.7f;

        if (pulsado(GLFW_KEY_SPACE)) {
            resistencia = 7;
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime);
        velocidad *= factorFrenado;
        velocidad = Math.max(-6, Math.min(16, velocidad));
        float direccion = 0;

        if (pulsado(GLFW_KEY_A) || pulsado(GLFW_KEY_LEFT)) {
            direccion += 1;
        }

        if ((pulsado(GLFW_KEY_D)) || pulsado(GLFW_KEY_RIGHT)) {
            direccion -= 1;
        }

        angulo += direccion * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float siguienteX = autoX + frenteX * velocidad * deltaTime;
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime;
        
        if (puedeCircular(siguienteX, siguienteZ)) {
            autoX = siguienteX;
            autoZ = siguienteZ;
        } else {
            velocidad = 0;
        }
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f);
        String titulo = getClass().getSimpleName();
        titulo += " | WASD/flechas: conducir | Espacio: freno";
        titulo += " | C: Camara | R: Reiniciar ";
        titulo += kilometrosPorHora + " Km/h";
        titulo += " | C: Camara aerea | V: 1a/3a persona | R: Reiniciar ";
        titulo += estadosExtras();
        glfwSetWindowTitle(ventana, titulo);
    }

    protected String estadosExtras() {
        return "";
    }

    //--------- Colisiones con la cuidad ---------
    private boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO;

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }

        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) {
                    continue;
                }

                float centroX = centro(columna);
                float centroZ = centro(fila);
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5));
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5));
                float distanciaX = x - cercaX;
                float distanciaZ = z - cercaZ;
                float distaciaCuadrado = distanciaX * distanciaX + distanciaZ * distanciaZ;

                if (distaciaCuadrado < RADIO_AUTO * RADIO_AUTO) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void configurarCamara() {
        if (camaraArea) {
            super.configurarCamara();
            return;
        }

        if (primeraPersona) {
            float frenteX = -(float) Math.sin(angulo);                  // hacia dónde mira el auto
            float frenteZ = -(float) Math.cos(angulo);
            float ojoX = autoX + frenteX * 0.6f;                        // 0.6 m por delante del centro del auto
            float ojoZ = autoZ + frenteZ * 0.6f;
            vector("uOjo", ojoX, 1.3f, ojoZ);
            vector("uObjetivo", ojoX + frenteX * 10, 1.0f, ojoZ + frenteZ * 10);   // mira 10 m hacia delante
            decimal("uAspecto", (float) ancho / alto);
            return;
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12;
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12;

        vector("uOjo", camaraX, 9, camaraZ);
        vector("uObjetivo", autoX, 0.8f, autoZ);
        decimal("uAspecto", (float) ancho / alto);
    }

    //-------- Dibujo del auto ------------
    @Override 
    protected void escena() {
        super.escena();
        dibujarAuto();
    }

    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f);
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f);
        float[] ladosRueda = { -0.88f, 0.88f };
        float[] ejesRueda = { -0.82f, 0.82f };

        for (float x: ladosRueda) {
            for (float z : ejesRueda) {
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.65f, 0.08f);
            }
        }

        float[] ladosFaros = { -0.55f, 0.55f};
        for (float x: ladosFaros) {
            pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f);
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f);
        }
    }

    private void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        float mundoX = autoX + coseno * x + seno * z;
        float mundoZ = autoZ - seno * x + coseno * z;
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo);
    }

    public static void main(String[] args) {
        Vehiculo aplicacion = new Vehiculo(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}

