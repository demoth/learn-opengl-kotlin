package shaders

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draw elements using indices array
 */
class ColoredTriangle : SimpleShader(
    "/shaders/ColoredTriangle-vertex.glsl",
    "/shaders/ColoredTriangle-fragment.glsl"
) {

    var colorUniform: Int = 0

    override fun draw() {
        val time = System.currentTimeMillis() / 500.0

        GL20C.glUniform3f(colorUniform, sin(time / 3f).toFloat(), sin(time / 2f).toFloat(), sin(time / 5f).toFloat());

        GL11C.glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_INT, 0)
    }

    override fun createVao(): Int {
        val vao = GL30C.glGenVertexArrays()
        GL30C.glBindVertexArray(vao)

        val vbo = GL15C.glGenBuffers()
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)
        val vertices = floatArrayOf(
            // position3, color3
            0f, 0.5f, 0.0f,
            1.0f, 0.0f, 0.0f,

            -0.5f, -0.5f, 0.0f,
            0.0f, 1.0f, 0.0f,

            0.5f, -0.5f, 0.0f,
            0.0f, 0.0f, 1.0f,
        )

        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW)
        // position
        GL20C.glVertexAttribPointer(0, 3, GL11C.GL_FLOAT, false, 24, 0)
        GL20C.glEnableVertexAttribArray(0)
        // color
        GL20C.glVertexAttribPointer(1, 3, GL11C.GL_FLOAT, false, 24, 12)
        GL20C.glEnableVertexAttribArray(1)


        val ebo = GL15C.glGenBuffers()
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indices = intArrayOf(0, 1, 2)
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_STATIC_DRAW)
        //GL11C.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        // unbind
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)

        colorUniform = GL20C.glGetUniformLocation(program, "triangleColor");

        return vao
    }
}

fun main() {
    ColoredTriangle().run()
}
