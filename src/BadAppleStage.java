import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.gl2.GLUT;
import static com.jogamp.opengl.GL2.*; // Importar constantes de GL2

//Meter la canción
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

public class BadAppleStage extends GLJPanel implements GLEventListener, KeyListener, MouseMotionListener {

    private GLUT glut = new GLUT();
    // --- CONFIGURACIÓN ---
    private static final String TITLE = "Bad Apple Stage 3D";
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    private static final int FPS = 30; // 30 FPS para coincidir con el video

    // --- VARIABLES DE CÁMARA (Primera Persona) ---
    private float camX = 0.0f;
    private float camY = 1.5f; // Altura de los ojos
    private float camZ = 10.0f;
    private float camYaw = -90.0f; // Rotación horizontal (mirando hacia -Z)
    private float camPitch = 0.0f; // Rotación vertical
    
    // VARIABLES PARA EL MOUSE (para que no se salga de la ventana)
    private java.awt.Robot robot;
    private boolean mouseLocked = true; // Empieza bloqueado para jugar directo
    
    // Auxiliar para el mouse
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    // --- VARIABLES DE ANIMACIÓN ---
    private int frameActual = 1;
    private int totalFrames = 6572; // TOTAL DE IMAGENES DE BAD APPLE
    private String rutaImagenesVideo = "imagenes/"; // CARPETA DONDE ESTÁN LOS FRAMES
    private String prefijoImagen = "output_"; // Ejemplo: output_0001.png
    private String extensionImagen = ".jpg";  // .jpg según los archivos
    
    // --- VARIABLES DE AUDIO ---
    private Clip clipAudio;
    private long tiempoInicio = 0; // Guardará el momento exacto en que inicia la canción

