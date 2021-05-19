package a4;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

public class Model
{
    private ImportedModel model;
    private int numObjVertices;
    private int texture;

    private float[] pvalues;
    private float[] tvalues;
    private float[] nvalues;

    FloatBuffer pTextBuf;

    public Model(String modelFile, String textureFile)
    {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        model = new ImportedModel(modelFile);
        texture = Utils.loadTexture(textureFile);

        numObjVertices = model.getNumVertices();
        Vector3f[] vertices = model.getVertices();
        Vector2f[] texCoords = model.getTexCoords();
        Vector3f[] normals = model.getNormals();

        pvalues = new float[numObjVertices*3];
        tvalues = new float[numObjVertices*2];
        nvalues = new float[numObjVertices*3];

        for (int i=0; i<numObjVertices; i++)
        {
            pvalues[i*3]   = (float) (vertices[i]).x();
            pvalues[i*3+1] = (float) (vertices[i]).y();
            pvalues[i*3+2] = (float) (vertices[i]).z();
            tvalues[i*2]   = (float) (texCoords[i]).x();
            tvalues[i*2+1] = (float) (texCoords[i]).y();
            nvalues[i*3]   = (float) (normals[i]).x();
            nvalues[i*3+1] = (float) (normals[i]).y();
            nvalues[i*3+2] = (float) (normals[i]).z();
        }

        pTextBuf = Buffers.newDirectFloatBuffer(tvalues);
    }

    FloatBuffer getBuffer() { return pTextBuf; }

    float[] getPValues() { return pvalues; }
    float[] getTValues() { return tvalues; }
    float[] getNValues() { return nvalues; }
    int getTexture() { return texture; }
    int getVertCount() { return numObjVertices; }

}
