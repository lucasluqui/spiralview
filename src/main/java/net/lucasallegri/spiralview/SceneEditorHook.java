package net.lucasallegri.spiralview;

import com.threerings.tudey.tools.SceneEditor;

public class SceneEditorHook {

  public static void main(String[] args) {
    new SceneEditor(args.length > 0 ? args[0] : null).startup();
  }

}
