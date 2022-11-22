package net.lucasallegri.spiralview;

import com.threerings.opengl.effect.tools.ParticleEditor;

public class ParticleEditorHook {
	
	public static void main(String[] args) {
		new ParticleEditor(args.length > 0 ? args[0] : null).startup();
	}

}
