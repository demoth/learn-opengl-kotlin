package shaders

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

/**
 * Draw elements using indices array
 */
class ElementArray : SimpleShader(
    "/shaders/SimpleShader-vertex.glsl",
    "/shaders/SimpleShader-fragment.glsl"
) {
    override fun draw() {
        GL11C.glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
    }

    override fun createVao(): Int {
        val vao = GL30C.glGenVertexArrays()
        GL30C.glBindVertexArray(vao)

        val vbo = GL15C.glGenBuffers()
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)
        val vertices = floatArrayOf(
            0.5f, 0.5f,
            0.5f, -0.5f,
            -0.5f, -0.5f,
            -0.5f, 0.5f,
        )

        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW)
        GL20C.glEnableVertexAttribArray(0)
        GL20C.glVertexAttribPointer(0, 2, GL11C.GL_FLOAT, false, 0, 0L)

        val ebo = GL15C.glGenBuffers()
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indices = intArrayOf(
            0, 1, 3,
            1, 2, 3
        )
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_STATIC_DRAW)
        //GL11C.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        // unbind
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)

        return vao
    }
}

fun main() {
    ElementArray().run()
}
