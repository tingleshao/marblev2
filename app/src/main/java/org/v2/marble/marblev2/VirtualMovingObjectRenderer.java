package org.v2.marble.marblev2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by chongshao on 10/2/17.
 */

public class VirtualMovingObjectRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {
    private static final String LOGTAG = "VirtualButtonRenderer";

    private SampleApplicationSession vuforiaAppSession;
    private SampleAppRenderer mSampleAppRenderer;

    private boolean mIsActive = false;

    private VirtualHandsActivity mActivity;

    private Vector<Texture> mTextures;

    private MeshObject mTeapot = new Hand();

    // TODO(chongshao): make it in one class
    private MeshObject hand1 = new Hand1();
    private MeshObject hand2 = new Hand2();
    private MeshObject hand3 = new Hand3();
    private MeshObject hand4 = new Hand4();
    private MeshObject hand5 = new Hand5();
    private MeshObject hand6 = new Hand6();
    private MeshObject hand7 = new Hand7();
    private MeshObject hand8 = new Hand8();
    private MeshObject hand9 = new Hand9();

    private int handIndex = 0;
    private static final int HAND_INDEX_LIMIT = 9;
    private float STEP_SIZE = 5f; // This number can be 1, 1.25, 1.67, 2.5, 5
    private static final float[] STEP_SIZE_LIST = {1f, 1.25f, 1.67f, 2.5f, 5f};
    private int stepSizeIndex = 0;

    ArrayList<MeshObject> hands = new ArrayList<>();

    // OpenGL ES 2.0 specific (3D model):
    private int shaderProgramID = 0;
    private int vertexHandle = 0;
    private int textureCoordHandle = 0;
    private int mvpMatrixHandle = 0;
    private int texSampler2DHandle = 0;

    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;

    // OpenGL ES 2.0 specific (Virtual Buttons):
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;

    // Goes through 0~8
    private int currObjIdx = 0;

    // Constants:
    static private float kTeapotScale = 0.003f;
    // static private float kTeapotScale = 0.03f;

    // Define the coordinates of the virtual buttons to render the area of action,
    // this values are the same as the wood dataset
    static private float RED_VB_BUTTON[] =  {-0.10868f, -0.05352f, -0.07575f, -0.06587f};
    static private float BLUE_VB_BUTTON[] =  {-0.04528f, -0.05352f, -0.01235f, -0.06587f};
    static private float YELLOW_VB_BUTTON[] =  {0.01482f, -0.05352f, 0.04775f, -0.06587f};
    static private float GREEN_VB_BUTTON[] =  {0.07657f, -0.05352f, 0.10950f, -0.06587f};

