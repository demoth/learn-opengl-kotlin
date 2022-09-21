/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package shaders

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Simple demo to showcase the use of
 * [GL20.glUniform3fv].
 *
 * @author Kai Burjack
 */
class UniformArrayDemo {
    private var window: Long = 0
    private var width = 1024
    private var height = 768
    private var vao = 0
    private var program = 0
    private var vec3ArrayUniform = 0
    private var chosenUniform = 0
    private var chosen = 0
    private val colors = BufferUtils.createFloatBuffer(3 * 4)

    init {
        colors.put(1f).put(0f).put(0f) // red
        colors.put(0f).put(1f).put(0f) // green
        colors.put(0f).put(0f).put(1f) // blue
        colors.put(1f).put(1f).put(0f) // yellow
        colors.flip()
    }

    var errCallback: GLFWErrorCallback? = null
    var keyCallback: GLFWKeyCallback? = null
    var fbCallback: GLFWFramebufferSizeCallback? = null
    var debugProc: Callback? = null
    @Throws(IOException::class)
    private fun init() {
        GLFW.glfwSetErrorCallback(object : GLFWErrorCallback() {
            private val delegate = createPrint(System.err)
            override fun invoke(error: Int, description: Long) {
                if (error == GLFW.GLFW_VERSION_UNAVAILABLE) System.err
                    .println("This demo requires OpenGL 3.0 or higher.")
                delegate.invoke(error, description)
            }

            override fun free() {
                delegate.free()
            }
        }.also { errCallback = it })
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        window = GLFW.glfwCreateWindow(
            width, height, "Uniform array test", MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (window == MemoryUtil.NULL) {
            throw AssertionError("Failed to create the GLFW window")
        }
        println("Press 'up' or 'down' to cycle through some colors.")
        GLFW.glfwSetFramebufferSizeCallback(window,
            object : GLFWFramebufferSizeCallback() {
                override fun invoke(window: Long, width: Int, height: Int) {
                    if (width > 0 && height > 0 && (this@UniformArrayDemo.width != width || this@UniformArrayDemo.height != height)) {
                        this@UniformArrayDemo.width = width
                        this@UniformArrayDemo.height = height
                    }
                }
            }.also {
                fbCallback = it
            })
        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(
                window: Long, key: Int, scancode: Int, action: Int,
                mods: Int
            ) {
                if (action != GLFW.GLFW_RELEASE) return
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    GLFW.glfwSetWindowShouldClose(window, true)
                } else if (key == GLFW.GLFW_KEY_UP) {
                    chosen = (chosen + 1) % 4
                } else if (key == GLFW.GLFW_KEY_DOWN) {
                    chosen = (chosen + 3) % 4
                }
            }
        }.also { keyCallback = it })
        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidmode!!.width() - width) / 2, (vidmode.height() - height) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(0)
        GLFW.glfwShowWindow(window)
        MemoryStack.stackPush().use { frame ->
            val framebufferSize = frame.mallocInt(2)
            GLFW.nglfwGetFramebufferSize(
                window,
                MemoryUtil.memAddress(framebufferSize),
                MemoryUtil.memAddress(framebufferSize) + 4
            )
            width = framebufferSize[0]
            height = framebufferSize[1]
        }
        GL.createCapabilities()
        debugProc = GLUtil.setupDebugMessageCallback()

        /* Create all needed GL resources */createVao()
        createRasterProgram()
        initProgram()
    }

    /**
     * Simple fullscreen quad.
     */
    private fun createVao() {
        vao = GL30C.glGenVertexArrays()
        val vbo = GL15C.glGenBuffers()
        GL30C.glBindVertexArray(vao)
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)
        val bb = BufferUtils.createByteBuffer(4 * 2 * 6)
        val fv = bb.asFloatBuffer()
        fv.put(-0.5f).put(-0.5f)
        fv.put(0.5f).put(-0.5f)
        fv.put(0.5f).put(0.5f)
        fv.put(0.5f).put(0.5f)
        fv.put(-0.5f).put(0.5f)
        fv.put(-0.5f).put(-0.5f)
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, bb, GL15C.GL_STATIC_DRAW)
        GL20C.glEnableVertexAttribArray(0)
        GL20C.glVertexAttribPointer(0, 2, GL11C.GL_FLOAT, false, 0, 0L)
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)
    }

    /**
     * Create the raster shader.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun createRasterProgram() {
        val program = GL20C.glCreateProgram()
        val vshader = createShader(
            "shaders/uniformarray-vs.glsl",
            GL20C.GL_VERTEX_SHADER
        )
        val fshader = createShader(
            "shaders/uniformarray-fs.glsl",
            GL20C.GL_FRAGMENT_SHADER
        )
        GL20C.glAttachShader(program, vshader)
        GL20C.glAttachShader(program, fshader)
        GL20C.glBindAttribLocation(program, 0, "position")
        GL30C.glBindFragDataLocation(program, 0, "color")
        GL20C.glLinkProgram(program)
        val linked = GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS)
        val programLog = GL20C.glGetProgramInfoLog(program)
        if (programLog.trim { it <= ' ' }.length > 0) {
            System.err.println(programLog)
        }
        if (linked == 0) {
            throw AssertionError("Could not link program")
        }
        this.program = program
    }

    /**
     * Initialize the shader program.
     */
    private fun initProgram() {
        GL20C.glUseProgram(program)
        vec3ArrayUniform = GL20C.glGetUniformLocation(program, "cols")
        chosenUniform = GL20C.glGetUniformLocation(program, "chosen")
        GL20C.glUseProgram(0)
    }

    private fun render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        GL20C.glUseProgram(program)

        /* Set uniform array. */GL20C.glUniform3fv(vec3ArrayUniform, colors)
        /* Set chosen color (index into array) */GL20C.glUniform1i(chosenUniform, chosen)
        GL30C.glBindVertexArray(vao)
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 6)
        GL30C.glBindVertexArray(0)
        GL20C.glUseProgram(0)
    }

    private fun loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents()
            GL11C.glViewport(0, 0, width, height)
            render()
            GLFW.glfwSwapBuffers(window)
        }
    }

    private fun run() {
        try {
            init()
            loop()
            errCallback!!.free()
            keyCallback!!.free()
            fbCallback!!.free()
            if (debugProc != null) debugProc!!.free()
            GLFW.glfwDestroyWindow(window)
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            GLFW.glfwTerminate()
        }
    }

    companion object {
        /**
         * Create a shader object from the given classpath resource.
         *
         * @param resource
         * the class path
         * @param type
         * the shader type
         *
         * @return the shader object id
         *
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun createShader(resource: String, type: Int): Int {
            val shader = GL20C.glCreateShader(type)
            val source: ByteBuffer = ioResourceToByteBuffer(resource, 8192)
            val strings = BufferUtils.createPointerBuffer(1)
            val lengths = BufferUtils.createIntBuffer(1)
            strings.put(0, source)
            lengths.put(0, source.remaining())
            GL20C.glShaderSource(shader, strings, lengths)
            GL20C.glCompileShader(shader)
            val compiled = GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS)
            val shaderLog = GL20C.glGetShaderInfoLog(shader)
            if (shaderLog.trim { it <= ' ' }.length > 0) {
                System.err.println(shaderLog)
            }
            if (compiled == 0) {
                throw AssertionError("Could not compile shader")
            }
            return shader
        }

        @JvmStatic
        fun main(args: Array<String>) {
            UniformArrayDemo().run()
        }
    }
}

private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
    val newBuffer = BufferUtils.createByteBuffer(newCapacity)
    buffer.flip()
    newBuffer.put(buffer)
    return newBuffer
}

@Throws(IOException::class)
fun ioResourceToByteBuffer(resource: String, bufferSize: Int): ByteBuffer {
    var buffer: ByteBuffer
    val url = Thread.currentThread().contextClassLoader.getResource(resource)
        ?: throw IOException("Classpath resource not found: $resource")
    val file = File(url.file)
    if (file.isFile) {
        val fis = FileInputStream(file)
        val fc = fis.channel
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
        fc.close()
        fis.close()
    } else {
        buffer = BufferUtils.createByteBuffer(bufferSize)
        val source = url.openStream() ?: throw FileNotFoundException(resource)
        try {
            val buf = ByteArray(8192)
            while (true) {
                val bytes = source.read(buf, 0, buf.size)
                if (bytes == -1) break
                if (buffer.remaining() < bytes) buffer = resizeBuffer(
                    buffer,
                    Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytes)
                )
                buffer.put(buf, 0, bytes)
            }
            buffer.flip()
        } finally {
            source.close()
        }
    }
    return buffer
}
