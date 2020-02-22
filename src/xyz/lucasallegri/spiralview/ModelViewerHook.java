package xyz.lucasallegri.spiralview;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.threerings.opengl.model.tools.ModelViewer;

public class ModelViewerHook extends ModelViewer {
	
	public ModelViewerHook(String model) {
		super(model);
		JMenuBar mbar = this._frame.getJMenuBar();
		JMenu file = mbar.getMenu(0);
		JMenu environment= new JMenu("Environment");
		file.add(environment, 0);
	}
	
	public static void main(String[] args) {
		new ModelViewerHook(args.length > 0 ? args[0] : null).startup();
	}

}
