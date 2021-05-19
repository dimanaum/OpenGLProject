#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 tex_coord;
layout (location = 2) in vec3 vertNormal;

out vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec;
out vec4 shadow_coord;

out vec3 originalVertex;

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

out vec3 vertEyeSpacePos;
out vec2 tc;
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform float alpha;
uniform float flipNormal;
layout (binding=0) uniform sampler2DShadow shadowTex;

void main(void)
{
	//output the vertex position to the rasterizer for interpolation
	varyingVertPos = (mv_matrix * vec4(vertPos,1.0)).xyz;

	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;

	if (flipNormal < 0) varyingNormal = -varyingNormal;

	// calculate the half vector (L+V)
	varyingHalfVec = (varyingLightDir-varyingVertPos).xyz;

	originalVertex = vertPos;

	shadow_coord = shadowMVP * vec4(vertPos,1.0);

	vertEyeSpacePos = (mv_matrix * vec4(vertPos.xyz, 1.0)).xyz;
	tc = tex_coord;
	gl_Position = proj_matrix * mv_matrix * vec4(vertPos,1.0);
}
