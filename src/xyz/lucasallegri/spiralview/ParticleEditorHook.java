package xyz.lucasallegri.spiralview;

import com.threerings.config.ConfigManager;
import com.threerings.media.image.ColorPository;
import com.threerings.projectx.dungeon.tools.SpawnTableTool;
import com.threerings.projectx.tools.ProjectXConfigEditor;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;
import com.threerings.util.O;

public class ParticleEditorHook {
	
	public static void main(String[] args) {
		ResourceManager rsrcmgr = new ResourceManager("rsrc/");
		MessageManager msgmgr = new MessageManager("rsrc.i18n");
		O _msgmgr = new O("rsrc.i18n");
		ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
		new ProjectXConfigEditor(_msgmgr, cfgmgr, new ColorPository());
	}

}
