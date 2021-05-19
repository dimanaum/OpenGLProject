package a4;
import org.joml.Vector2f;

public class MouseKeeperTracker
{
    public Vector2f lastPos = new Vector2f();
    public Vector2f changePos = new Vector2f();
    public boolean isDragging = false;

    public void getUpdatePos(Vector2f newPos)
    {
        if(isDragging)
            newPos.sub(lastPos, changePos);
        else
            changePos.set(0, 0);

        lastPos.set(newPos);
    }

    public void dragStart(int x, int y)
    {
        lastPos.set(x, y);
        isDragging = true;
    }

    public void dragStop()
    {
        isDragging = false;
    }
}
