package shaders

import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

fun createRasterProgram(vertexSource: String, fragmentSource: String): Int {
    val program = GL20C.glCreateProgram()
    val vertexShader = createShader(vertexSource, GL20C.GL_VERTEX_SHADER)
    val fragmentShader = createShader(fragmentSource, GL20C.GL_FRAGMENT_SHADER)
    GL20C.glAttachShader(program, vertexShader)
    GL20C.glAttachShader(program, fragmentShader)
    GL20C.glBindAttribLocation(program, 0, "position")
    GL30C.glBindFragDataLocation(program, 0, "color")
    GL20C.glLinkProgram(program)
    val linkStatus = GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS)
    val programLog = GL20C.glGetProgramInfoLog(program)
    if (programLog.isNotEmpty()) {
        System.err.println(programLog)
    }
    check(linkStatus != 0) { "Could not link program" }
    return program
}

internal fun createShader(location: String, type: Int): Int {
    val shader = GL20C.glCreateShader(type)
    val resource = object{}::class.java.getResourceAsStream(location)
    checkNotNull(resource) { "Could not read $location" }
    GL20C.glShaderSource(shader, String(resource.readAllBytes()))
    GL20C.glCompileShader(shader)
    val compileStatus = GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS)
    val shaderLog = GL20C.glGetShaderInfoLog(shader)
    if (shaderLog.isNotEmpty()) {
        System.err.println(shaderLog)
    }
    check(compileStatus != 0) { "Could not compile shader" }
    return shader
}
