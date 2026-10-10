package com.graphics;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class CiudadIluminada extends Vehiculo{
    protected boolean noche = false;
    private boolean faros = false;
    private static final float FARO_X = 0.55f;
    private static final float FARO_Y = 0.68f;
    private static final float FARO_Z = 1.32f;
    private static final int MAX_FAROLAS = 32;
    private float[] posicionesLuz;
    private final List<float[]> farolas = new ArrayList<>(); // cada elemento: {x, z}

    public CiudadIluminada() {
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] != 0) {   // solo en manzanas (edificio o parque), no en calles
                    // Esquina noroeste de la manzana, sobre la acera
                    farolas.add(new float[] { centro(columna) - 4.25f, centro(fila) - 4.25f });
                }
            }
        }

        posicionesLuz = new float[farolas.size() * 3];
        for (int i = 0; i < farolas.size(); i++) {
            posicionesLuz[i * 3] = farolas.get(i)[0];
            posicionesLuz[i * 3 + 1] = 4.9f;
            posicionesLuz[i * 3 + 2] = farolas.get(i)[1];
        }
    }

    @Override 
    protected void tecla(int key) {
        super.tecla(key);
        if (key == GLFW_KEY_N) {
            noche = !noche;
        }
        
        if (key == GLFW_KEY_F) {
            faros = !faros;
        }
    }

    @Override 
    protected void prepararLuces() {
        super.prepararLuces();
        vector("uEmision", 0, 0, 0);
        if (noche) {
            vector("uDireccionSol", -0.3f, 1.0f, -0.4f);
            vector("uColorSol", 0.12f, 0.14f, 0.26f);
            vector("uAmbiente", 0.14f, 0.16f, 0.28f);
        } else {
            vector("uDireccionSol", 0.4f, 1.0f, 0.3f);
            vector("uColorSol", 0.70f, 0.70f, 0.70f);
            vector("uAmbiente", 0.30f, 0.30f, 0.30f);
        }
        int cantidad = noche ? Math.min(farolas.size(), MAX_FAROLAS) : 0;   // de día no hay farolas encendidas
        glUniform1i(uniform("uCantidadFarolas"), cantidad);
        glUniform3fv(uniform("uFarolas"), posicionesLuz);

        float seno = (float) Math.sin(angulo);
        float coseno = (float) Math.cos(angulo);
        float[] lados = { -FARO_X, FARO_X };
        float[] posFaros = new float[6];
        for (int i = 0; i < 2; i++) {
            posFaros[i * 3]     = autoX + coseno * lados[i] - seno * FARO_Z;
            posFaros[i * 3 + 1] = FARO_Y;
            posFaros[i * 3 + 2] = autoZ - seno * lados[i] - coseno * FARO_Z;
        }
        glUniform3fv(uniform("uPosFaro"), posFaros);
        vector("uDirFaro", -seno, -0.12f, -coseno);   // hacia delante y un poco hacia abajo
        glUniform1i(uniform("uFarosOn"), faros ? 1 : 0);
    }

    @Override
    protected void colorFondo() {
        if (noche) {
            glClearColor(0.01f, 0.02f, 0.06f, 1);   // cielo nocturno, casi negro azulado
        } else {
            super.colorFondo();                      // cielo de día: el original
        }
    }

    @Override
    protected String estadosExtras() {
        return " | " + (noche ? "Noche" : "Dia") + " | F: Faros " + (faros ? "ON" : "OFF");
    }

    @Override
    protected void escena() {
        super.escena();        // ciudad y auto
        dibujarFarolas();
    }

    private void dibujarFarolas() {
        for (float[] f : farolas) {
            caja(f[0], 2.5f, f[1], 0.25f, 4.4f, 0.25f, 0.25f, 0.25f, 0.28f);   // postes
        }

        if (noche) {
            vector("uEmision", 1.0f, 0.9f, 0.55f);   // de noche las bombillas brillan con su propio color
        }
        for (float[] f : farolas) {
            caja(f[0], 4.9f, f[1], 0.7f, 0.4f, 0.7f, 1.0f, 0.9f, 0.55f);   // bombillas
        }
        vector("uEmision", 0, 0, 0);   // volver a la normalidad
    }

    @Override
    protected String fragmentShader() {
        return """
            #version 330 core
            in vec3 vNormal;
            in vec3 vMundo; // Posición del píxel en el mundo.

            uniform vec3 uColor;
            uniform vec3 uDireccionSol;
            uniform vec3 uColorSol;
            uniform vec3 uAmbiente;
            uniform vec3 uEmision;
            uniform vec3 uFarolas[32]; // Posición de cada farola.
            uniform int uCantidadFarolas; // Cuántas están encendidas.
            uniform vec3 uPosFaro[2]; // Posición de cada faro.
            uniform vec3 uDirFaro; // Hacia dónde apunta el haz.
            uniform int uFarosOn; // 1 si están encendidos.
            uniform int uMapa;

            const float ALCANCE = 22.0; // Distancia (m) a partir de la cual una farola ya no ilumina.
            const vec3 COLOR_FAROLA = vec3(1.0, 0.82, 0.5) * 2.0; // Luz cálida e intensidad.
            const float ALCANCE_FARO = 38.0;
            const float COS_INTERIOR = 0.94; // Centro del haz: unos 20 grados.
            const float COS_EXTERIOR = 0.82; // Borde del haz: unos 35 grados.
            const vec3 COLOR_FARO = vec3(1.0, 0.95, 0.8) * 3.5;

            out vec4 color;

            void main() {
                if (uMapa == 1) {
                    color = vec4(uColor + uEmision, 1.0);   // minimapa: colores planos, sin luces
                    return;
                }
                vec3 normal = normalize(vNormal);
                float difusa = max(dot(normal, normalize(uDireccionSol)), 0.0);
                vec3 luz = uAmbiente + uColorSol * difusa;

                for (int i = 0; i < uCantidadFarolas; i++) {
                    vec3 haciaFarola = uFarolas[i] - vMundo; // Flecha del píxel a la farola.
                    float distancia = max(length(haciaFarola), 0.001);
                    float difusaFarola = max(dot(normal, haciaFarola / distancia), 0.0);
                    float atenuacion = clamp(1.0 - distancia / ALCANCE, 0.0, 1.0);
                    atenuacion = atenuacion * atenuacion; // Cae más rápido al alejarse.
                    luz += COLOR_FAROLA * difusaFarola * atenuacion;
                }

                if (uFarosOn == 1) {
                    vec3 ejeFaro = normalize(uDirFaro);
                    for (int i = 0; i < 2; i++) {
                        vec3 haciaFaro = uPosFaro[i] - vMundo; // Del píxel al faro.
                        float distancia = max(length(haciaFaro), 0.001);
                        vec3 direccion = haciaFaro / distancia;
                        float difusaFaro = max(dot(normal, direccion), 0.0);
                        float cosAngulo = dot(-direccion, ejeFaro); // Del faro al píxel, comparado con el eje.
                        float cono = smoothstep(COS_EXTERIOR, COS_INTERIOR, cosAngulo); // 0 fuera, 1 dentro, borde suave.
                        float atenuacion = clamp(1.0 - distancia / ALCANCE_FARO, 0.0, 1.0);
                        atenuacion = atenuacion * atenuacion;
                        luz += COLOR_FARO * difusaFaro * cono * atenuacion;
                    }
                }

                color = vec4(uColor * luz + uEmision, 1.0);
            }
        """;
    }

    public static void main(String[] args) {
        CiudadIluminada aplicacion = new CiudadIluminada();
        aplicacion.run();
    }
}