    // --- OBJETOS DE OPENGL ---
    private GLU glu;
    private Texture texturaPantalla;
    private Texture texturaPiso;
    private Texture texturaMetal;
    private Texture texturaLuna;
    private Texture texturaMarmol;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GLJPanel canvas = new BadAppleStage();
            canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
            final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);

            JFrame frame = new JFrame();
            frame.getContentPane().add(canvas);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    new Thread(() -> {
                        if (animator.isStarted()) animator.stop();
                        System.exit(0);
                    }).start();
                }
            });

            frame.setTitle(TITLE);
            frame.pack();
            frame.setVisible(true);
            animator.start();
        });
    }

    public BadAppleStage() {
        this.addGLEventListener(this);
        this.addKeyListener(this);
        this.addMouseMotionListener(this);
        // Ocultar cursor opcionalmente 
        this.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));
        
        // --- INICIALIZAR ROBOT ---
        try {
            robot = new java.awt.Robot();
        } catch (java.awt.AWTException e) {
            System.out.println("Error: No se pudo iniciar el control de mouse.");
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();

        // Configuracion Basica Fondo y Profundidad
        setupBasicGL(gl);

        // Iluminacion y neblina
        setupLighting(gl);
        setupFog(gl);

        // Cargaar recursos (texturas)
        cargarTodasLasTexturas(gl);
        
        // Cargar archivo de audio
        reproducirMusica("audios/bad_apple_song.wav");
    }

    // Funciones que separe del init
    private void setupBasicGL(GL2 gl){
        // Color de fondo
        float[] colorFondo = {0.00f, 0.00f, 0.00f, 1.0f}; // Fondo negro
        gl.glClearColor(colorFondo[0], colorFondo[1], colorFondo[2], colorFondo[3]);
        
        //Configuracion de profundidad (Z-Buffer)
        gl.glClearDepth(1.0f);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        
        // Suavizado de perspectivas y sombras
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        gl.glShadeModel(GL_SMOOTH);
    }
    private void setupLighting(GL2 gl) {
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_LIGHT0); // Luz principal (la toma automaticamente luz movil)
        gl.glEnable(GL_LIGHT1); // Luz ambiental tenue

        // Luz 0: Direccional/Puntual desde la pantalla hacia el escenario
        float[] lightPos0 = {0.0f, 5.0f, -4.0f, 1.0f}; 
        float[] lightColor0 = {0.9f, 0.9f, 0.9f, 1.0f}; // Luz blanca fuerte
        gl.glLightfv(GL_LIGHT0, GL_POSITION, lightPos0, 0);
        gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, lightColor0, 0);
        gl.glLightfv(GL_LIGHT0, GL_SPECULAR, lightColor0, 0);

        // Luz 1: Ambiental azulada/gris para que no se vea todo negro absoluto
        float[] ambientColor = {0.2f, 0.2f, 0.3f, 1.0f};
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, ambientColor, 0);
    }
    private void setupFog(GL2 gl){
        // Para la niebla (gris oscuro o negro)
        float[] colorNiebla = {0.15f, 0.15f, 0.15f, 1.0f}; 
        
        gl.glEnable(GL_FOG);
        gl.glFogi(GL_FOG_MODE, GL_EXP2); // Modo exponencial (más realista)
        gl.glFogf(GL_FOG_DENSITY, 0.01f); // Que tan espesa es (0.01f a 0.1f)
        
        gl.glFogfv(GL_FOG_COLOR, colorNiebla, 0);
        gl.glHint(GL_FOG_HINT, GL_NICEST);
    }
    private void cargarTodasLasTexturas(GL2 gl) {
        // Cargar texturas simples (que no se repiten)
        texturaMetal = cargarTextura("imagenes/metal.jpg");
        texturaLuna = cargarTextura("imagenes/luna.jpg");
        
        // Cargar texturas que SE REPITEN (Tiling)
        // Usamos una función auxiliar para no repetir código
        texturaPiso = cargarTextura("imagenes/piso.jpg");
        aplicarRepeticion(gl, texturaPiso); // Configura GL_REPEAT automáticamente
        texturaMarmol = cargarTextura("imagenes/marmol.jpg");
        aplicarRepeticion(gl, texturaMarmol);
    }
    // Ayuda para configurar texturas repetibles (Piso/Mármol)
    private void aplicarRepeticion(GL2 gl, Texture tex) {
        if (tex != null) {
            tex.enable(gl);
            tex.bind(gl);
            // Configurar para que se repita (Tile) y no se estire
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            // Filtros para que se vea bien de lejos/cerca
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            tex.disable(gl);
        }
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        
        // Evitar division por cero
        if (height == 0) height = 1;
        // Calcular la proporcion (relacion entre ancho y alto de la ventana)
        float aspect = (float) width / height;
        // Mapeo a Pixeles (usa toda la ventana paraa pintar, desde la esquina 0,0 a w,h)
        gl.glViewport(0, 0, width, height);
        // Cambiar al modo "lente"
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity(); // Resetear la lente
        glu.gluPerspective(60.0, aspect, 0.5, 150.0); // FOV de 60 grados
        // Regresar al modo objetos
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // CÁMARA (LookAt)
        // Calcular hacia donde miramos basándonos en Yaw y Pitch
        float lookX = (float) (camX + Math.cos(Math.toRadians(camYaw)));
        float lookY = (float) (camY + Math.tan(Math.toRadians(camPitch)));
        float lookZ = (float) (camZ + Math.sin(Math.toRadians(camYaw)));
        
        glu.gluLookAt(camX, camY, camZ, lookX, lookY, lookZ, 0, 1, 0);

        // LUCES Y VIDEO
        luzMovil(gl);
        actualizarFrameVideo(gl);

        // DIBUJAR AMBIENTE (fondo)
        dibujarDonaGigante(gl);
        dibujarPilaresInfinitos(gl);
        dibujarLuna(gl);
        
        // DIBUJAR ESCENARIO (Centro)
        dibujarPantallaVideo(gl);
        dibujarPiso(gl);
        dibujarTruss(gl);
        dibujarCapilla(gl);
        
        // EFECTOS
        dibujarCubosFlotantes(gl);
        dibujarNieve(gl);
        
        // Actualizar contador de frames
        //frameActual++;
        //if (frameActual >= totalFrames) frameActual = 0; // Loop
        
        // CONTROL DE FRAMES SINCRONIZADO CON AUDIO
        if (tiempoInicio > 0) {
            // Calculamos cuántos segundos han pasado desde que empezó la música
            long tiempoActual = System.currentTimeMillis();
            double segundosTranscurridos = (tiempoActual - tiempoInicio) / 1000.0;
            
            // Convertimos segundos a número de frame (Tiempo * 30 FPS)
            frameActual = (int) (segundosTranscurridos * FPS);
            
            // Si el video termina, reiniciamos todo (Loop)
            if (frameActual >= totalFrames) {
                if (clipAudio != null) {
                    // Detener si está sonando
                    if (clipAudio.isRunning()) {
                        clipAudio.stop();
                    }
                    
                    // Limpiar el buffer de memoria (Si no deja de sonar en la sigueinte vuelta)
                    clipAudio.flush();
                    
                    // Rebobinar usando Frames (usamos el frame para volver al estado de inicio)
                    clipAudio.setFramePosition(0);
                    
                    // Arrancar de nuevo
                    clipAudio.start();
                }
                tiempoInicio = System.currentTimeMillis(); // Resetear reloj
                frameActual = 0;
            }
        } else {
            // RESPALDO: Si el audio falló, avanzamos normal para que no se congele
            frameActual++;
            if (frameActual >= totalFrames) frameActual = 0;
        }
    }

    //Funciones que separe del display
    // << AMBIENTE DE FONDO >>
    
    // Dona gigante tipo alambre
    private void dibujarDonaGigante(GL2 gl){
        gl.glPushMatrix();
        
        // Material del Anillo (Gris claro o Cian para contraste)
        float[] colorAnillo = {0.5f, 0.5f, 0.5f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorAnillo, 0);
        // Quitamos texturas para que sea líneas puras
        gl.glDisable(GL_TEXTURE_2D); 
        
        // Animacion
        // Rotación LENTA constante
        // Lo rotamos un poco en X para que esté inclinado y se vea más dinámico
        gl.glRotatef(20.0f, 1.0f, 0.0f, 0.0f); // Inclinación fija
        gl.glRotatef(frameActual * 0.2f, 0.0f, 1.0f, 0.0f); // Rotación giratoria
        
        // DIBUJAR EL TORUS (Dona)
        // Parámetros: (Grosor del tubo, Radio del hueco, Lados del tubo, Lados del anillo)
        // Radio interno 40.0f = Gigante, estamos dentro de ella.
        // Grosor 1.0f = para que se vea fino y elegante.
        glut.glutWireTorus(1.0f, 60.0f, 10, 50); 
        
        // Un segundo anillo cruzado
        gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        glut.glutWireTorus(1.0f, 60.0f, 10, 50); 

        gl.glEnable(GL_TEXTURE_2D); // Reactivamos texturas para lo demás
        gl.glPopMatrix();
    }
    
    // Pilares Circulares del Infinito
    private void dibujarPilaresInfinitos(GL2 gl){
        gl.glPushMatrix();
        
        // Material OSCURO (Casi negro)
        float[] colorPilar = {0.05f, 0.05f, 0.05f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorPilar, 0);
        gl.glDisable(GL_TEXTURE_2D); 

        // CONFIGURACIÓN:
        int cantidadPilares = 12; // Cantidad de pilares
        float radioBase = 70.0f;  // Qué tan lejos están del centro

        for (int i = 0; i < cantidadPilares; i++) {
            gl.glPushMatrix();
            
            // --- CÁLCULO CIRCULAR ---
            // Dividimos el círculo (2 PI) entre la cantidad de pilares
            double angulo = (2.0 * Math.PI / cantidadPilares) * i;
            
            // Variación "aleatoria" para que no sea un círculo perfecto
            // Algunos se acercan un poco, otros se alejan
            float variacionRadio = (float) Math.sin(i * 12.34) * 10.0f;
            float radioFinal = radioBase + variacionRadio;
            
            // Convertimos ángulo a coordenadas X y Z
            float x = (float) Math.cos(angulo) * radioFinal;
            float z = (float) Math.sin(angulo) * radioFinal;
            
            // --- INFINITUD ---
            // Los ponemos muy abajo (Y = -100)
            gl.glTranslatef(x, -100.0f, z);
            
            // Rotamos el pilar para que mire al centro
            //gl.glRotatef((float) Math.toDegrees(-angulo), 0.0f, 1.0f, 0.0f);

            // --- DIBUJAR ---
            // Grosor variable entre 2 y 4
            float grosor = 2.0f + (float) Math.abs(Math.cos(i * 5.5)) * 2.0f;
            
            // Altura 250 (Infinitos hacia arriba)
            dibujarCaja(gl, grosor, 250.0f, grosor);
            
            gl.glPopMatrix();
        }
        
        gl.glEnable(GL_TEXTURE_2D);
        gl.glPopMatrix();
    }
    
    // La Luna
    private void dibujarLuna(GL2 gl){
        gl.glPushMatrix();
        
        // Material BLANCO (para que se vea la textura tal cual)
        float[] colorLuna = {1.0f, 1.0f, 1.0f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorLuna, 0);
        
        // Le damos un poco de brillo propio (EMISSION) para que resalte en la oscuridad
        // Un gris tenue, para que no se vea como un foco cegador, sino como un cuerpo celeste
        float[] brilloLuna = {0.3f, 0.3f, 0.3f, 1.0f};
        gl.glMaterialfv(GL_FRONT, GL_EMISSION, brilloLuna, 0);

        // Activamos textura
        if (texturaLuna != null) {
            texturaLuna.enable(gl);
            texturaLuna.bind(gl);
        }

        // POSICIÓN: Muy arriba (Y=80) y centrada (X=0, Z=0)
        gl.glTranslatef(0.0f, 80.0f, 0.0f);
        
        // Rotación para que la textura gire lento
        gl.glRotatef(frameActual * 0.1f, 0.0f, 1.0f, 0.0f); 

        // DIBUJAR ESFERA CON GLU (Maneja mejor las texturas esféricas)
        // Necesitamos crear un objeto "Quadric" para poder texturizar la esfera
        com.jogamp.opengl.glu.GLUquadric luna = glu.gluNewQuadric();
        glu.gluQuadricTexture(luna, true); // ¡Esto activa el mapeo de la imagen!
        
        // Dibujamos: Objeto, Radio(15), Rebanadas(30), Anillos(30)
        glu.gluSphere(luna, 15.0f, 30, 30); 

        // Limpieza
        if (texturaLuna != null) texturaLuna.disable(gl);
        
        // Apagamos el brillo para no afectar lo siguiente que se dibuje
        float[] sinBrilloLuna = {0.0f, 0.0f, 0.0f, 1.0f};
        gl.glMaterialfv(GL_FRONT, GL_EMISSION, sinBrilloLuna, 0);
        
        gl.glPopMatrix();
    }
    
    // << ESCENARIO CENTRAL >>
    
    // Truss o marco de la pantalla
    private void dibujarTruss(GL2 gl) {
        if (texturaMetal != null) {
            texturaMetal.enable(gl);
            texturaMetal.bind(gl);
        }
        
        float[] colorMetal = {0.4f, 0.4f, 0.4f, 1.0f};
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorMetal, 0);
        gl.glMaterialfv(GL_FRONT, GL_SPECULAR, colorMetal, 0);
        gl.glMateriali(GL_FRONT, GL_SHININESS, 50);

        // Dibujar columnas simples a los lados de la pantalla
        gl.glPushMatrix();
        gl.glTranslatef(-4.5f, 0, -5.0f); // Izquierda
        dibujarCaja(gl, 0.5f, 6.0f, 0.5f);
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        gl.glTranslatef(4.5f, 0, -5.0f); // Derecha
        dibujarCaja(gl, 0.5f, 6.0f, 0.5f);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslatef(0, 5.5f, -5.0f); // Arriba
        dibujarCaja(gl, 9.5f, 0.5f, 0.5f);
        gl.glPopMatrix();

        if (texturaMetal != null) texturaMetal.disable(gl);
    }
    
    // Pantalla de Video
    private void dibujarPantallaVideo(GL2 gl){
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 2.5f, -5.0f); // Posición de la pantalla
        if (texturaPantalla != null) {
            texturaPantalla.enable(gl);
            texturaPantalla.bind(gl);
            // Hacer que la pantalla brille (ignorar iluminación parcial)
            float[] emission = {0.5f, 0.5f, 0.5f, 1.0f};
            gl.glMaterialfv(GL_FRONT, GL_EMISSION, emission, 0);
        }
        
        gl.glColor3f(1, 1, 1);
        gl.glBegin(GL_QUADS); // inicio del dibujo del rectangulo para proyectar
            // 4:3 Aspect Ratio (4 ancho, 3 alto)
            gl.glNormal3f(0, 0, 1);
            gl.glTexCoord2f(0, 1); gl.glVertex3f(-4.0f, -3.0f, 0.0f);
            gl.glTexCoord2f(1, 1); gl.glVertex3f( 4.0f, -3.0f, 0.0f);
            gl.glTexCoord2f(1, 0); gl.glVertex3f( 4.0f,  3.0f, 0.0f);
            gl.glTexCoord2f(0, 0); gl.glVertex3f(-4.0f,  3.0f, 0.0f);
        gl.glEnd();
        
        if (texturaPantalla != null) {
            texturaPantalla.disable(gl);
            // Apagar emisión para los demás objetos
            float[] noEmission = {0.0f, 0.0f, 0.0f, 1.0f};
            gl.glMaterialfv(GL_FRONT, GL_EMISSION, noEmission, 0);
        }
        gl.glPopMatrix();
    }
    
    //Piso Detallado
    private void dibujarPiso(GL2 gl){
        if (texturaPiso != null) {
            texturaPiso.enable(gl);
            texturaPiso.bind(gl);
        }
        
        // Material del piso (Gris medio)
        float[] matPiso = {0.6f, 0.6f, 0.6f, 1.0f};
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, matPiso, 0);
        
        // DIBUJAR REJILLA DE 40x40 CUADRITOS
        // Esto crea muchos vértices para que la luz se calcule bien en cada punto
        float step = 1.0f; // Tamaño de cada cuadrito
        float size = 20.0f; // Tamaño total del piso (de -20 a 20)
        float repeticionesTextura = 10.0f; // Cuántas veces se repite la imagen

        gl.glBegin(GL_QUADS);
        gl.glNormal3f(0, 1, 0); // La normal siempre apunta arriba
        
        for (float x = -size; x < size; x += step) {
            for (float z = -size; z < size; z += step) {
                // Coordenadas de textura (Matemáticas para ajustar la imagen al cuadrito)
                float s0 = ((x + size) / (size * 2)) * repeticionesTextura;
                float s1 = ((x + step + size) / (size * 2)) * repeticionesTextura;
                float t0 = ((z + size) / (size * 2)) * repeticionesTextura;
                float t1 = ((z + step + size) / (size * 2)) * repeticionesTextura;

                // Vértice 1
                gl.glTexCoord2f(s0, t0); 
                gl.glVertex3f(x, 0, z);
                
                // Vértice 2
                gl.glTexCoord2f(s0, t1); 
                gl.glVertex3f(x, 0, z + step);
                
                // Vértice 3
                gl.glTexCoord2f(s1, t1); 
                gl.glVertex3f(x + step, 0, z + step);
                
                // Vértice 4
                gl.glTexCoord2f(s1, t0); 
                gl.glVertex3f(x + step, 0, z);
            }
        }
        gl.glEnd();
        
        if (texturaPiso != null) texturaPiso.disable(gl);
    }
    
    // Capilla
    private void dibujarCapilla(GL2 gl) {
        gl.glPushMatrix();
        
        // Material Blanco
        float[] matMarmol = {1.0f, 1.0f, 1.0f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, matMarmol, 0);
        float[] especular = {0.8f, 0.8f, 0.8f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_SPECULAR, especular, 0);
        gl.glMateriali(GL_FRONT, GL_SHININESS, 100); 

        // Activar textura de Mármol
        if (texturaMarmol != null) {
            gl.glEnable(GL_TEXTURE_2D);
            texturaMarmol.enable(gl);
            texturaMarmol.bind(gl);
        } else {
            gl.glDisable(GL_TEXTURE_2D); 
        }

        float limite = 18.0f;
        float alturaPilar = 25.0f; 
        float grosorPilar = 4.0f;

        // --- PILARES ---
        float[] posicionesX = {limite, -limite, limite, -limite};
        float[] posicionesZ = {limite, limite, -limite, -limite};

        for (int i = 0; i < 4; i++) {
            gl.glPushMatrix();
            gl.glTranslatef(posicionesX[i], 0.0f, posicionesZ[i]);
            dibujarCajaRepetida(gl, grosorPilar, alturaPilar, grosorPilar);
            gl.glPopMatrix();
        }

        // --- BARANDALES ---
        float alturaBarandal = 0.8f; 
        float largoBarandal = limite * 2; 
        
        gl.glPushMatrix(); gl.glTranslatef(0.0f, 0.0f, -limite);
        dibujarCajaRepetida(gl, largoBarandal, alturaBarandal, 1.0f); gl.glPopMatrix();

        gl.glPushMatrix(); gl.glTranslatef(0.0f, 0.0f, limite);
        dibujarCajaRepetida(gl, largoBarandal, alturaBarandal, 1.0f); gl.glPopMatrix();
        
        gl.glPushMatrix(); gl.glTranslatef(limite, 0.0f, 0.0f);
        dibujarCajaRepetida(gl, 1.0f, alturaBarandal, largoBarandal); gl.glPopMatrix();
        
        gl.glPushMatrix(); gl.glTranslatef(-limite, 0.0f, 0.0f);
        dibujarCajaRepetida(gl, 1.0f, alturaBarandal, largoBarandal); gl.glPopMatrix();

        // --- CÚPULA (DONA CON TEXTURA) ---
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, alturaPilar, 0.0f); 
        gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        
        // Truco para que la textura se pegue a la dona
        // Activamos la generación automática de coordenadas
        gl.glEnable(GL_TEXTURE_GEN_S);
        gl.glEnable(GL_TEXTURE_GEN_T);
        
        // Parámetros para que la textura se vea tipo "esférica" sobre la dona
        gl.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
        gl.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);

        glut.glutSolidTorus(2.0f, 25.0f, 30, 30); 
        
        // Apagamos la generación automática para no arruinar lo demás
        gl.glDisable(GL_TEXTURE_GEN_S);
        gl.glDisable(GL_TEXTURE_GEN_T);
        
        gl.glPopMatrix();

        // Limpieza final
        if (texturaMarmol != null) texturaMarmol.disable(gl);
        gl.glDisable(GL_TEXTURE_2D); 
        
        gl.glPopMatrix();
    }
    
    // << EFECTOS DINAMICOS >>
    
    // Luz movil (luz tipo discoteca)
    private void luzMovil(GL2 gl){
        // CÁLCULO DE POSICIÓN
        // 0.01f. Velocidad de la bombilla disco
        // El 8.0f es el ancho (qué tanto se va a la izquierda o derecha)
        float lightX = (float) Math.sin(frameActual * 0.01f) * 8.0f; // el sin es el que convierte el movimiento en va y ven
        
        float[] lightPosMovil = {lightX, 6.0f, -2.0f, 1.0f}; 
        gl.glLightfv(GL_LIGHT0, GL_POSITION, lightPosMovil, 0);

        // DIBUJAR LA BOMBILLA (Para que ver quién emite la luz)
        gl.glPushMatrix();
            gl.glTranslatef(lightX, 6.0f, -2.0f); // Moverse al mismo lugar que la luz
            
            // Hacemos que brille (Emisión) para que parezca un foco encendido
            float[] colorFoco = {1.0f, 1.0f, 0.8f, 1.0f}; // Amarillento brillante
            gl.glMaterialfv(GL_FRONT, GL_EMISSION, colorFoco, 0);
            
            // Dibujamos una cajita pequeña que representa el foco
            dibujarCaja(gl, 0.2f, 0.2f, 0.2f);
            
            // APAGAMOS EL BRILLO (para no afectar lo demás)
            float[] sinBrillo = {0.0f, 0.0f, 0.0f, 1.0f};
            gl.glMaterialfv(GL_FRONT, GL_EMISSION, sinBrillo, 0);
        gl.glPopMatrix();
    }
    
    // Cubos Rojos Flotantes
    private void dibujarCubosFlotantes(GL2 gl){
        // Color Rojo para los cubos
        float[] colorCubos = {0.8f, 0.0f, 0.0f, 1.0f}; 
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorCubos, 0);

        float anguloRotacion = frameActual * 0.5f; // Giran más lento a lo lejos

        for (int i = 0; i < 15; i++) { // cantidad de cubos
            gl.glPushMatrix();
            
            // --- DISTANCIA ---
            // distancia a partir del radio en el centro
            float distancia = 50.0f; 
            
            // Distribución circular alejada
            float posX = (float) Math.sin(i * 0.5) * distancia; 
            float posZ = -10.0f + (float) Math.cos(i * 0.5) * distancia; // -10 empuja el centro hacia atrás
            
            // Altura variada (entre 5 y 15 unidades de altura)
            float posY = 5.0f + (float) Math.abs(Math.sin((frameActual * 0.02) + i)) * 10.0f;

            gl.glTranslatef(posX, posY, posZ);
            gl.glRotatef(anguloRotacion + (i * 20), 1.0f, 1.0f, 1.0f);
            
            // Cubos más grandes para que se noten a la distancia
            dibujarCaja(gl, 4.0f, 4.0f, 4.0f); 
            
            gl.glPopMatrix();
        }

        // --- RESETEAR COLOR A BLANCO ---
        // Esto evita que la pantalla se pinte de rojo en el siguiente frame
        float[] colorBlanco = {1.0f, 1.0f, 1.0f, 1.0f};
        gl.glMaterialfv(GL_FRONT, GL_DIFFUSE, colorBlanco, 0);
    }
    
    // Nieve
    private void dibujarNieve(GL2 gl){
        // Apagamos luces y texturas para que sean puntos blancos puros
        gl.glDisable(GL_LIGHTING);
        gl.glDisable(GL_TEXTURE_2D);
        
        gl.glColor3f(1.0f, 1.0f, 1.0f); // Color Blanco Nieve
        gl.glPointSize(3.0f); // Tamaño de los copos (entre 2 y 4 se ve bien)

        gl.glBegin(GL_POINTS);
        
        // Creamos 800 copos de nieve
        for (int i = 0; i < 800; i++) {
            // MATEMÁTICAS ALEATORIAS FIJAS:
            // Usamos funciones trigonométricas con números primos grandes para simular aleatoriedad
            // sin usar Random() que es lento.
            
            // Posición X (ancho): de -25 a 25
            float px = (float) (Math.sin(i * 12.9898) * 25.0f);
            
            // Posición Z (profundidad): de -25 a 25
            float pz = (float) (Math.cos(i * 78.233) * 25.0f);
            
            // Posición Y (altura): Cae desde 20.0 hasta 0.0
            // Usamos frameActual para animar la caída
            // El ' + i' hace que cada copo esté en una altura distinta
            float alturaInicial = 20.0f;
            float velocidadCaida = 0.1f;
            float py = alturaInicial - ((frameActual * velocidadCaida + i) % 20.0f);
            
            gl.glVertex3f(px, py, pz);
        }
        gl.glEnd();

        // Restauramos el estado de OpenGL 
        gl.glEnable(GL_LIGHTING); // Volvemos a encender luces para el siguiente cuadro
        gl.glEnable(GL_TEXTURE_2D); // Volvemos a permitir texturas
    }
    
    
    
    // Lógica para cargar la imagen correspondiente al frame actual
    private void actualizarFrameVideo(GL2 gl) {
        try {
            // Generar nombre de archivo: output_0001.png, output_0002.png, etc.
            // %04d significa que rellena con ceros hasta tener 4 dígitos
            String nombreArchivo = String.format("%s%04d%s", prefijoImagen, frameActual, extensionImagen);
            String rutaCompleta = rutaImagenesVideo + nombreArchivo;
            
            File archivoImg = new File(rutaCompleta);
            if(archivoImg.exists()){
                // Si ya había una textura cargada, destruirla para liberar memoria (si no colapsa)
                if(texturaPantalla != null) {
                    texturaPantalla.destroy(gl);
                }
                
                BufferedImage buffImage = ImageIO.read(archivoImg);
                texturaPantalla = AWTTextureIO.newTexture(GLProfile.getDefault(), buffImage, false);
            }
        } catch (Exception e) {
            // Si falla, no rompemos el programa, solo no actualiza textura
            // System.out.println("Error cargando frame: " + frameActual); 
        }
    }

    // Función auxiliar para dibujar cubos
    private void dibujarCaja(GL2 gl, float w, float h, float d) {
        float x = w/2; float y = h; float z = d/2; // Pivot en la base
        gl.glBegin(GL_QUADS);
        // Frente
        gl.glNormal3f(0, 0, 1);
        gl.glTexCoord2f(0, 0); gl.glVertex3f(-x, 0, z);
        gl.glTexCoord2f(1, 0); gl.glVertex3f( x, 0, z);
        gl.glTexCoord2f(1, 1); gl.glVertex3f( x, y, z);
        gl.glTexCoord2f(0, 1); gl.glVertex3f(-x, y, z);
        // Atras
        gl.glNormal3f(0, 0, -1);
        gl.glVertex3f(-x, 0, -z);
        gl.glVertex3f(-x, y, -z);
        gl.glVertex3f( x, y, -z);
        gl.glVertex3f( x, 0, -z);
        // Izq
        gl.glNormal3f(-1, 0, 0);
        gl.glTexCoord2f(0, 0); gl.glVertex3f(-x, 0, -z);
        gl.glTexCoord2f(1, 0); gl.glVertex3f(-x, 0,  z);
        gl.glTexCoord2f(1, 1); gl.glVertex3f(-x, y,  z);
        gl.glTexCoord2f(0, 1); gl.glVertex3f(-x, y, -z);
        // Der
        gl.glNormal3f(1, 0, 0);
        gl.glTexCoord2f(0, 0); gl.glVertex3f( x, 0,  z);
        gl.glTexCoord2f(1, 0); gl.glVertex3f( x, 0, -z);
        gl.glTexCoord2f(1, 1); gl.glVertex3f( x, y, -z);
        gl.glTexCoord2f(0, 1); gl.glVertex3f( x, y,  z);
        // Top
        gl.glNormal3f(0, 1, 0);
        gl.glVertex3f(-x, y, z);
        gl.glVertex3f( x, y, z);
        gl.glVertex3f( x, y, -z);
        gl.glVertex3f(-x, y, -z);
        gl.glEnd();
    }
    
    // Dibuja caja repitiendo la textura (Tiling)
    // para que la textura en la capilla no se distorcione
    private void dibujarCajaRepetida(GL2 gl, float w, float h, float d) {
        float x = w / 2;
        float y = h;
        float z = d / 2;

        // CÁLCULO
        // Por cada 5 unidades de tamaño, repetimos la textura 1 vez.
        // Si el barandal mide 36, la textura se repite 7.2 veces.
        float repX = w / 5.0f; 
        float repY = h / 5.0f;
        float repZ = d / 5.0f;

        gl.glBegin(GL_QUADS);
        // Frente
        gl.glNormal3f(0, 0, 1);
        gl.glTexCoord2f(0, 0);       gl.glVertex3f(-x, 0, z);
        gl.glTexCoord2f(repX, 0);    gl.glVertex3f(x, 0, z);
        gl.glTexCoord2f(repX, repY); gl.glVertex3f(x, y, z);
        gl.glTexCoord2f(0, repY);    gl.glVertex3f(-x, y, z);

        // Atras
        gl.glNormal3f(0, 0, -1);
        gl.glTexCoord2f(0, 0);       gl.glVertex3f(x, 0, -z); // Invertido X
        gl.glTexCoord2f(repX, 0);    gl.glVertex3f(-x, 0, -z);
        gl.glTexCoord2f(repX, repY); gl.glVertex3f(-x, y, -z);
        gl.glTexCoord2f(0, repY);    gl.glVertex3f(x, y, -z);

        // Izquierda
        gl.glNormal3f(-1, 0, 0);
        gl.glTexCoord2f(0, 0);       gl.glVertex3f(-x, 0, -z);
        gl.glTexCoord2f(repZ, 0);    gl.glVertex3f(-x, 0, z);
        gl.glTexCoord2f(repZ, repY); gl.glVertex3f(-x, y, z);
        gl.glTexCoord2f(0, repY);    gl.glVertex3f(-x, y, -z);

        // Derecha
        gl.glNormal3f(1, 0, 0);
        gl.glTexCoord2f(0, 0);       gl.glVertex3f(x, 0, z);
        gl.glTexCoord2f(repZ, 0);    gl.glVertex3f(x, 0, -z);
        gl.glTexCoord2f(repZ, repY); gl.glVertex3f(x, y, -z);
        gl.glTexCoord2f(0, repY);    gl.glVertex3f(x, y, z);

        // Top (Techo)
        gl.glNormal3f(0, 1, 0);
        gl.glTexCoord2f(0, 0);       gl.glVertex3f(-x, y, z);
        gl.glTexCoord2f(repX, 0);    gl.glVertex3f(x, y, z);
        gl.glTexCoord2f(repX, repZ); gl.glVertex3f(x, y, -z);
        gl.glTexCoord2f(0, repZ);    gl.glVertex3f(-x, y, -z);
        gl.glEnd();
    }

    Texture cargarTextura(String imageFile) {
        Texture text1 = null;
        try {
            File f = new File(imageFile);
            // DIAGNÓSTICO: Imprimir dónde está buscando
            if (!f.exists()) {
                System.out.println("ERROR CRÍTICO: No encuentro el archivo en: " + f.getAbsolutePath());
            } 
            
            BufferedImage buffImage = ImageIO.read(f);
            text1 = AWTTextureIO.newTexture(GLProfile.getDefault(), buffImage, false);
        } catch (Exception e) {
            System.out.println("¡EXCEPCIÓN cargando " + imageFile + "!: " + e.getMessage());
        }
        return text1;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {}

    // CONTROL DE TECLADO (WASD)
    @Override
    public void keyPressed(KeyEvent e) {
        float speed = 0.5f;
        float rad = (float) Math.toRadians(camYaw);

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: // Adelante
                camX += Math.cos(rad) * speed;
                camZ += Math.sin(rad) * speed;
                break;
            case KeyEvent.VK_S: // Atrás
                camX -= Math.cos(rad) * speed;
                camZ -= Math.sin(rad) * speed;
                break;
            case KeyEvent.VK_A: // Izquierda (Strafe)
                camX += Math.cos(rad - Math.PI / 2) * speed;
                camZ += Math.sin(rad - Math.PI / 2) * speed;
                break;
            case KeyEvent.VK_D: // Derecha (Strafe)
                camX += Math.cos(rad + Math.PI / 2) * speed;
                camZ += Math.sin(rad + Math.PI / 2) * speed;
                break;
            
            // --- CAMBIO AQUÍ ---
            case KeyEvent.VK_ESCAPE:
                // En lugar de salir de golpe, alternamos el bloqueo del mouse
                mouseLocked = !mouseLocked; 

                if (!mouseLocked) {
                    // Si desbloqueamos: Mostramos el cursor normal para poder cerrar la ventana
                    this.setCursor(Cursor.getDefaultCursor());
                } else {
                    // Si bloqueamos: Ocultamos el cursor otra vez
                    this.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                            new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank"));
                    
                    // Opcional: Centrarlo inmediatamente para que no salte la cámara
                    if (robot != null) {
                        Point loc = this.getLocationOnScreen();
                        robot.mouseMove(loc.x + getWidth()/2, loc.y + getHeight()/2);
                    }
                }
                break;
            // -------------------
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    // CONTROL DE MOUSE (VISTA)
    @Override
    public void mouseMoved(MouseEvent e) {
        // Si no tenemos robot o el mouse no está bloqueado, no hacemos nada
        if (robot == null || !mouseLocked) return;
        
        // Calculamos el centro de la ventana (el canvas)
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Si el mouse YA está en el centro (porque el robot lo acaba de poner ahí), ignoramos este evento
        // Esto evita un bucle infinito y temblores
        if (e.getX() == centerX && e.getY() == centerY) return;

        // Calculamos cuánto se movió desde el centro
        int dx = e.getX() - centerX;
        int dy = e.getY() - centerY;
        
        // Sensibilidad
        float sensitivity = 0.3f;
        
        // Aplicamos el movimiento a la cámara
        camYaw += dx * sensitivity;
        camPitch -= dy * sensitivity;
        
        // Límites verticales (Cuello)
        if(camPitch > 89.0f) camPitch = 89.0f;
        if(camPitch < -89.0f) camPitch = -89.0f;
        
        // --- CURSOR INFINITO ---
        // Usamos el Robot para devolver el mouse fisicamente al centro de la pantalla
        try {
            // Necesitamos la posición absoluta en el monitor, no solo en la ventana
            Point locationOnScreen = this.getLocationOnScreen();
            robot.mouseMove(locationOnScreen.x + centerX, locationOnScreen.y + centerY);
        } catch (Exception ex) {
            // Si la ventana no es visible aún, puede fallar, lo ignoramos
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e); // Comportarse igual si arrastra
    }
    
    // REPRODUCIR MUSICA
    private void reproducirMusica(String ruta) {
        try {
            File archivoAudio = new File(ruta);
            if (archivoAudio.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(archivoAudio);
                clipAudio = AudioSystem.getClip();
                clipAudio.open(audioInput);
                
                // Iniciamos el audio
                clipAudio.start();
                
                // MARCAMOS EL TIEMPO DE INICIO (Vital para la sincronización)
                tiempoInicio = System.currentTimeMillis();
            } else {
                System.out.println("No encuentro el audio en: " + ruta);
            }
        } catch (Exception e) {
            System.out.println("Error al reproducir audio: " + e.getMessage());
        }
    }
}