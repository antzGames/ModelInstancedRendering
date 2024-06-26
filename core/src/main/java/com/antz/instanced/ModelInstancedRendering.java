package com.antz.instanced;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ModelInstancedRendering extends Game {
    @Override
    public void create() {
        //setScreen(new ModelInstancedRenderingBasicScreen()); // Original
        setScreen(new ModelInstancedRenderingPBRScreen()); // gdx-gltf + PBR Shaders
    }
}
