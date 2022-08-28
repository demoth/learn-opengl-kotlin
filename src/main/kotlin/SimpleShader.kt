import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

class SimpleShader {
    // The window handle
    private var window: Long = 0
    private val fullscreen = false

    private var shaderProgram: Int = 0
    private var vertexArray = 0

    private val vertexShaderSource = """
        #version 330 core
        layout (location = 0) in vec3 vert;
        void main()
        {
            gl_Position = vec4(vert.x, vert.y, 0.0, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 330 core
        out vec4 outColor;
        void main()
        {
            outColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
        }
    """.trimIndent()

    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")
        init()
        loop()

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()
    }

    private fun init() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 8)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

        // Create the window
        val primaryMonitor = GLFW.glfwGetPrimaryMonitor()

        window = GLFW.glfwCreateWindow(512, 512, "Hello World!",
            if (fullscreen) primaryMonitor else MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        check(window != MemoryUtil.NULL) { "Failed to create the GLFW window" }

        // Set up a key callback. It will be called every time a key is pressed, repeated or released.
        // Alternative approach would be probing a specific key event in the main loop.
        GLFW.glfwSetKeyCallback(window) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
                GLFW.glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
        }
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1) // int*
            val height = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(window, width, height)

            // Get the resolution of the primary monitor
            val vidmode = GLFW.glfwGetVideoMode(primaryMonitor)

            // Center the window
            GLFW.glfwSetWindowPos(
                window,
                (vidmode!!.width() - width[0]) / 2,
                (vidmode.height() - height[0]) / 2
            )
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()

        GLFW.glfwSetFramebufferSizeCallback(window) { window, width, height ->
            glViewport(0, 0, width, height)
        }

        vertexArray = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vertexArray)

        // x, y
        val vertices = floatArrayOf(
            0.0f, 0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f
        )
        val vertexBuffer = GL15.glGenBuffers()
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
        GL15.glBufferData(vertexBuffer, vertices, GL_STATIC_DRAW)


        // why?
        GL15.glBindBuffer(GL_ARRAY_BUFFER, 0) // unbind

        // Create and compile shaders
        val vertexShader = createShader(vertexShaderSource, GL_VERTEX_SHADER)
        val fragmentShader = createShader(fragmentShaderSource, GL_FRAGMENT_SHADER)
        shaderProgram = createProgram(vertexShader, fragmentShader)

        // bind vertex attribute
        val vertAttrLocation = GL20.glGetAttribLocation(shaderProgram, "vert")
        GL20.glEnableVertexAttribArray(vertAttrLocation)
        GL20.glVertexAttribPointer(vertAttrLocation, 3, GL_FLOAT, false, 0, 0)


        GL20.glDeleteShader(vertexShader)
        GL20.glDeleteShader(fragmentShader)

        GL30.glBindVertexArray(0) // unbind



        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()


        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Set the clear color
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT) // clear the framebuffer
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT) // clear the framebuffer

            GL20.glUseProgram(shaderProgram)
            GL30.glBindVertexArray(vertexArray)
            GL11.glDrawArrays(GL_TRIANGLES, 0, 3)

            GLFW.glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            GLFW.glfwPollEvents()
        }
    }
}

fun createProgram(vararg shaders: Int): Int {
    val program = GL20.glCreateProgram()
    shaders.forEach {
        GL20.glAttachShader(program, it)
    }
    GL30.glBindFragDataLocation(program, 0, "outColor")

    GL20.glLinkProgram(program)

    val statusValue = IntArray(1)
    GL20.glGetProgramiv(program, GL_LINK_STATUS, statusValue)
    val status = if (statusValue.first() != 0) "Success" else "Failure"
    println("Program compiled: $status")

    val compilationOutput = GL20.glGetProgramInfoLog(program, 1024)
    if (compilationOutput.isNotBlank())
        println("Linking output: $compilationOutput")

    return program

}

fun createShader(source: String, type: Int): Int {
    // Shader related code
    val shader = GL20.glCreateShader(type)
    GL20.glShaderSource(shader, source)
    GL20.glCompileShader(shader)

    val statusValue = IntArray(1)
    GL20.glGetShaderiv(shader, GL_COMPILE_STATUS, statusValue)
    val status = if (statusValue.first() != 0) "Success" else "Failure"
    println("Shader compiled: $status")

    val compilationOutput = GL20.glGetShaderInfoLog(shader, 1024)
    if (compilationOutput.isNotBlank())
        println("Compilation output: $compilationOutput")

    return shader
}

fun main() {
    SimpleShader().run()
}
