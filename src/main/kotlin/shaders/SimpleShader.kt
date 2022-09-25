/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package shaders

import org.lwjgl.glfw.Callbacks
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
import org.lwjgl.system.MemoryUtil.*

/**
 * Draw simple triangle using vertex & fragment shader.
 * Based on the UniformArrayDemo from lwjgl demos
 */
open class SimpleShader(private val vertexShader: String, private val fragmentShader: String) {
    protected var window: Long = 0
    protected var width = 1024
    protected var height = 768
    protected var vao = 0
    protected var program = 0

    var errCallback: GLFWErrorCallback? = null
    var debugProc: Callback? = null

    private fun init() {
        errCallback = GLFWErrorCallback.createPrint(System.err)
        GLFW.glfwSetErrorCallback(errCallback)

        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)

        window = GLFW.glfwCreateWindow(width, height, "Simple shader example", NULL, NULL)
        check(window != NULL) { "Failed to create the GLFW window" }

        GLFW.glfwSetFramebufferSizeCallback(window, object : GLFWFramebufferSizeCallback() {
            override fun invoke(window: Long, newWidth: Int, newHeight: Int) {
                if (newWidth > 0 && newHeight > 0 && (width != newWidth || height != newHeight)) {
                    width = newWidth
                    height = newHeight
                }
            }
        })

        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (action == GLFW.GLFW_RELEASE && key == GLFW.GLFW_KEY_ESCAPE) {
                    GLFW.glfwSetWindowShouldClose(window, true)
                }
            }
        })

        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        check(videoMode != null) { "Could not get video mode" }
        GLFW.glfwSetWindowPos(window, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)

        MemoryStack.stackPush().use { stack ->
            val frameBufferSize = stack.mallocInt(2)
            GLFW.nglfwGetFramebufferSize(
                window,
                memAddress(frameBufferSize),
                memAddress(frameBufferSize) + 4
            )
            width = frameBufferSize[0]
            height = frameBufferSize[1]
        }
        GL.createCapabilities()
        debugProc = GLUtil.setupDebugMessageCallback()

        program = createRasterProgram(vertexShader, fragmentShader)
        vao = createVao()
    }

    private fun render() {
        GL11C.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        GL11C.glViewport(0, 0, width, height)

        GL20C.glUseProgram(program)
        GL30C.glBindVertexArray(vao)
        draw()

        // unbind
        GL30C.glBindVertexArray(0)
        GL20C.glUseProgram(0)

        GLFW.glfwSwapBuffers(window)

    }

    private fun loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents()
            render()
        }
    }

    fun run() {
        try {
            init()
            loop()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            // cleanup
            errCallback?.free()
            debugProc?.free()
            GLFW.glfwDestroyWindow(window)
            Callbacks.glfwFreeCallbacks(window)
            GLFW.glfwTerminate()
        }
    }

    open fun draw() = GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3)

    open fun createVao(): Int {
        val vao = GL30C.glGenVertexArrays()
        val vbo = GL15C.glGenBuffers()
        GL30C.glBindVertexArray(vao)
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)

        val data = floatArrayOf(
            -0.5f, -0.5f,
            0.5f, -0.5f,
            0.0f, 0.5f,
        )

        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_STATIC_DRAW)
        GL20C.glEnableVertexAttribArray(0)
        GL20C.glVertexAttribPointer(0, 2, GL11C.GL_FLOAT, false, 0, 0L)

        // unbind
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)

        return vao
    }

}

fun main() {
    SimpleShader("/shaders/SimpleShader-vertex.glsl", "/shaders/SimpleShader-fragment.glsl").run()
}
