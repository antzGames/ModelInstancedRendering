/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Based on: ModelInstancedRenderingTest.java
 * https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/gles3/ModelInstancedRenderingTest.java
 * <p>
 * Author: Antz
 * https://antzgames.itch.io/modelinstancedrendering
 * <p>
 * July 2023
 * <p>
 * Learn OpenGL Instancing: https://learnopengl.com/Advanced-OpenGL/Instancing
 *
 ******************************************************************************/
package com.antz.instanced;

import com.antz.instanced.shader.MyPBRDepthShaderProvider;
import com.antz.instanced.shader.MyPBRShaderProvider;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public class ModelInstancedRenderingPBRScreen implements Screen {

    // gdx-gltf stuff
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene scene;
    private DirectionalShadowLight light;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;

    // Instance constants
    private int INSTANCE_COUNT_SIDE;
    private int INSTANCE_COUNT;
    private final float INSTANCE_SEPARATION_FACTOR = 9.0f;
    private static float CULLING_FACTOR;

    private SpriteBatch batch2D;
    private BitmapFont font;
    private PerspectiveCamera camera;
    private FirstPersonCameraController controller;
    private Matrix4 mat4;
    private Vector3 vec3Temp;
    private FloatBuffer offsets;
    private float[] floatTemp;
    private Frustum camFrustum;

    private int instanceUpdated;
    private long updateTime;
    private long renderTime;

    private final StringBuffer stringBuffer = new StringBuffer();
    private float size;
    private boolean rotateOn = false, showStats = true;

    @Override
    public void show() {
        // Check if for GL30 profile
        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("GLES 3.0 profile required for this test");
        }
        initGLTF();
        initInstances();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK, true);
        controller.update();
        checkUserInput();

        // rotate all instances that are close and in view
        long startTime = TimeUtils.nanoTime();
        update(delta);
        updateTime = TimeUtils.timeSinceNanos(startTime);

        // draw all instances
        //light.setCenter(camera.position);
        sceneManager.update(delta);
        sceneManager.render();
        renderTime = (long) (1f / Gdx.graphics.getFramesPerSecond()* 1000);

        // 2D stuff for stats text
        if (showStats) {
            batch2D.begin();
            drawStats();
            batch2D.end();
        }
    }

    private void update(float delta) {
        instanceUpdated = 0;
        if (!rotateOn) return; // no need to update matrix transform, so return

        offsets.position(0);

        // Everything you do in this loop will impact performance at high INSTANCE_COUNT
        for (int x = 0; x < INSTANCE_COUNT; x++) {
            int targetIndex = x * 16; // each instance uses 16 floats for matrix4

            /*

             Matrix4               TRANSPOSED
             0  1  2  3            0  4  8  x
             4  5  6  7            1  5  9  y
             8  9  10 11           2  6  10 z
             x  y  z  15           3  7  11 15

             */

            // get position of instance (x, y, z)
            // from the transposed Matrix4 - see above
            vec3Temp.set(offsets.get(targetIndex + 3), offsets.get(targetIndex + 7), offsets.get(targetIndex + 11));

            // Attempt culling if not within camera's frustum, or too far away to be noticed rotating
            if (!(camFrustum.sphereInFrustum(vec3Temp, size*2)) || vec3Temp.dst(camera.position) > CULLING_FACTOR) continue;

            // Get the transpose maxtrix4
            floatTemp[0] = offsets.get(targetIndex);
            floatTemp[1] = offsets.get(targetIndex + 1);
            floatTemp[2] = offsets.get(targetIndex + 2);
            floatTemp[3] = offsets.get(targetIndex + 3);
            floatTemp[4] = offsets.get(targetIndex + 4);
            floatTemp[5] = offsets.get(targetIndex + 5);
            floatTemp[6] = offsets.get(targetIndex + 6);
            floatTemp[7] = offsets.get(targetIndex + 7);
            floatTemp[8] = offsets.get(targetIndex + 8);
            floatTemp[9] = offsets.get(targetIndex + 9);
            floatTemp[10] = offsets.get(targetIndex + 10);
            floatTemp[11] = offsets.get(targetIndex + 11);
            floatTemp[12] = offsets.get(targetIndex + 12);
            floatTemp[13] = offsets.get(targetIndex + 13);
            floatTemp[14] = offsets.get(targetIndex + 14);
            floatTemp[15] = offsets.get(targetIndex + 15);

            instanceUpdated++;
            mat4.set(floatTemp);
            mat4 = mat4.tra(); // convert back to regular Matrix4

            // spin every other cube differently
            if (x % 2 == 0)
                mat4.rotate(Vector3.X, 45 * delta);
            else
                mat4.rotate(Vector3.Z, 45 * delta);

            // update float buffer and update the mesh instance data
            offsets.position(targetIndex);
            offsets.put(mat4.tra().getValues()); // **** >>>> transpose again
            scene.modelInstance.model.nodes.first().parts.first().meshPart.mesh.updateInstanceData(targetIndex, mat4.getValues());
        }
    }

    private void drawStats() {
        stringBuffer.setLength(0);
        stringBuffer.append("x:").append((int)camera.position.x).append(", y:").append((int)camera.position.y).append(", z:").append((int)camera.position.z);

        font.draw(batch2D,"WASD + mouse drag: camera, F1: Toggle stats, SPACE: Toggle rotation. rotation=" + rotateOn, 10, 40);
        font.draw(batch2D,"3D Cubes: " + INSTANCE_COUNT + "  Matrix4 Updated: " + instanceUpdated + "   Matrix4 Skipped: " + (INSTANCE_COUNT - instanceUpdated), 10, 80);
        font.draw(batch2D,"Update Time: " + TimeUtils.nanosToMillis(updateTime) + "ms   Total Render Time: " + renderTime + "ms", 10, 120);
        font.draw(batch2D,"FPS: " + Gdx.graphics.getFramesPerSecond() + "  Camera Position: " + stringBuffer,10, 160);
    }

    private void checkUserInput() {
        // toggle rotation if space key pressed (or screen touched on Android)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || (Gdx.app.getType().equals(Application.ApplicationType.Android) && Gdx.input.isTouched()))
            rotateOn = !rotateOn;

        // toggle show stats if F1 key pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1))
            showStats = !showStats;
    }

    private void initInstances() {
        // 4 x 4 = 16 floats = Matrix4
        scene.modelInstance.model.meshes.first().enableInstancedRendering(true, INSTANCE_COUNT,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3)
        );

        // Create offset FloatBuffer that will contain instance data to pass to shader
        offsets = BufferUtils.newFloatBuffer(INSTANCE_COUNT * 16);

        // fill instance data buffer
        for (int x = 1; x <= INSTANCE_COUNT_SIDE; x++) {
            for (int y = 1; y <= INSTANCE_COUNT_SIDE; y++) {
                for (int z = 1; z <= INSTANCE_COUNT_SIDE; z++) {

                    // sets the position of each instance
                    vec3Temp.set(x*size*INSTANCE_SEPARATION_FACTOR,
                        y*size*INSTANCE_SEPARATION_FACTOR,
                        z*size*INSTANCE_SEPARATION_FACTOR);

                    mat4.set(scene.modelInstance.transform);

                    // This randomly sets a random rotation to a random axis
                    int rand = MathUtils.random(2);
                    if (rand == 0)
                        mat4.setToRotationRad(Vector3.X, MathUtils.random(0.0f, (float)Math.PI*2.0f));
                    else if (rand == 1)
                        mat4.setToRotationRad(Vector3.Y, MathUtils.random(0.0f, (float)Math.PI*2.0f));
                    else
                        mat4.setToRotationRad(Vector3.Z, MathUtils.random(0.0f, (float)Math.PI*2.0f));

                    mat4.setTranslation(vec3Temp);
                    offsets.put(mat4.tra().getValues()); // notice transpose here
                }
            }
        }

        ((Buffer) offsets).position(0);
        scene.modelInstance.model.meshes.first().setInstanceData(offsets);
        floatTemp = new float[INSTANCE_COUNT*16];
    }

    private void initGLTF() {
        // Catch Browser keys
        Gdx.input.setCatchKey(Input.Keys.SPACE, true);
        Gdx.input.setCatchKey(Input.Keys.F1, true);

        // reusable variables
        mat4 = new Matrix4();
        vec3Temp = new Vector3();

        // Same shaders as Duck Field Demo
        sceneManager = new SceneManager(new MyPBRShaderProvider(), new MyPBRDepthShaderProvider());

        // gdx-gltf set up
        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f/512f));

        // setup light
        // set the light parameters so that your area of interest is in the shadow light frustum
        // but keep it reasonably tight to keep sharper shadows
        Vector3 lightPosition = new Vector3(0,35,0);    // even though this is a directional light and is "infinitely far away", use this to set the near plane
        float farPlane = 100;
        float nearPlane = 0;
        float VP_SIZE = 70f;

        int SHADOW_MAP_SIZE = 8192;
        light = new DirectionalShadowLight(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE).setViewport(VP_SIZE, VP_SIZE, nearPlane, farPlane);
        light.direction.set(1, -1, 1);
        light.direction.nor();
        light.color.set(Color.WHITE);
        light.intensity = 2.8f;

        // for the directional shadow light we can set the light centre which is the center of the frustum of the orthogonal camera
        // that is used to create the depth buffer.
        // calculate the centre from the light position, near and far planes and light direction
        float halfDepth = (nearPlane + farPlane)/2f;
        Vector3 lightCentre = new Vector3();
        lightCentre.set(light.direction).scl(halfDepth).add(lightPosition);
        light.setCenter(lightCentre);           // set the centre of the frustum box

        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(0.25f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        // batch
        batch2D = new SpriteBatch();

        // until they fix the default font, load the fixed version locally
        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        font.setColor(Color.GREEN);
        font.getData().setScale(2);

        // set low instance limits for all other platforms not desktop
        // >>> always use an odd number so camera is not inside a cube
        if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
            INSTANCE_COUNT_SIDE = 101; // 1M+ cubes
        } else {
            INSTANCE_COUNT_SIDE = 29; // about 25k cubes
        }

        // size of box
        size = 2f; // look in blender at the size of your model, mine is 2x2x2 meters

        // setup camera
        camera = new PerspectiveCamera(45, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = INSTANCE_COUNT_SIDE * INSTANCE_SEPARATION_FACTOR * size * 2;

        // used for frustum
        if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
            CULLING_FACTOR = camera.far * 0.25f; // cull very small cubes that we cant detect rotating
        } else {
            CULLING_FACTOR = camera.far; // cull very small cubes that we cant detect rotating
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        // Set camera to center of cube field
        camera.position.set(
            INSTANCE_COUNT_SIDE * INSTANCE_SEPARATION_FACTOR * size/2f,
            INSTANCE_COUNT_SIDE * INSTANCE_SEPARATION_FACTOR * size/2f,
            INSTANCE_COUNT_SIDE * INSTANCE_SEPARATION_FACTOR * size/2f);

        camera.direction.set(Vector3.Z);
        camera.up.set(Vector3.Y);
        camera.update();
        camFrustum = camera.frustum;

        sceneManager.setCamera(camera);

        // Try it with your model!  Zebra are over rated anyhow!
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("graphics/zebra.gltf"));
        scene = new Scene(sceneAsset.scene);
        sceneManager.addScene(scene);

        // 101 * 101 * 101 = 1.03 million for desktop
        INSTANCE_COUNT = INSTANCE_COUNT_SIDE * INSTANCE_COUNT_SIDE * INSTANCE_COUNT_SIDE;

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(size*16); // you can change the speed
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose () {
        batch2D.dispose();
        font.dispose();
        sceneAsset.dispose();
        sceneManager.dispose();
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }
}
