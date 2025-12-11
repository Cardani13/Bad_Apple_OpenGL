# Bad Apple - OpenGL Project

Una recreación 3D del famoso video musical "Bad Apple!!" utilizando **Java** y la librería gráfica **JOGL (Java OpenGL)**.

Este proyecto implementa un motor de **streaming de texturas sincronizado por audio** para reproducir la animación frame por frame dentro de un entorno 3D con efectos atmosféricos, iluminación dinámica y sistema de partículas.

## Características Principales

* **Motor de Streaming de Texturas:** Carga y libera memoria dinámicamente para reproducir +6500 cuadros sin saturar la RAM.
* **Sincronización Audio-Video:** Algoritmo basado en *Delta Time* que ajusta los frames al tiempo real de la canción `.wav`, evitando desfases si el rendimiento baja.
* **Escenario 3D Atmosférico:**
    * Cámara en primera persona (WASD + Mouse Infinito).
    * Iluminación puntual dinámica (Luz móvil).
    * Niebla volumétrica.
    * Sistema de partículas (Nieve con física de bucle).
* **Geometría Procedural:** Uso de `GL_REPEAT` y generación automática de texturas para la capilla y pilares.

## Requisitos del Sistema

Para ejecutar este proyecto necesitas:

* **Java Development Kit (JDK):** Versión 8 o superior.
* **IDE Recomendado:** NetBeans / IntelliJ / Eclipse.
* **Librerías OpenGL:** JOGL 2.x (Gluegen-rt y Jogl-all).

## Instalación y Configuración

1.  **Clonar el repositorio:**
    ```bash
    git clone [https://github.com/Cardani13/Bad_Apple_OpenGL.git](https://github.com/Cardani13/Bad_Apple_OpenGL.git)
    ```

2.  **Configurar Librerías (JOGL):**
    * Este proyecto requiere agregar los `.jar` de JOGL al *Classpath* de tu IDE.
    * Asegúrate de incluir las librerías nativas (`natives-windows`, `natives-linux`, etc.) correspondientes a tu sistema operativo.

3.  **Estructura de Carpetas:**
    Para que el proyecto funcione, asegúrate de que los recursos estén en la raíz del proyecto.
    
    > **Nota:** Debido al peso, asegúrate de colocar la secuencia de imágenes (frames) en la carpeta `imagenes/`.
    
    ```text
    Proyecto/
    ├── src/
    │   └── BadAppleStage.java
    ├── imagenes/
    │   ├── output_0001.jpg ... output_6572.jpg
    │   ├── piso.jpg
    │   ├── metal.jpg
    │   └── luna.jpg
    └── audios/
        └── bad_apple_song.wav
    ```

## Controles

| Tecla / Acción | Función |
| :--- | :--- |
| **W, A, S, D** | Moverse por el escenario (Adelante, Izq, Atrás, Der) |
| **Mouse** | Girar la cámara (Vista en primera persona) |
| **ESC** | Liberar/Capturar el cursor del mouse |

## Arquitectura del Código

El proyecto sigue una estructura modular dentro de `GLEventListener`:

* `init()`: Configuración inicial de luces, niebla y carga de audio.
* `display()`: Bucle principal de renderizado. Gestiona la lógica de sincronización y llama a los sub-módulos de dibujo.
* `reshape()`: Ajuste de la lente de la cámara (FOV) y corrección de aspecto.
* **Módulos de Dibujo:** Funciones separadas para `dibujarCapilla`, `dibujarNieve`, `luzMovil`, etc.

## Créditos

* **Música:** "Bad Apple!!" feat. nomico (Alstroemeria Records).
* **Programación:** Rodriguez Cano Carlos Daniel.

---
*Espero que disfruten este proyecto tanto como yo disfruté creándolo. :D*