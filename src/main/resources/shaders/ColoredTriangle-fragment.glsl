#version 330 core

out vec4 outColor;
in vec3 inColor;

uniform vec3 triangleColor;

void main(void) {
    outColor = vec4(triangleColor, 1.0);
    //outColor = vec4(inColor, 1.0);
    //outColor = vec4(inColor.b, 1.0, 1.0, 1.0);
}
