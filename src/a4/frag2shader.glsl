#version 430

in vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec, vertEyeSpacePos;
in vec4 shadow_coord;
out vec4 fragColor;

in vec3 originalVertex;

struct PositionalLight
{
	vec4 ambient, diffuse, specular;
	vec3 position;
};

struct Material
{
	vec4 ambient, diffuse, specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform int reflectMode;
uniform int bumpMode;
uniform int fogMode;
layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D samp;
layout (binding=2) uniform samplerCube t;
uniform float alpha;
uniform float flipNormal;

in vec2 tc;

void main(void)
{
	vec4 fogColor = vec4(0.2, 0.2, 0.7, 1.0);	// bluish gray
	float fogStart = 0.2;
	float fogEnd = 20;

	vec4 color;

	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-varyingVertPos);
	vec3 H = normalize(varyingHalfVec);

	if(bumpMode!=0)
	{
		float a = 5;		// controls depth of bumps
		float b = 6.0;	    // controls width of bumps
		float x = originalVertex.x;
		float y = originalVertex.y;
		float z = originalVertex.z;
		N.x = varyingNormal.x + a*sin(b*x);
		N.y = varyingNormal.y + a*sin(b*y);
		N.z = varyingNormal.z + a*sin(b*z);
		N = normalize(N);
	}

	vec3 r = -reflect(normalize(-varyingVertPos), normalize(varyingNormal));
	float cosPhi = dot(H,N);
	float notInShadow = textureProj(shadowTex, shadow_coord);
	color = globalAmbient * material.ambient + light.ambient * material.ambient;

	float dist = length(vertEyeSpacePos.xyz);
	float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.0, 1.0);

	if (notInShadow == 1.0)
	{
		color += light.diffuse * material.diffuse * max(dot(L,N),0.0)
		+ light.specular * material.specular
		* pow(max(dot(H,N),0.0),material.shininess*3.0);
	}

	if(reflectMode == 0)
	{
		if (fogMode != 0)
		{
			fragColor = mix(fogColor, (texture(samp, tc)*0.5) + (color*0.5), fogFactor);
			fragColor = vec4(fragColor.xyz, alpha);
		}
		else
		{
			fragColor = (texture(samp, tc)*0.5) + (color*0.5);
			fragColor = vec4(fragColor.xyz, alpha);
		}
	}
	else
	{
		if(fogMode != 0)
		{
			fragColor = mix(fogColor, texture(t, r)*0.5, fogFactor);
			fragColor = vec4(fragColor.xyz, alpha);
		}
		else
		{
			fragColor = texture(t,r)*0.5;
			fragColor = vec4(fragColor.xyz, alpha);
		}

	}
}
