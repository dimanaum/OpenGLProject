#version 430

in vec3 tc;
in float altitude;
in vec3 vertEyeSpacePos;
out vec4 fragColor;

uniform int fogMode;
uniform mat4 v_matrix;
uniform mat4 proj_matrix;
uniform int isAbove;
layout (binding = 0) uniform samplerCube samp;

void main(void)
{
	vec4 fogColor = vec4(0.2, 0.2, 0.7, 1.0);	// bluish gray
	float fogStart = 0.2;
	float fogEnd = 0.8;

	float dist = length(vertEyeSpacePos.xyz);
	float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.4, 1.0);

	if (fogMode != 0)
	{
		if ((altitude < .47) && (isAbove == 0))
			fragColor = vec4(0,0,.2,1);
		else
			fragColor = mix(fogColor, texture(samp,tc), fogFactor);
	}
	else
	{
		if ((altitude < .47) && (isAbove == 0))
			fragColor = vec4(0,0,.2,1);
		else
		fragColor = texture(samp,tc);
	}
}