    //  float[] modelViewMatrix = {0.87753934f, 0.32194763f, -0.35535115f, 0.0f, 0.40119046f, -0.89885277f, 0.17638008f, 0.0f, -0.26262322f, -0.29734397f, -0.917941f, 0.0f, 0.0010600443f, 0.0148388855f, 0.19901177f, 1.0f} ;
    float[] modelViewMatrix = { 0.6972165f, 0.53568584f, -0.47637162f, 0.0f,
            0.7147522f, -0.5704037f, 0.4046838f, 0.0f,
            -0.05494073f, -0.62263995f, -0.78057736f, 0.0f,
            -0.003425967f, 0.0032251133f, 0.18447536f, 1.0f};
    float[] invM = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            -15.0f, -50.0f, -500.0f, 1.0f};

    public VirtualMovingObjectRenderer(VirtualHandsActivity activity,
                                SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);
        Matrix.invertM(invM, 0, modelViewMatrix, 0);
        hands.add(hand1);
        hands.add(hand2);
        hands.add(hand3);
        hands.add(hand4);
        hands.add(hand5);
        hands.add(hand6);
        hands.add(hand7);
        hands.add(hand8);
        hands.add(hand9);
    }

    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }
    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    private void initRendering()
    {
        Log.d(LOGTAG, "initRendering");

        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        // OpenGL setup for Virtual Buttons
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LineShaders.LINE_VERTEX_SHADER, LineShaders.LINE_FRAGMENT_SHADER);

        mvpMatrixButtonsHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "modelViewProjectionMatrix");
        vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID,
                "vertexPosition");
        lineOpacityHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "color");
    }


    public void rotate1(float delta_angle) {
        //   this.angle1 += delta_angle;
        Matrix.rotateM(invM, 0, invM, 0, delta_angle, 1, 0, 0);
        Matrix.invertM(modelViewMatrix, 0, invM, 0);

        //   updateM(true);
        //   this.displayAngles();
    }

    public void rotate2(float delta_angle) {
        Matrix.rotateM(invM, 0, invM, 0, delta_angle, 0, 1, 0);
        Matrix.invertM(modelViewMatrix, 0, invM, 0);

        //  updateM(true);
    }

    public void rotate3(float delta_angle) {
        Matrix.rotateM(invM, 0, invM, 0, delta_angle, 0, 0, 1);
        Matrix.invertM(modelViewMatrix, 0, invM, 0);

        //   updateM(true);
    }

    private void reset() {
        modelViewMatrix = new float[]{ 0.6972165f, 0.53568584f, -0.47637162f, 0.0f,
                0.7147522f, -0.5704037f, 0.4046838f, 0.0f,
                -0.05494073f, -0.62263995f, -0.78057736f, 0.0f,
                -0.003425967f, 0.0032251133f, 0.18447536f, 1.0f};
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        state.getNumTrackableResults();
        if (true) {
            currObjIdx++;
            if (currObjIdx == 5) {
                mTeapot = hands.get(handIndex);
                handIndex = (int)((float)handIndex + STEP_SIZE);
                if (handIndex >= HAND_INDEX_LIMIT) {
                    handIndex = 0;
                }
                //       reset(); // TODO(chongshao): this reset may be the thing makes it flashy
                currObjIdx = 0;
            }
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(0);
            reset();
            //   float[] modelViewMatrix = Tool.convertPose2GLMatrix(
            //           trackableResult.getPose()).getData();
            //  Log.d("DTL", String.valueOf(modelViewMatrix[0]) + " " +
//                         String.valueOf(modelViewMatrix[1]) + " " +
//                    String.valueOf(modelViewMatrix[2]) + " " +
//                    String.valueOf(modelViewMatrix[3]) + " " +
//                    String.valueOf(modelViewMatrix[4]) + " " +
//                    String.valueOf(modelViewMatrix[5]) + " " +
//                    String.valueOf(modelViewMatrix[6]) + " " +
//                    String.valueOf(modelViewMatrix[7]) + " " +
//                    String.valueOf(modelViewMatrix[8]) + " " +
//                    String.valueOf(modelViewMatrix[9]) + " " +
//                    String.valueOf(modelViewMatrix[10]) + " " +
//                    String.valueOf(modelViewMatrix[11]) + " " +
//                    String.valueOf(modelViewMatrix[12]) + " " +
//                    String.valueOf(modelViewMatrix[13]) + " " +
//                    String.valueOf(modelViewMatrix[14]) + " " +
//                    String.valueOf(modelViewMatrix[15]));

            // The image target specific result:
            ImageTargetResult imageTargetResult = (ImageTargetResult) trackableResult;

            // Set transformations:
            float[] modelViewProjection = new float[16];
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // Set the texture used for the teapot model:
            int textureIndex = 0;
//
            //          float vbVertices[] = new float[imageTargetResult
            //            .getNumVirtualButtons() * 24];
            //        short vbCounter = 0;

            // Iterate through this targets virtual buttons:
//            for (int i = 0; i < imageTargetResult.getNumVirtualButtons(); ++i)
//            {
//                VirtualButtonResult buttonResult = imageTargetResult
//                        .getVirtualButtonResult(i);
//                VirtualButton button = buttonResult.getVirtualButton();
//
//                int buttonIndex = 0;
//                // Run through button name array to find button index
//                for (int j = 0; j < VirtualHandsActivity.NUM_BUTTONS; ++j)
//                {
//                    if (button.getName().compareTo(
//                            mActivity.virtualButtonColors[j]) == 0)
//                    {
//                        buttonIndex = j;
//                        break;
//                    }
//                }
//
//                // If the button is pressed, then use this texture:
//                if (buttonResult.isPressed())
//                {
//                    textureIndex = buttonIndex + 1;
//                }
//
//                // Define the four virtual buttons as Rectangle using the same values as the dataset
//                Rectangle vbRectangle[] = new Rectangle[4];
//                vbRectangle[0] = new Rectangle(RED_VB_BUTTON[0], RED_VB_BUTTON[1],
//                        RED_VB_BUTTON[2], RED_VB_BUTTON[3]);
//                vbRectangle[1] = new Rectangle(BLUE_VB_BUTTON[0], BLUE_VB_BUTTON[1],
//                        BLUE_VB_BUTTON[2], BLUE_VB_BUTTON[3]);
//                vbRectangle[2] = new Rectangle(YELLOW_VB_BUTTON[0], YELLOW_VB_BUTTON[1],
//                        YELLOW_VB_BUTTON[2], YELLOW_VB_BUTTON[3]);
//                vbRectangle[3] = new Rectangle(GREEN_VB_BUTTON[0], GREEN_VB_BUTTON[1],
//                        GREEN_VB_BUTTON[2], GREEN_VB_BUTTON[3]);

            // We add the vertices to a common array in order to have one
            // single
            // draw call. This is more efficient than having multiple
            // glDrawArray calls
//                vbVertices[vbCounter] = vbRectangle[buttonIndex].getLeftTopX();
//                vbVertices[vbCounter + 1] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 2] = 0.0f;
//                vbVertices[vbCounter + 3] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 4] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 5] = 0.0f;
//                vbVertices[vbCounter + 6] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 7] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 8] = 0.0f;
//                vbVertices[vbCounter + 9] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 10] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 11] = 0.0f;
//                vbVertices[vbCounter + 12] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 13] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 14] = 0.0f;
//                vbVertices[vbCounter + 15] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 16] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 17] = 0.0f;
//                vbVertices[vbCounter + 18] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 19] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 20] = 0.0f;
//                vbVertices[vbCounter + 21] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 22] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 23] = 0.0f;
//                vbCounter += 24;

            //     }

            // We only render if there is something on the array
//            if (vbCounter > 0)
//            {
//                // Render frame around button
//                GLES20.glUseProgram(vbShaderProgramID);
//
//                GLES20.glVertexAttribPointer(vbVertexHandle, 3,
//                        GLES20.GL_FLOAT, false, 0, fillBuffer(vbVertices));
//
//                GLES20.glEnableVertexAttribArray(vbVertexHandle);
//
//                GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//                GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 1.0f);
//
//                GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false,
//                        modelViewProjection, 0);
//
//                // We multiply by 8 because that's the number of vertices per
//                // button
//                // The reason is that GL_LINES considers only pairs. So some
//                // vertices
//                // must be repeated.
//                GLES20.glDrawArrays(GLES20.GL_LINES, 0,
//                        imageTargetResult.getNumVirtualButtons() * 8);
//
//                SampleUtils.checkGLError("VirtualButtons drawButton");
//
//                GLES20.glDisableVertexAttribArray(vbVertexHandle);
//            }

            // Assumptions:
            Texture thisTexture = mTextures.get(textureIndex);

            // Scale 3D model
            Matrix.scaleM(modelViewMatrix, 0, kTeapotScale, kTeapotScale,
                    kTeapotScale);

            float[] modelViewProjectionScaled = new float[16];
            Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // Render 3D model
            GLES20.glUseProgram(shaderProgramID);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mTeapot.getVertices());
            //   GLES20.glVertexAttribPointer(textureCoordHandle, 2,
            //       GLES20.GL_FLOAT, false, 0, mTeapot.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            //        GLES20.glEnableVertexAttribArray(textureCoordHandle);

            // TODO(Chongshao): I disabled this
            //     GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            //     GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
            //             thisTexture.mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjectionScaled, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    mTeapot.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    mTeapot.getIndices());

            GLES20.glDisableVertexAttribArray(vertexHandle);
            // TODO(Chongshao): I disabled this
            //       GLES20.glDisableVertexAttribArray(textureCoordHandle);

            SampleUtils.checkGLError("VirtualButtons renderFrame");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();

    }


    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }

    public void updateFreq() {
        STEP_SIZE = STEP_SIZE_LIST[stepSizeIndex];
        stepSizeIndex++;

        if (stepSizeIndex ==5) {
            stepSizeIndex = 0;
        }
    }
}
