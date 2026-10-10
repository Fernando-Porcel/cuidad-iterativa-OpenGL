package com.graphics;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class AppCuidad {
    protected long ventana;
    private int programa;
    private int vao;
    private int vbo;
    protected int ancho = 1100;
    protected int alto = 760;

    //Angulo Inicial de la camara de la cuidad
    private float orbita = 0.6f;

    // Cambiar el angulo a la vista del mapa
    protected boolean vistaMapa = false;

    // Ancho y profundidad de cada celda del mapa
    private static final float CELDA = 10;

    private final Map<String, Integer> uniforms = new HashMap<>();

    protected static final int[][] MAPA = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    // Distancia del origen a cada borde
    protected static final float LIMITE = MAPA.length * CELDA / 2;

    // Organizar las tres fases de la aplicacion y limpiar en caso ocurra un error
    public void run() {
        // Preparamos mensajes de error en la consola
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        // Instamos el manejador de errores en GLFW
        errores.set();
        try {
            iniciar();
            loop();
        } finally {
            limpiar();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }

    private void iniciar() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Reestablece todas las opciones por defecto
        GLFW.glfwDefaultWindowHints();

        // Ocultar la ventana al principio: la mostsrar recien cuando OpenGL este listo
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        // Permitir que el usuario pueda redimensionar la pantalla
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        // Queremos que use la version de OpenGL 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        // Solo funciones modernos de OpenGL (evitar compatibilidad con codigo muy antiguo)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // MacOS hay que reativar para usar el core profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Creamos la ventana: ancho, alto, titulo, monitor = ventana normal, ventana para compartir
        ventana = GLFW.glfwCreateWindow(ancho, alto, "Ciudad | Flachas: orbitar | Esc: Salir", 0, 0);
    
        // Decir que todas las ordenes de OPENGL que ejecutamos a partir de ahora
        // se aplique a esta ventana
        GLFW.glfwMakeContextCurrent(ventana);

        // Sincronizar el dibujo con el monitor (1 = esperar al siguiente refresco)
        // Evitamos el "tearing" (imagen partida)
        GLFW.glfwSwapInterval(1);
        // hacer visible la ventana
        GLFW.glfwShowWindow(ventana);

        // Cargar en java las funciones de OpenGL
        // LWJGL necesita este paso para enlazar las llamados de Java con la GPU
        GL.createCapabilities();

        // Hace que las superficie cercana oculten a las lejanas
        glEnable(GL_DEPTH_TEST);

        // Registramos la funcion del evento del teclado para cerrar la ventana
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                tecla(key);
            }
        });

        crearPrograma();
        crearCubo();
    }

    private void loop() {
        // Guardamos el instante inicial para calcular el tiempo entre cuadros
        double tiempoAnterior = glfwGetTime();
        // Cuenta los cuadros dibujados para la comprobacion
        int cuadros = 0;
        // Cero permite jugar: otro valor limita el arranque de prueba
        int maxCuadros = Integer.getInteger("demo.frames", 0);
        // Reserva espacio para que GLFW escriba el ancho en pixeles
        int[] anchoReal = new int[1];
        // Reserva espacio para que GLFW escriba el alto en pixeles
        int[] altoReal = new int[1];

        while(!glfwWindowShouldClose(ventana)) {
            // Procesa el teclado, redimensionamiento y boton de cierre
            glfwPollEvents();
            // Counsulta el tiempo actual en segundos
            double tiempoActual = glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;
            actualizar(deltaTime);
            glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0 && alto > 0) {
                dibujarFrame();
            }
            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    private void limpiar() {
        if (programa != 0) {
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (ventana != 0) {
            Callbacks.glfwFreeCallbacks(ventana);
            glfwDestroyWindow(ventana);
        }
        glfwTerminate();
    }

    // ============== Teclas y Camaras ===============

    /* Atiende las acciones de una sola pulsacion */
    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(ventana, true);
        }
    }

    protected boolean pulsado(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    protected void actualizar(float deltaTime) {
        if (pulsado(GLFW_KEY_LEFT)) {
            orbita -= deltaTime;
        }

        if (pulsado(GLFW_KEY_RIGHT)) {
            orbita += deltaTime;
        }
    }

    protected void configurarCamara() {
        float radio = LIMITE * 1.85f;
        float altura = LIMITE * 1.57f;
        float camaraX = (float) Math.sin(orbita) * radio;
        float camaraZ = (float) Math.cos(orbita) * radio;
        vector("uOjo", camaraX, altura, camaraZ);
        vector("uObjetivo", 0, 0, 0);
        decimal("uAspecto", (float) ancho/alto);
    }

    protected void prepararLuces() {
        // Enviar las luces al GPU
    }

    // ============== Cuidad a partir de la matriz ===============
    protected float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    /* Color con el que se borra la pantalla en cada cuadro: es el "cielo" */
    protected void colorFondo() {
        glClearColor(0.12f, 0.20f, 0.30f, 1);
    }

    protected void dibujarFrame() {
        glViewport(0, 0, ancho, alto);
        colorFondo();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0);
        configurarCamara();
        prepararLuces();
        escena();

        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar");
        }
    }

    protected float alturaEdificio(int fila, int columna) {
        return 5 + (fila * 3 + columna * 7) % 9;
    }

    protected void escena() {
        caja(0, -0.25f, 0, LIMITE * 2, 0.5f, LIMITE * 2, 0.16f, 0.19f, 0.23f);
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 0) {
                    dibujarMarcasCalle(fila, columna, x, z);
                } else {
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f);
                    if (tipo == 1) {
                        float altura = alturaEdificio(fila, columna);
                        float rojo = 0.28f + columna * 0.045f;
                        float azul = 0.48f + fila * 0.025f;
                        caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.40f, azul);
                        caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.26f, 0.32f);
                    } else {
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f);
                    }
                }
            }
        }
    }

    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        float grosor = vistaMapa ? 0.5f : 0.13f;   // en el minimapa, 0.13 m es menos de un píxel
        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, grosor, 1, 0.84f, 0.35f);
            }
        }

        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x, 0.025f, z + desplazamiento, grosor, 0.03f, 1.6f, 1, 0.84f, 0.35f);
            }
        }
    }

    // ============== Dibujar cajas y enviar uniform ===============
    protected void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        //cajaGirada();
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    protected void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        decimal("uGiro", angulo);
        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    protected int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) {
            int ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre, ubicacion);
        }
        return uniforms.get(nombre);
    }

    protected void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    protected void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    protected void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }

    // ============== Shaders: Codigo que se ejecuta en la GPU ===============

    private String vertexShader() {
        return """
            #version 330 core // Selecciona GLSL 3.30 para el contexto OpenGL 3.3.
            layout (location = 0) in vec3 aPos; // Lee la posición local del vértice desde el VBO.
            layout (location = 1) in vec3 aNormal; // Lee la normal de la cara desde el mismo VBO.
            uniform vec3 uPos; // Recibe el centro de la caja en la ciudad.
            uniform vec3 uEscala; // Recibe el tamaño de la caja en cada eje.
            uniform vec3 uOjo; // Recibe la posición de la cámara.
            uniform vec3 uObjetivo; // Recibe el punto que observa la cámara.
            uniform float uGiro; // Recibe el giro del objeto alrededor de Y.
            uniform float uAspecto; // Recibe la relación ancho/alto de la imagen.
            uniform int uMapa; // Selecciona perspectiva (0) o vista superior ortográfica (1).
            out vec3 vMundo; // Envía la posición mundial al shader de fragmentos.
            out vec3 vNormal; // Envía la normal transformada para la iluminación.

            void main() { // OpenGL ejecuta este bloque una vez por vértice.
                float coseno = cos(uGiro); // Calcula el coseno del giro del objeto.
                float seno = sin(uGiro); // Calcula el seno del mismo giro.
                mat3 giro = mat3( // Construye la matriz de rotación; GLSL recibe sus columnas.
                    coseno, 0.0, -seno, // Primera columna: dirección del eje X rotado.
                    0.0, 1.0, 0.0, // Segunda columna: Y permanece vertical.
                    seno, 0.0, coseno // Tercera columna: dirección del eje Z rotado.
                ); // Completa la matriz de tres filas y tres columnas.
                vMundo = giro * (aPos * uEscala) + uPos; // Escala, gira y traslada el vértice al mundo.
                vNormal = normalize(giro * (aNormal / uEscala)); // Corrige la normal con la inversa transpuesta de escala y giro.

                if (uMapa == 1) { // Esta rama se usa al dibujar el minimapa en clase4.
                    float pantallaX = vMundo.x / 57.0; // Ajusta el ancho del mundo al intervalo visible -1 a 1.
                    float pantallaY = -vMundo.z / 57.0; // Coloca el norte (-Z) en la parte superior del mapa.
                    float profundidad = -vMundo.y / 100.0; // Hace que los objetos más altos se vean por encima.
                    gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0); // Proyecta sin reducir objetos lejanos.
                } else { // La escena principal utiliza una cámara con perspectiva.
                    vec3 frente = normalize(uObjetivo - uOjo); // Calcula la dirección hacia la que mira la cámara.
                    vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Obtiene el eje horizontal de la cámara.
                    vec3 arriba = cross(derecha, frente); // Obtiene el eje vertical perpendicular a los otros dos.
                    vec3 diferencia = vMundo - uOjo; // Traslada el origen del mundo hasta la cámara.
                    float vistaX = dot(diferencia, derecha); // Mide cuánto está el vértice a la derecha de la cámara.
                    float vistaY = dot(diferencia, arriba); // Mide cuánto está el vértice encima de la cámara.
                    float vistaZ = -dot(diferencia, frente); // Usa Z negativa delante de la cámara, como espera OpenGL.
                    float factor = 1.0 / tan(radians(55.0) * 0.5); // Convierte el campo visual de 55 grados en escala de perspectiva.
                    float cerca = 0.1; // Define la distancia mínima visible.
                    float lejos = 250.0; // Define la distancia máxima visible.
                    float clipX = vistaX * factor / uAspecto; // Corrige la coordenada horizontal según el ancho de la pantalla.
                    float clipY = vistaY * factor; // Aplica el campo visual a la coordenada vertical.
                    float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ; // Calcula el término de profundidad dependiente de Z.
                    clipZ += 2.0 * lejos * cerca / (cerca - lejos); // Añade el término constante de profundidad.
                    gl_Position = vec4(clipX, clipY, clipZ, -vistaZ); // OpenGL dividirá XYZ por W para producir perspectiva.
                }
            }
        """;
    }

    protected String fragmentShader() {
        return """
            #version 330 core // Indica la versión del lenguaje del shader.
            uniform vec3 uColor; // Recibe el color RGB enviado por cajaGirada().
            out vec4 color; // Define el color final que se escribe en la imagen.
            void main() { // Se ejecuta para cada fragmento de la geometría dibujada.
                color = vec4(uColor, 1.0); // Copia el material y establece opacidad completa.
            }
        """; // Termina la cadena del shader de fragmentos.
    }

    /** Compila un shader y comunica el mensaje del controlador si hay un error GLSL. */
    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo); // Reserva un shader del tipo vértice o fragmento.
        glShaderSource(shader, fuente); // Entrega a OpenGL el texto GLSL.
        glCompileShader(shader); // Solicita la compilación para esta GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Consulta si la compilación falló.
            String error = glGetShaderInfoLog(shader); // Recupera el detalle del error antes de borrar el objeto.
            glDeleteShader(shader); // Libera el shader que no pudo compilarse.
            throw new IllegalStateException(error); // Detiene la aplicación con el mensaje de diagnóstico.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Une los shaders en un programa ejecutable y libera los objetos intermedios. */
    private void crearPrograma() {
        int shaderVertices = compilar(GL_VERTEX_SHADER, vertexShader()); // Compila las transformaciones geométricas.
        int shaderFragmentos = 0; // Permite saber si el segundo shader llegó a crearse.
        try { // Garantiza que ambos objetos intermedios se liberen incluso ante un fallo.
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, fragmentShader()); // Compila el color de la etapa actual.
            programa = glCreateProgram(); // Crea el contenedor que enlazará ambos shaders.
            glAttachShader(programa, shaderVertices); // Adjunta el shader que calcula posiciones.
            glAttachShader(programa, shaderFragmentos); // Adjunta el shader que calcula colores.
            glLinkProgram(programa); // Enlaza las entradas y salidas de ambos shaders.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) { // Comprueba que el enlace haya sido válido.
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Muestra incompatibilidades del programa.
            }
        } finally { // Ya no hacen falta objetos shader separados después del enlace.
            glDeleteShader(shaderVertices); // Libera el objeto del shader de vértices.
            if (shaderFragmentos != 0) { // Comprueba que el segundo objeto exista.
                glDeleteShader(shaderFragmentos); // Libera el objeto del shader de fragmentos.
            }
        }
    }

    // ==================== 7. GEOMETRÍA DEL CUBO: VAO Y VBO ====================

    /** Crea un cubo explícito, con posición XYZ y normal XYZ por vértice, como en los ejemplos. */
    private void crearCubo() {
        float[] vertices = { // Cada línea describe un vértice: X, Y, Z, normalX, normalY, normalZ.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior izquierda, normal hacia +Z.
            0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior derecha.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior derecha; termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: repite la esquina para el segundo triángulo.
            -0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior izquierda.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: cierra el segundo triángulo.

            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: primer vértice, normal hacia -Z.
            -0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: segundo vértice.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: termina el primer triángulo.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: inicia el segundo triángulo.
            0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: esquina inferior derecha.
            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: cierra la cara.

            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: primer vértice, normal hacia -X.
            -0.5f, 0.5f, -0.5f, -1, 0, 0, // Cara izquierda: segundo vértice.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: termina el primer triángulo.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, -1, 0, 0, // Cara izquierda: esquina inferior frontal.
            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: cierra la cara.

            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: primer vértice, normal hacia +X.
            0.5f, -0.5f, 0.5f, 1, 0, 0, // Cara derecha: segundo vértice.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: termina el primer triángulo.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 1, 0, 0, // Cara derecha: esquina superior trasera.
            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: cierra la cara.

            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: primer vértice, normal hacia +Y.
            -0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: segundo vértice.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: esquina trasera derecha.
            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: cierra la cara.

            -0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: primer vértice, normal hacia -Y.
            0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: segundo vértice.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: termina el primer triángulo.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: esquina frontal izquierda.
            -0.5f, -0.5f, -0.5f, 0, -1, 0 // Cara inferior: cierra los 36 vértices del cubo.
        };
        vao = glGenVertexArrays(); // Reserva el objeto que recordará la disposición de los atributos.
        glBindVertexArray(vao); // Activa ese VAO para configurarlo.
        vbo = glGenBuffers(); // Reserva el buffer que almacenará los vértices.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Selecciona el VBO como destino de datos de vértices.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Copia los datos a la GPU; el cubo no cambiará.
        int bytesPorVertice = 6 * Float.BYTES; // Cada vértice contiene seis float de cuatro bytes.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L); // Describe XYZ desde el primer byte del vértice.
        glEnableVertexAttribArray(0); // Habilita la posición que recibe aPos.
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3L * Float.BYTES); // Describe la normal después de XYZ.
        glEnableVertexAttribArray(1); // Habilita la normal que recibe aNormal.
        glBindBuffer(GL_ARRAY_BUFFER, 0); // Deja de seleccionar el VBO una vez configurado.
        glBindVertexArray(0); // Finaliza la configuración del VAO.
    }

    /** Punto de entrada para ejecutar la primera lección. */
    public static void main(String[] args) {
        AppCuidad aplicacion = new AppCuidad(); // Crea la aplicación que muestra únicamente la ciudad.
        aplicacion.run(); // Inicia la ventana, el ciclo de dibujo y la limpieza final.
    }
}

