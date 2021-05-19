package a4;

import java.awt.event.*;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import java.util.Random;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener
{	private GLCanvas myCanvas;
	private int renderingProgramSURFACE, renderingProgramFLOOR, renderingProgramCubeMap;
	private int renderingProgram1, renderingProgram2;
	private int lineRenderProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[40];
	private float cameraHeight = 2.0f, cameraPitch = 15.0f;

	private int width = 800, height = 800;

	private float surfacePlaneHeight = -0.4f;
	private float floorPlaneHeight = -10.0f;

	private Vector3f initialLightLoc = new Vector3f(10.0f, 12.0f, -12.0f);

	private Camera camObj;
	private MouseKeeperTracker mouseTrack;

	private Model penguinObject;
	private Model beachObject;
	private Model palmTreeObject;
	private Model oceanObject;
	private Model boxObject;
	private Model surfBoardObject;
	private Model shadesObject;
	private Model ballObject;
	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int vLoc, mLoc, pLoc, nLoc, aboveLoc;
	private int mvLoc, projLoc, sLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private boolean lightsOn;
	private boolean fogOn;

	private Sphere mySphere = new Sphere(48);
	private int numSphereVertices;

	private int moonNormalMap;
	private int moonTexture;

	// white light properties
	private float[] globalAmbient = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
	private float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

	// gold material
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();

	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

	private int skyboxTexture;
	private int[] bufferId = new int[1];
	private int refractTextureId;
	private int reflectTextureId;
	private int refractFrameBuffer;
	private int reflectFrameBuffer;

	private int noiseHeight = 256;
	private int noiseWidth = 256;
	private int noiseDepth = 256;
	private double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
	private int noiseTexture;
	private Random random = new Random(5);
	private double PI = 3.1415926535;

	private float depthLookup = 0.0f;
	private int dOffsetLoc;
	private long lastTime = System.currentTimeMillis();

	public Code()
	{
		setTitle("Dmitriy - HW4");
		setSize(width, height);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();
		this.add(myCanvas);
		this.setVisible(true);

		configListeners();
		camObj = new Camera();
		mouseTrack = new MouseKeeperTracker();
		currentLightPos.set(initialLightLoc);
		lightsOn = true;
		fogOn = false;

		Animator animator = new Animator(myCanvas);
		animator.start();
	}
	
	// 3D Noise Texture section

	double smooth(double zoom, double x1, double y1, double z1)
	{
		//get fractional part of x, y, and z
		double fractX = x1 - (int) x1;
		double fractY = y1 - (int) y1;
		double fractZ = z1 - (int) z1;

		//neighbor values that wrap
		double x2 = x1 - 1; if (x2<0) x2 = (Math.round(noiseWidth / zoom)) - 1;
		double y2 = y1 - 1; if (y2<0) y2 = (Math.round(noiseHeight / zoom)) - 1;
		double z2 = z1 - 1; if (z2<0) z2 = (Math.round(noiseDepth / zoom)) - 1;

		//smooth the noise by interpolating
		double value = 0.0;
		value += fractX       * fractY       * fractZ       * noise[(int)x1][(int)y1][(int)z1];
		value += (1.0-fractX) * fractY       * fractZ       * noise[(int)x2][(int)y1][(int)z1];
		value += fractX       * (1.0-fractY) * fractZ       * noise[(int)x1][(int)y2][(int)z1];	
		value += (1.0-fractX) * (1.0-fractY) * fractZ       * noise[(int)x2][(int)y2][(int)z1];
				
		value += fractX       * fractY       * (1.0-fractZ) * noise[(int)x1][(int)y1][(int)z2];
		value += (1.0-fractX) * fractY       * (1.0-fractZ) * noise[(int)x2][(int)y1][(int)z2];
		value += fractX       * (1.0-fractY) * (1.0-fractZ) * noise[(int)x1][(int)y2][(int)z2];
		value += (1.0-fractX) * (1.0-fractY) * (1.0-fractZ) * noise[(int)x2][(int)y2][(int)z2];
		
		return value;
	}

	double turbulence(double x, double y, double z, double maxZoom)
	{	double sum = 0.0, zoom = maxZoom;
	
		sum = (Math.sin((1.0/512.0)*(8*PI)*(x+z-4*y)) + 1) * 8.0;
		while(zoom >= 0.9)
		{	sum = sum + smooth(zoom, x/zoom, y/zoom, z/zoom) * zoom;
			zoom = zoom / 2.0;
		}
		sum = 128.0 * sum/maxZoom;
		return sum;
	}

	void fillDataArray(byte data[])
	{	double maxZoom = 32.0;
		for (int i=0; i<noiseWidth; i++)
		{	for (int j=0; j<noiseHeight; j++)
			{	for (int k=0; k<noiseDepth; k++)
				{	noise[i][j][k] = random.nextDouble();
		}	}	}
		for (int i = 0; i<noiseHeight; i++)
		{	for (int j = 0; j<noiseWidth; j++)
			{	for (int k = 0; k<noiseDepth; k++)
				{	data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+0] = (byte)turbulence(i,j,k,maxZoom);
					data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+1] = (byte)turbulence(i,j,k,maxZoom);
					data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+2] = (byte)turbulence(i,j,k,maxZoom);
					data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+3] = (byte)255;
	}	}	}	}

	private int buildNoiseTexture()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();

		byte[] data = new byte[noiseWidth*noiseHeight*noiseDepth*4];
		
		fillDataArray(data);

		ByteBuffer bb = Buffers.newDirectByteBuffer(data);

		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];

		gl.glBindTexture(GL_TEXTURE_3D, textureID);

		gl.glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
		gl.glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0,
				noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_BYTE, bb);
	
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		return textureID;
	}

	void createReflectRefractBuffers()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
	
		// Initialize Reflect Framebuffer
		gl.glGenFramebuffers(1, bufferId, 0);
		reflectFrameBuffer = bufferId[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, reflectFrameBuffer);
		gl.glGenTextures(1, bufferId, 0);
		reflectTextureId = bufferId[0];
		gl.glBindTexture(GL_TEXTURE_2D, reflectTextureId);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, reflectTextureId, 0);
		gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
		gl.glGenTextures(1, bufferId, 0);
		gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);

		// Initialize Refract Framebuffer
		gl.glGenFramebuffers(1, bufferId, 0);
		refractFrameBuffer = bufferId[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, refractFrameBuffer);
		gl.glGenTextures(1, bufferId, 0);
		refractTextureId = bufferId[0];
		gl.glBindTexture(GL_TEXTURE_2D, refractTextureId);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, refractTextureId, 0);
		gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
		gl.glGenTextures(1, bufferId, 0);
		gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, myCanvas.getWidth(), myCanvas.getHeight(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);
	}

	void prepForSkyBoxRender()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int fogPtr;

		gl.glUseProgram(renderingProgramCubeMap);

		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "proj_matrix");
		aboveLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "isAbove");

		fogPtr = gl.glGetUniformLocation(renderingProgramCubeMap, "fogMode");
				
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		if(fogOn)
			gl.glProgramUniform1i(renderingProgramCubeMap, fogPtr, 1);
		else
			gl.glProgramUniform1i(renderingProgramCubeMap, fogPtr, 0);

		gl.glUniform1i(aboveLoc, 1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
	}

	void prepForTopSurfaceRender()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int fogPtr;

		gl.glUseProgram(renderingProgramSURFACE);

		mLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "norm_matrix");
		aboveLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "isAbove");
		mvLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "mv_matrix");

		fogPtr = gl.glGetUniformLocation(renderingProgramSURFACE, "fogMode");

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));

		mMat.translation(0.0f, surfacePlaneHeight, 100.0f);

		if(fogOn)
			gl.glProgramUniform1i(renderingProgramSURFACE, fogPtr, 1);
		else
			gl.glProgramUniform1i(renderingProgramSURFACE, fogPtr, 0);

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		installLights(renderingProgramSURFACE, vMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		if (cameraHeight >= surfacePlaneHeight)
			gl.glUniform1i(aboveLoc, 1);
		else
			gl.glUniform1i(aboveLoc, 0);

		dOffsetLoc = gl.glGetUniformLocation(renderingProgramSURFACE, "depthOffset");
		gl.glUniform1f(dOffsetLoc, depthLookup);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[32]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[33]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[34]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, reflectTextureId);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, refractTextureId);
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);

		gl.glUseProgram(renderingProgramSURFACE);
	}

	void prepForFloorRender()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int fogPtr;

		gl.glUseProgram(renderingProgramFLOOR);

		mLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "norm_matrix");
		aboveLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "isAbove");

		fogPtr = gl.glGetUniformLocation(renderingProgramFLOOR, "fogMode");

		if(fogOn)
			gl.glProgramUniform1i(renderingProgramFLOOR, fogPtr, 1);
		else
			gl.glProgramUniform1i(renderingProgramFLOOR, fogPtr, 0);
		
		mMat.translation(0.0f, floorPlaneHeight, 100.f);

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		installLights(renderingProgramFLOOR, vMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		if (cameraHeight >= surfacePlaneHeight)
			gl.glUniform1i(aboveLoc, 1);
		else
			gl.glUniform1i(aboveLoc, 0);

		dOffsetLoc = gl.glGetUniformLocation(renderingProgramFLOOR, "depthOffset");
		gl.glUniform1f(dOffsetLoc, depthLookup);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[32]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[33]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[34]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);
	}

	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClearColor(0.7f, 0.8f, 0.9f, 1.0f); // background fog color is bluish-grey
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		matAmb = Utils.purpleAmbient();
		matDif = Utils.purpleDiffuse();
		matSpe = Utils.purpleSpecular();
		matShi = Utils.purpleShininess();

		cameraHeight = camObj.getCameraLoc().y;

		mvLoc = gl.glGetUniformLocation(renderingProgram1, "mv_matrix");
		projLoc = gl.glGetUniformLocation(renderingProgram1, "proj_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram1, "norm_matrix");

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - lastTime;
		lastTime = currentTime;

		depthLookup += (float)elapsedTime * .0001f;

		// render reflection scene to reflection buffer ----------------

		if (cameraHeight >= surfacePlaneHeight)
		{
			vMat.translation(0.0f, -(surfacePlaneHeight - cameraHeight), 0.0f);
			vMat.rotateX((float)Math.toRadians(-cameraPitch));
			vMat.set(camObj.getViewMatrix());

			gl.glBindFramebuffer(GL_FRAMEBUFFER, reflectFrameBuffer);
			gl.glClear(GL_DEPTH_BUFFER_BIT);
			gl.glClear(GL_COLOR_BUFFER_BIT);
			prepForSkyBoxRender();
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);	// cube is CW, but we are viewing the inside
			gl.glDisable(GL_DEPTH_TEST);
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			gl.glEnable(GL_DEPTH_TEST);
		}

		// render refraction scene to refraction buffer ----------------------------------------
		vMat.translation(0.0f, -(-(surfacePlaneHeight - cameraHeight)), 0.0f);
		vMat.rotateX((float)Math.toRadians(cameraPitch));
		vMat.set(camObj.getViewMatrix());

		gl.glBindFramebuffer(GL_FRAMEBUFFER, refractFrameBuffer);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);

		if (cameraHeight >= surfacePlaneHeight)
		{
			prepForFloorRender();
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);
			gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		}
		else
		{
			prepForSkyBoxRender();
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);	// cube is CW, but we are viewing the inside
			gl.glDisable(GL_DEPTH_TEST);
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			gl.glEnable(GL_DEPTH_TEST);
		}

		// now render the entire scene #####################################

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);

		// draw cube map

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);
		prepForSkyBoxRender();
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	// cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		// draw water top (surface) ======================

		prepForTopSurfaceRender();

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		if (cameraHeight >= surfacePlaneHeight)
			gl.glFrontFace(GL_CCW);
		else
			gl.glFrontFace(GL_CW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		// draw water bottom (floor) =========================

		prepForFloorRender();
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		lightVmat.identity().setLookAt(currentLightPos, origin, up);	// vector from light to origin
		lightPmat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);

		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	//  for reducing
		gl.glPolygonOffset(3.0f, 5.0f);		//  shadow artifacts

		passOne();

		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);

		gl.glDrawBuffer(GL_FRONT);

		passTwo();
	}

	public void passOne()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(renderingProgram1);

		mvLoc = gl.glGetUniformLocation(renderingProgram1, "mv_matrix");
		projLoc = gl.glGetUniformLocation(renderingProgram1, "proj_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram1, "norm_matrix");

		// draw the penguin

		mMat.identity();
		mMat.translate(-0.5f, -0.25f, 0.0f);
		mMat.rotateXYZ(0.0f, 3.0f, 0.0f);
		mMat.scale(0.15f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, penguinObject.getVertCount());

		// draw the palm tree

		mMat.identity();
		mMat.scale(0.8f);
		mMat.translate(1.5f, -0.5f, 0.0f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, palmTreeObject.getVertCount());

		// draw the surfboard

		mMat.identity();
		mMat.scale(0.2f);
		mMat.rotateXYZ(1.55f, 0.15f, 0.0f);
		mMat.translate(-6.5f, 0.0f, -6.5f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());

		// draw the surfboard

		mMat.identity();
		mMat.scale(0.2f);
		mMat.rotateXYZ(1.55f, 0.15f, 0.0f);
		mMat.translate(-6.5f, 0.0f, -6.5f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());

		// draw the ball

		mMat.identity();
		mMat.scale(0.06f);
		mMat.translate(5.0f, 0.0f, 20.0f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[25]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());
	}

	public void passTwo()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int reflectPtr;
		int bumpPtr;
		int fogPtr;
		int alphaLoc;
		int flipLoc;

		reflectPtr = gl.glGetUniformLocation(renderingProgram2, "reflectMode");
		bumpPtr = gl.glGetUniformLocation(renderingProgram2, "bumpMode");
		fogPtr = gl.glGetUniformLocation(renderingProgram2, "fogMode");
		alphaLoc = gl.glGetUniformLocation(renderingProgram2, "alpha");
		flipLoc = gl.glGetUniformLocation(renderingProgram2, "flipNormal");

		gl.glProgramUniform1f(renderingProgram2, alphaLoc, 1.0f);
		gl.glProgramUniform1f(renderingProgram2, flipLoc, 1.0f);

		if(fogOn)
			gl.glProgramUniform1i(renderingProgram2, fogPtr, 1);
		else
			gl.glProgramUniform1i(renderingProgram2, fogPtr, 0);

		vMat.identity();
		vMat.setTranslation(camObj.getX(), camObj.getY(), camObj.getZ());

		gl.glUseProgram(renderingProgram2);

		mvLoc = gl.glGetUniformLocation(renderingProgram2, "mv_matrix");
		projLoc = gl.glGetUniformLocation(renderingProgram2, "proj_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");

		// draw the penguin

		matAmb = Utils.goldAmbient();
		matDif = Utils.goldDiffuse();
		matSpe = Utils.goldSpecular();
		matShi = Utils.goldShininess();

		vMat.set(camObj.getViewMatrix());

		mMat.identity();
		mMat.translate(-0.5f, -0.25f, 0.0f);
		mMat.rotateXYZ(0.0f, 3.0f, 0.0f);
		mMat.scale(0.15f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, penguinObject.getTexture());

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, penguinObject.getVertCount());

		// draw the beach

		mMat.identity();
		mMat.scale(0.25f);
		mMat.translate(0.0f, -2.5f, 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, beachObject.getTexture());

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, beachObject.getVertCount());

		// draw the palm tree

		mMat.identity();
		mMat.scale(0.8f);
		mMat.translate(1.5f, -0.5f, 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, palmTreeObject.getTexture());

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, palmTreeObject.getVertCount());

		// draw the shades

		mMat.identity();
		mMat.scale(0.055f);
		mMat.rotateXYZ(0.0f, -0.15f, 0.0f);
		mMat.translate(-9.0f, 22.0f, 12.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[21]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glProgramUniform1i(renderingProgram2, reflectPtr, 1);
		gl.glDrawArrays(GL_TRIANGLES, 0, shadesObject.getVertCount());
		gl.glProgramUniform1i(renderingProgram2, reflectPtr, 0);

		// draw the surfboard

		mMat.identity();
		mMat.scale(0.2f);
		mMat.rotateXYZ(1.55f, 0.15f, 0.0f);
		mMat.translate(-6.5f, 0.0f, -6.5f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		gl.glProgramUniform1f(renderingProgram2, alphaLoc, 1.0f);
		gl.glProgramUniform1f(renderingProgram2, flipLoc, 1.0f);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[23]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, surfBoardObject.getTexture());

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		if(!fogOn)
		{
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);

			gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());
		}
		else
		{
			// 2-pass rendering a transparent version of the surfboard

			gl.glEnable(GL_BLEND);
			gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			gl.glBlendEquation(GL_FUNC_ADD);

			gl.glEnable(GL_CULL_FACE);

			gl.glCullFace(GL_FRONT);
			gl.glProgramUniform1f(renderingProgram2, alphaLoc, 0.2f);
			gl.glProgramUniform1f(renderingProgram2, flipLoc, -1.0f);
			gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());

			gl.glCullFace(GL_BACK);
			gl.glProgramUniform1f(renderingProgram2, alphaLoc, 0.2f);
			gl.glProgramUniform1f(renderingProgram2, flipLoc, 1.0f);
			gl.glDrawArrays(GL_TRIANGLES, 0, surfBoardObject.getVertCount());

			gl.glDisable(GL_BLEND);
		}

		// draw the ball

		mMat.identity();
		mMat.scale(0.06f);
		mMat.translate(5.0f, 0.0f, 20.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[25]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, ballObject.getTexture());

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[27]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glProgramUniform1i(renderingProgram2, bumpPtr, 1);
		gl.glDrawArrays(GL_TRIANGLES, 0, ballObject.getVertCount());
		gl.glProgramUniform1i(renderingProgram2, bumpPtr, 0);

		// draw the boxobj

		if(lightsOn)
		{
			// draw the boxobj

			mMat.identity();
			mMat.scale(0.7f);
			mMat.translate(currentLightPos.x, currentLightPos.y, currentLightPos.z);

			mvMat.identity();
			mvMat.mul(vMat);
			mvMat.mul(mMat);
			mvMat.invert(invTrMat);
			invTrMat.transpose(invTrMat);

			shadowMVP2.identity();
			shadowMVP2.mul(b);
			shadowMVP2.mul(lightPmat);
			shadowMVP2.mul(lightVmat);
			shadowMVP2.mul(mMat);

			mvMat.invert(invTrMat);
			invTrMat.transpose(invTrMat);

			gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
			gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
			gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
			gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[35]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[36]);
			gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(1);
			gl.glActiveTexture(GL_TEXTURE1);
			gl.glBindTexture(GL_TEXTURE_2D, moonTexture);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[37]);
			gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(2);

			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);

			gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVertices);
		}

		if(camObj.getVisible())
		{
			mMat.identity();
			mMat.scale(2.0f);
			mMat.translate(1.5f, -0.2f, 0.0f);

			mvMat.identity();
			mvMat.mul(vMat);
			mvMat.mul(mMat);
			mvMat.invert(invTrMat);
			invTrMat.transpose(invTrMat);

			mvLoc = gl.glGetUniformLocation(lineRenderProgram, "mv_matrix");
			projLoc = gl.glGetUniformLocation(lineRenderProgram, "proj_matrix");
			gl.glUseProgram(lineRenderProgram);
			gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
			gl.glUniformMatrix4fv(mvLoc, 1, false, vMat.get(vals));
			gl.glDrawArrays(GL_LINES, 0, 6);
		}

		installLights(renderingProgram2, vMat);
	}

	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		renderingProgramSURFACE = Utils.createShaderProgram("src/a4/vertShaderSURFACE.glsl", "src/a4/fragShaderSURFACE.glsl");
		renderingProgramFLOOR = Utils.createShaderProgram("src/a4/vertShaderFLOOR.glsl", "src/a4/fragShaderFLOOR.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("src/a4/vertCShader.glsl", "src/a4/fragCShader.glsl");

		renderingProgram1 = Utils.createShaderProgram("src/a4/vert1shader.glsl", "src/a4/frag1shader.glsl");
		renderingProgram2 = Utils.createShaderProgram("src/a4/vert2shader.glsl", "src/a4/frag2shader.glsl");
		penguinObject = new Model("penguin.obj", "src/a4/squareMoonMap.jpg");
		beachObject = new Model("beach.obj", "src/a4/sand.jpg");
		palmTreeObject = new Model("palmtree.obj", "src/a4/leaf3.jpeg");
		//oceanObject = new Model("ocean.obj", "src/a4/blue.jpg");
		//boxObject = new Model("box.obj", "src/a4/yellow.jpg");
		shadesObject = new Model("sunglasses.obj", "src/a4/black.jpg");
		surfBoardObject = new Model("surfboard.obj", "src/a4/swirls.jpg");
		ballObject = new Model("ball.obj", "src/a4/waves.jpg");
		lineRenderProgram = Utils.createShaderProgram("src/a4/axis.glsl", "src/a4/lineFrag.glsl");
		skyboxTexture = Utils.loadCubeMap("src/a4/cubeMap");

		moonTexture = Utils.loadTexture("src/a4/moon.jpg");
		moonNormalMap = Utils.loadTexture("src/a4/moonNORMAL.jpg");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();
		setupShadowBuffers();
		b.set(
				0.5f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.5f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.5f, 0.0f,
				0.5f, 0.5f, 0.5f, 1.0f);

		skyboxTexture = Utils.loadCubeMap("src/a4/cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		createReflectRefractBuffers();

		noiseTexture = buildNoiseTexture();
	}

	private void installLights(int renderingProgram, Matrix4f vMatrix)
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();

		Vector3f lightLocationTemp = new Vector3f(currentLightPos);
		lightLocationTemp.mulPosition(vMatrix);
		lightPos[0] = lightLocationTemp.x(); lightPos[1] = lightLocationTemp.y(); lightPos[2] = lightLocationTemp.z();

		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	private void setupShadowBuffers()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();

		gl.glGenFramebuffers(1, shadowBuffer, 0);

		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
				scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		float[] cubeVertexPositions =
		{ -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
		float[] PLANE_POSITIONS = {
			-120.0f, 0.0f, -240.0f,  -120.0f, 0.0f, 0.0f,  120.0f, 0.0f, -240.0f,
			120.0f, 0.0f, -240.0f,  -120.0f, 0.0f, 0.0f,  120.0f, 0.0f, 0.0f
		};
		float[] PLANE_TEXCOORDS = {
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 0.0f,
			1.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f
		};
		float[] PLANE_NORMALS = {
			0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f
		};

		numSphereVertices = mySphere.getIndices().length;

		int[] indices = mySphere.getIndices();
		Vector3f[] vertices = mySphere.getVertices();
		Vector2f[] texCoords = mySphere.getTexCoords();
		Vector3f[] normals = mySphere.getNormals();
		Vector3f[] tangents = mySphere.getTangents();

		float[] s_pvalues = new float[indices.length*3];
		float[] s_tvalues = new float[indices.length*2];
		float[] s_nvalues = new float[indices.length*3];
		float[] s_tanvalues = new float[indices.length*3];

		for (int i=0; i<indices.length; i++)
		{
			s_pvalues[i*3]   = (float) (vertices[indices[i]]).x();
			s_pvalues[i*3+1] = (float) (vertices[indices[i]]).y();
			s_pvalues[i*3+2] = (float) (vertices[indices[i]]).z();
			s_tvalues[i*2]   = (float) (texCoords[indices[i]]).x();
			s_tvalues[i*2+1] = (float) (texCoords[indices[i]]).y();
			s_nvalues[i*3]   = (float) (normals[indices[i]]).x();
			s_nvalues[i*3+1] = (float) (normals[indices[i]]).y();
			s_nvalues[i*3+2] = (float) (normals[indices[i]]).z();
			s_tanvalues[i*3] = (float) (tangents[indices[i]]).x();
			s_tanvalues[i*3+1] = (float) (tangents[indices[i]]).y();
			s_tanvalues[i*3+2] = (float) (tangents[indices[i]]).z();
		}

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(40, vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer penguinBuf = Buffers.newDirectFloatBuffer(penguinObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, penguinBuf.limit()*4, penguinBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer penguinTextBuf = Buffers.newDirectFloatBuffer(penguinObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, penguinTextBuf.limit()*4, penguinTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer penguinNormBuf = Buffers.newDirectFloatBuffer(penguinObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, penguinNormBuf.limit()*4, penguinNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer beachBuf = Buffers.newDirectFloatBuffer(beachObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, beachBuf.limit()*4, beachBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer beachTextBuf = Buffers.newDirectFloatBuffer(beachObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, beachTextBuf.limit()*4, beachTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer beachNormBuf = Buffers.newDirectFloatBuffer(beachObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, beachNormBuf.limit()*4, beachNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer palmTreeBuf = Buffers.newDirectFloatBuffer(palmTreeObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, palmTreeBuf.limit()*4, palmTreeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer palmTreeTextBuf = Buffers.newDirectFloatBuffer(palmTreeObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, palmTreeTextBuf.limit()*4, palmTreeTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		FloatBuffer palmTreeNormBuf = Buffers.newDirectFloatBuffer(palmTreeObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, palmTreeNormBuf.limit()*4, palmTreeNormBuf, GL_STATIC_DRAW);

//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
//		FloatBuffer oceanBuf = Buffers.newDirectFloatBuffer(oceanObject.getPValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, oceanBuf.limit()*4, oceanBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
//		FloatBuffer oceanTextBuf = Buffers.newDirectFloatBuffer(oceanObject.getTValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, oceanTextBuf.limit()*4, oceanTextBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
//		FloatBuffer oceanNormBuf = Buffers.newDirectFloatBuffer(oceanObject.getNValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, oceanNormBuf.limit()*4, oceanNormBuf, GL_STATIC_DRAW);

//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
//		FloatBuffer boxBuf = Buffers.newDirectFloatBuffer(boxObject.getPValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, boxBuf.limit()*4, boxBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[17]);
//		FloatBuffer boxTextBuf = Buffers.newDirectFloatBuffer(boxObject.getTValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, boxTextBuf.limit()*4, boxTextBuf, GL_STATIC_DRAW);
//
//		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
//		FloatBuffer boxNormBuf = Buffers.newDirectFloatBuffer(boxObject.getNValues());
//		gl.glBufferData(GL_ARRAY_BUFFER, boxNormBuf.limit()*4, boxNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
		FloatBuffer shadesBuf = Buffers.newDirectFloatBuffer(shadesObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, shadesBuf.limit()*4, shadesBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[20]);
		FloatBuffer shadesTextBuf = Buffers.newDirectFloatBuffer(shadesObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, shadesTextBuf.limit()*4, shadesTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[21]);
		FloatBuffer shadesNormBuf = Buffers.newDirectFloatBuffer(shadesObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, shadesNormBuf.limit()*4, shadesNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[22]);
		FloatBuffer surfBoardBuf = Buffers.newDirectFloatBuffer(surfBoardObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, surfBoardBuf.limit()*4, surfBoardBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[23]);
		FloatBuffer surfBoardTextBuf = Buffers.newDirectFloatBuffer(surfBoardObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, surfBoardTextBuf.limit()*4, surfBoardTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[24]);
		FloatBuffer surfBoardNormBuf = Buffers.newDirectFloatBuffer(surfBoardObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, surfBoardNormBuf.limit()*4, surfBoardNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[25]);
		FloatBuffer ballBuf = Buffers.newDirectFloatBuffer(ballObject.getPValues());
		gl.glBufferData(GL_ARRAY_BUFFER, ballBuf.limit()*4, ballBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[26]);
		FloatBuffer ballTextBuf = Buffers.newDirectFloatBuffer(ballObject.getTValues());
		gl.glBufferData(GL_ARRAY_BUFFER, ballTextBuf.limit()*4, ballTextBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[27]);
		FloatBuffer ballNormBuf = Buffers.newDirectFloatBuffer(ballObject.getNValues());
		gl.glBufferData(GL_ARRAY_BUFFER, ballNormBuf.limit()*4, ballNormBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[32]);
		FloatBuffer planeBuf = Buffers.newDirectFloatBuffer(PLANE_POSITIONS);
		gl.glBufferData(GL_ARRAY_BUFFER, planeBuf.limit()*4, planeBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[33]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(PLANE_TEXCOORDS);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[34]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(PLANE_NORMALS);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[35]);
		FloatBuffer sphereVertBuf = Buffers.newDirectFloatBuffer(s_pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereVertBuf.limit()*4, sphereVertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[36]);
		FloatBuffer sphereTexBuf = Buffers.newDirectFloatBuffer(s_tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereTexBuf.limit()*4, sphereTexBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[37]);
		FloatBuffer sphereNorBuf = Buffers.newDirectFloatBuffer(s_nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereNorBuf.limit()*4, sphereNorBuf, GL_STATIC_DRAW);
	}

	void toggleLights()
	{
		lightsOn = !lightsOn;

		if(lightsOn)
		{
			lightAmbient = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
			lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
			lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		}
		else
		{
			lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 0.0f };
			lightDiffuse = new float[] { 0.0f, 0.0f, 0.0f, 0.0f };
			lightSpecular = new float[] { 0.0f, 0.0f, 0.0f, 0.0f };
		}

	}

	void toggleFog()
	{
		fogOn = !fogOn;
	}

	//prep the keypress listeners
	private void configListeners()
	{
		myCanvas.addGLEventListener(this);
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();
		myCanvas.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent keyEvent) {}

			@Override
			public void keyPressed(KeyEvent keyEvent)
			{
				switch(keyEvent.getKeyChar())
				{
					case 'w':
						camObj.moveForward();
						break;
					case 's':
						camObj.moveBackward();
						break;
					case 'a':
						camObj.moveLeft();
						break;
					case 'd':
						camObj.moveRight();
						break;
					case 'e':
						camObj.moveUp();
						break;
					case 'q':
						camObj.moveDown();
						break;
					case 'l':
						toggleLights();
						break;
					case 'f':
						toggleFog();
						break;
					case ' ':
						camObj.toggleVisibility();
						break;
				}
			}

			@Override
			public void keyReleased(KeyEvent keyEvent) {}
		});

		myCanvas.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent keyEvent) {}

			@Override
			public void keyPressed(KeyEvent keyEvent)
			{
				switch(keyEvent.getKeyCode())
				{
					case KeyEvent.VK_UP:
						camObj.rotateUp();
						break;
					case KeyEvent.VK_DOWN:
						camObj.rotateDown();
						break;
					case KeyEvent.VK_LEFT:
						camObj.rotateLeft();
						break;
					case KeyEvent.VK_RIGHT:
						camObj.rotateRight();
						break;
				}
			}

			@Override
			public void keyReleased(KeyEvent keyEvent) {}
		});

		myCanvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) { }

			@Override
			public void mousePressed(MouseEvent e)
			{
				mouseTrack.dragStart(e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				mouseTrack.dragStop();
			}

			@Override
			public void mouseEntered(MouseEvent e) { }

			@Override
			public void mouseExited(MouseEvent e) { }
		});

		myCanvas.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e)
			{
				mouseTrack.getUpdatePos(new Vector2f(e.getX(), e.getY()));
				currentLightPos.add(mouseTrack.changePos.x*0.1f, 0, mouseTrack.changePos.y*-0.1f);
			}

			@Override
			public void mouseMoved(MouseEvent e) { }
		});

		myCanvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if(e.getWheelRotation() > 0)//down
					currentLightPos.y -= 1;
				else
					currentLightPos.y += 1;
			}
		});
	}

	public static void main(String[] args) { new Code(); }
	public void dispose(GLAutoDrawable drawable) {}
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		setupShadowBuffers();
		createReflectRefractBuffers();
	}
}