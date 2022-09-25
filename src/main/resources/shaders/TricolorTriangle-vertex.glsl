#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 inColor;

out vec3 fragColor;

void main(void) {
	gl_Position = vec4(position, 1.0);
	fragColor = inColor;
}
