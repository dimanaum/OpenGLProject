#version 430

layout (location = 0) in vec3 position;
out vec3 tc;
out float altitude;
out vec3 vertEyeSpacePos;

uniform mat4 v_matrix;
uniform mat4 proj_matrix;
uniform int isAbove;
layout (binding = 0) uniform samplerCube samp;

void main(void)
{
	tc = position;
	mat4 v3_matrix = mat4(mat3(v_matrix));
	gl_Position = proj_matrix * v3_matrix * vec4(position,1.0);
	vertEyeSpacePos = (v_matrix * vec4(position.xyz, 1.0)).xyz;
	altitude = gl_Position.y;
}
