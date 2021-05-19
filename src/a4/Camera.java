package a4;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera
{
    private Vector3f uVect;
    private Vector3f vVect;
    private Vector3f nVect;
    private Vector3f cameraLoc;
    private Matrix4f viewMatrix;
    private Matrix4f posMatrix;

    private boolean visible;

    public Camera()
    {
        uVect = new Vector3f(1.0f, 0.0f, 0.0f);
        vVect = new Vector3f(0.0f, -1.0f, 0.0f);
        nVect = new Vector3f(0.0f, 0.0f, 1.0f);

        cameraLoc = new Vector3f(0.5f, 2.0f, 8.5f);

        visible = false;

        viewMatrix = new Matrix4f();
        posMatrix = new Matrix4f();
    }

    Matrix4f getViewMatrix() {
        Matrix4f tempMatrix = new Matrix4f();

        posMatrix.set(
                uVect.x, -vVect.x, nVect.x, 0,
                uVect.y, -vVect.y, nVect.y, 0,
                uVect.z, -vVect.z, nVect.z, 0,
                0.0f, 0.0f, 0.0f, 1.0f
        );

        viewMatrix.identity();

        viewMatrix.mul(posMatrix);
        viewMatrix.translate(-cameraLoc.x, -cameraLoc.y, -cameraLoc.z);

        return viewMatrix;
    }

    Vector3f getCameraLoc()
    {
        return cameraLoc;
    }

    float getX()
    {
        return cameraLoc.x;
    }

    float getY()
    {
        return cameraLoc.y;
    }

    float getZ()
    {
        return cameraLoc.z;
    }

    public void moveForward()
    {
        cameraLoc.add(-nVect.x * 0.5f, -nVect.y * 0.5f, -nVect.z * 0.5f);
    }

    public void moveBackward()
    {
        cameraLoc.add(nVect.x * 0.5f, nVect.y * 0.5f, nVect.z * 0.5f);
    }

    public void moveLeft()
    {
        cameraLoc.add(-uVect.x * 0.5f, -uVect.y * 0.5f, -uVect.z * 0.5f);
    }

    public void moveRight()
    {
        cameraLoc.add(uVect.x * 0.5f, uVect.y * 0.5f, uVect.z * 0.5f);
    }

    public void moveDown()
    {
        cameraLoc.add(-vVect.x * 0.5f, -vVect.y * 0.5f, -vVect.z * 0.5f);
    }

    public void moveUp()
    {
        cameraLoc.add(vVect.x * 0.5f, vVect.y * 0.5f, vVect.z * 0.5f);
    }

    public void rotateLeft()
    {
        uVect.rotateAxis((float) Math.toRadians(-1), 0, vVect.y, 0);
        nVect.rotateAxis((float) Math.toRadians(-1), 0, vVect.y, 0);
        vVect.rotateAxis((float) Math.toRadians(-1), 0, vVect.y, 0);
    }

    public void rotateRight()
    {
        uVect.rotateAxis((float) Math.toRadians(1), 0, vVect.y, 0);
        nVect.rotateAxis((float) Math.toRadians(1), 0, vVect.y, 0);
        vVect.rotateAxis((float) Math.toRadians(1), 0, vVect.y, 0);
    }

    public void rotateUp()
    {
        vVect.rotateAxis((float) Math.toRadians(1), uVect.x, uVect.y, uVect.z);
        nVect.rotateAxis((float) Math.toRadians(1), uVect.x, uVect.y, uVect.z);
    }

    public void rotateDown()
    {
        vVect.rotateAxis((float) Math.toRadians(-1), uVect.x, uVect.y, uVect.z);
        nVect.rotateAxis((float) Math.toRadians(-1), uVect.x, uVect.y, uVect.z);
    }

    public void toggleVisibility()
    {
        visible = !visible;
    }

    public boolean getVisible()
    {
        return visible;
    }
}
