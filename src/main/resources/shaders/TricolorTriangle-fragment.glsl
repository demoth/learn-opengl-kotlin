#version 330 core

out vec4 outColor;
in vec3 fragColor;


void main(void) {
    outColor = vec4(fragColor, 1.0);
}
