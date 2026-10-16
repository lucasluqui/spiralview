package com.lucasluqui.silverweave;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileSystemView;

import com.lucasluqui.silverweave.code.Code;
import com.threerings.config.ConfigReference;
import com.threerings.config.Parameter;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.SphereCoords;
import com.threerings.media.image.ColorPository;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.tools.ModelTool;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureRenderer;
import com.threerings.swing.LogPanel;
import com.threerings.util.MessageBundle;

import com.lucasluqui.silverweave.item.ItemCatalog;
import com.lucasluqui.silverweave.item.KnightItem;
import com.lucasluqui.silverweave.item.Slot;
import com.lucasluqui.silverweave.model.ActivityCycler;
import com.lucasluqui.silverweave.model.KnightAnimator;
import com.lucasluqui.silverweave.model.KnightModel;
import com.lucasluqui.silverweave.ui.AnimationPanel;
import com.lucasluqui.silverweave.ui.EnvironmentPanel;
import com.lucasluqui.silverweave.ui.Glyph;
import com.lucasluqui.silverweave.ui.IconCache;
import com.lucasluqui.silverweave.ui.ItemSearch;
import com.lucasluqui.silverweave.ui.LoadingScreen;
import com.lucasluqui.silverweave.ui.Overlay;
import com.lucasluqui.silverweave.ui.OverlayButton;
import com.lucasluqui.silverweave.ui.SidebarPanel;
import com.lucasluqui.silverweave.ui.Theme;
import com.lucasluqui.silverweave.ui.Toast;
import com.lucasluqui.silverweave.ui.ViewControls;
import com.lucasluqui.spiralview.ViewKL;
import com.lucasluqui.util.ImageUtil;

import org.lwjgl.opengl.GL11;

import static com.lucasluqui.silverweave.Log.log;

public class SilverweaveTool extends ModelTool
  implements SidebarPanel.Actions
{
  static void main ()
  {
    ViewKL.setupLAF();
    LoadingScreen loading = new LoadingScreen(
      ImageUtil.loadImageWithinJar("/img/icon-silverweave.png"), "Silverweave", "Loading...");
    loading.show();
    SilverweaveTool tool = new SilverweaveTool();
    tool._loading = loading;
    // shown from the event thread: showing the window starts the render loop there, which
    // deadlocks with a window being shown from any other thread
    EventQueue.invokeLater(tool::startup);
  }

  public SilverweaveTool ()
  {
    super("silverweave");
    _frame.setTitle(_msgs.get("m.title"));
    _frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon-silverweave.png"));

    // fill the screen, minus the task bar; the bounds also keep the window on screen when it
    // is restored from maximized
    _frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
    _frame.setExtendedState(_frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

    // the log panel is for developers; this is a user-facing tool
    for (Component comp : _frame.getContentPane().getComponents()) {
      if (comp instanceof LogPanel) {
        _frame.getContentPane().remove(comp);
      }
    }

    JMenuBar menubar = new JMenuBar();
    _frame.setJMenuBar(menubar);

    JMenu file = createMenu("file", KeyEvent.VK_F);
    menubar.add(file);
    // first, so that its shortcut gets noticed; CTRL+K works too (see dispatchSearchKey)
    file.add(createMenuItem("search_items", KeyEvent.VK_S, KeyEvent.VK_F));
    file.addSeparator();
    file.add(createMenuItem("import_knight", KeyEvent.VK_I, KeyEvent.VK_I));
    file.add(createMenuItem("export_knight", KeyEvent.VK_E, KeyEvent.VK_E));
    file.add(createMenuItem("test_knight", KeyEvent.VK_T, KeyEvent.VK_F5, 0));
    file.addSeparator();
    file.add(createMenuItem("save_snapshot", KeyEvent.VK_N, KeyEvent.VK_F12, 0));
    file.addSeparator();
    file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

    // the debugging overlays stay off and out of the menus, but the canvas expects them to exist
    _showEnvironment = createCheckBoxMenuItem("environment", KeyEvent.VK_E, KeyEvent.VK_V);
    _showEnvironment.setSelected(true);
    _showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B);
    _showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_C, KeyEvent.VK_M);
    _showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T);

    // the floor grid is never shown
    _showGrid = createCheckBoxMenuItem("grid", KeyEvent.VK_G, KeyEvent.VK_D);
    _showGrid.setSelected(false);

    JMenu edit = createMenu("edit", KeyEvent.VK_E);
    menubar.add(edit);
    edit.add(_undoItem = createMenuItem("undo", KeyEvent.VK_U, KeyEvent.VK_Z));
    edit.add(_redoItem = createMenuItem("redo", KeyEvent.VK_R, KeyEvent.VK_Y));
    updateHistoryItems();

    JMenu view = createMenu("view", KeyEvent.VK_V);
    menubar.add(view);
    view.add(createMenuItem("reset", KeyEvent.VK_R, KeyEvent.VK_R, 0));

    _loadout.addChangeListener(e -> {
      knightChanged();
      updateHistoryItems();
    });
  }

  @Override
  public void actionPerformed (ActionEvent event)
  {
    String action = event.getActionCommand();
    if (action.equals("save_snapshot")) {
      saveSnapshot();
    } else if (action.equals("test_knight")) {
      testKnight();
    } else if (action.equals("import_knight")) {
      importKnight();
    } else if (action.equals("export_knight")) {
      exportKnight();
    } else if (action.equals("undo")) {
      _loadout.undo();
    } else if (action.equals("redo")) {
      _loadout.redo();
    } else if (action.equals("search_items")) {
      // once the menu has let go of the focus, which would otherwise put the search away
      EventQueue.invokeLater(() -> {
        if (_catalog != null) {
          _search.open();
        }
      });
    } else {
      super.actionPerformed(event);
    }
  }

  /**
   * Asks the user for a share code and loads the knight it describes.
   */
  public void importKnight ()
  {
    if (_catalog == null) {
      return;
    }
    JTextField field = codeField("");
    while (true) {
      int choice = JOptionPane.showConfirmDialog(_frame,
        new Object[] { "Paste a Share Code:", field }, "Import Loadout",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (choice != JOptionPane.OK_OPTION || field.getText().trim().isEmpty()) {
        return;
      }
      if (applyCode(field.getText(), "Import Loadout")) {
        return;
      }
    }
  }

  @Override
  public void loadKnight (String code)
  {
    if (_catalog != null) {
      applyCode(code, "Load");
    }
  }

  /**
   * Puts on the knight described by a share code, telling the user about any problems.
   *
   * @return false if the code is invalid.
   */
  protected boolean applyCode (String code, String title)
  {
    try {
      int skipped = _loadout.applyShareCode(code, _catalog);
      if (skipped > 0) {
        JOptionPane.showMessageDialog(_frame, skipped == 1 ?
          "One item in this code isn't available in this version of the game and was skipped." :
          skipped + " items in this code aren't available in this version of the game and were skipped.",
          title, JOptionPane.INFORMATION_MESSAGE);
      }
      return true;
    } catch (ShareCode.InvalidCodeException e) {
      JOptionPane.showMessageDialog(_frame, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  @Override
  public void saveKnight ()
  {
    if (_catalog == null) {
      return;
    }
    KnightItem helmet = _loadout.get(Slot.HELMET);
    Object name = JOptionPane.showInputDialog(_frame, "Name this loadout:", "Save Loadout",
      JOptionPane.PLAIN_MESSAGE, null, null, helmet == null ? "My Loadout" : helmet.displayName);
    if (name != null && !name.toString().trim().isEmpty()) {
      _saved.add(name.toString().trim(), _loadout.toShareCode());
      _sidebar.showSaved();
    }
  }

  /**
   * Dresses the knight in random items, then holds the button back for a moment so that the
   * knight isn't redressed faster than it can be looked at.
   */
  public void randomizeKnight ()
  {
    if (_catalog == null) {
      return;
    }
    _loadout.randomize(_catalog, new Random());
    _randomizeButton.setEnabled(false);
    Timer cooldown = new Timer(RANDOMIZE_COOLDOWN, e -> _randomizeButton.setEnabled(true));
    cooldown.setRepeats(false);
    cooldown.start();
  }

  /**
   * Copies the share code of the current knight to the clipboard so the user can share it.
   */
  public void exportKnight ()
  {
    if (_catalog == null) {
      return;
    }
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
      new StringSelection(_loadout.toShareCode()), null);
    // the button sits at the top of the view, so the message goes below it
    Toast.show(_shareButton, "Share Code copied to clipboard", Glyph.CHECK, true);
  }

  /**
   * Lets the user pick an island to test the knight in.
   */
  public void testKnight ()
  {
    _sidebar.showIslands();
  }

  @Override
  public void testKnight (Island island)
  {
    if (_catalog == null || _testProcess != null) {
      return;
    }
    try {
      // hand the knight over in a file, since its model reference can be large
      File knight = File.createTempFile("silverweave-knight", ".dat");
      knight.deleteOnExit();
      try (BinaryExporter out = new BinaryExporter(new FileOutputStream(knight))) {
        out.writeObject(_loadout.createModelReference(KNIGHT_MODEL));
      }
      // its output is passed along, watching for word that its window is up
      _testProcess = new ProcessBuilder(createTestCommand(knight, island))
        .directory(new File(System.getProperty("user.dir")))
        .redirectInput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start();
      _sidebar.setTestRunning(true);
      watchTestOutput(_testProcess);
      _testProcess.onExit().thenRun(() -> EventQueue.invokeLater(() -> {
        _testProcess = null;
        _sidebar.setTestRunning(false);
        knight.delete();
        // back from testing
        if (!_frame.isVisible()) {
          _frame.setVisible(true);
          _frame.toFront();
        }
      }));
    } catch (IOException e) {
      log.warning("Failed to launch the test session.", e);
      JOptionPane.showMessageDialog(_frame, "Couldn't start the test session.\n" + e.getMessage(),
        "Test Loadout", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Passes the test session's output along, and hides the editor once the session has a window
   * up in its place, so that only one of them sits in the task bar.
   */
  protected void watchTestOutput (Process process)
  {
    Thread thread = new Thread(() -> {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(
          process.getInputStream(), StandardCharsets.UTF_8))) {
        for (String line; (line = in.readLine()) != null; ) {
          if (line.equals(KnightTester.SHOWN_SIGNAL)) {
            EventQueue.invokeLater(() -> {
              if (process.isAlive()) {
                _frame.setVisible(false);
              }
            });
          } else {
            System.out.println(line);
          }
        }
      } catch (IOException e) {
        // the session ended
      }
    }, "Silverweave test output");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Creates the command that runs the tester in a new JVM set up like this one.
   */
  protected List<String> createTestCommand (File knight, Island island)
  {
    List<String> command = new ArrayList<>();
    command.add(ProcessHandle.current().info().command().orElse(
      Paths.get(System.getProperty("java.home"), "bin", "java").toString()));
    long maxMemory = Runtime.getRuntime().maxMemory();
    if (maxMemory != Long.MAX_VALUE) {
      command.add("-Xmx" + (maxMemory / (1024 * 1024)) + "m");
    }
    for (String property : FORWARDED_PROPERTIES) {
      String value = System.getProperty(property);
      if (value != null) {
        command.add("-D" + property + "=" + value);
      }
    }
    // its output comes through a pipe, which is read as UTF-8
    command.add("-Dstdout.encoding=UTF-8");
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(KnightTester.class.getName());
    command.add(knight.getAbsolutePath());
    command.add(island.scene);
    return command;
  }

  /**
   * Creates a text field suited to share codes.
   */
  protected static JTextField codeField (String code)
  {
    // (in the look-and-feel's own font, like the other dialogs' fields)
    JTextField field = new JTextField(code, 36);
    field.selectAll();
    return field;
  }

  @Override
  public void setSkybox (String name)
  {
    // an empty name stands for no skybox, which the user chose over the default
    _prefs.put("skybox", name == null ? "" : name);
    showSkybox(name);
  }

  @Override
  public void setSkyboxOption (String option, String value)
  {
    String skybox = _prefs.get("skybox", "");
    if (!skybox.isEmpty()) {
      skyboxOptions(skybox).put(option, value);
      showSkybox(skybox);
    }
  }

  /**
   * Shows a skybox (a directory under {@link #SKYBOX_DIR}), or none if null, with the options the
   * user picked for it, and offers its options in the sidebar.
   */
  protected void showSkybox (String name)
  {
    if (_skybox == null) {
      return;
    }
    String path = name == null ? null : resolveSkybox(name);
    ModelConfig config = path == null ? null : _cfgmgr.getConfig(ModelConfig.class, path);
    List<EnvironmentPanel.Option> options = new ArrayList<>();
    try {
      if (config == null) {
        _skybox.setConfig((ModelConfig)null);
      } else {
        ConfigReference<ModelConfig> ref = new ConfigReference<>(path);
        Preferences saved = skyboxOptions(name);
        for (Parameter param : config.parameters) {
          if (!(param instanceof Parameter.Choice) ||
              ((Parameter.Choice)param).options.length < 2) {
            continue;
          }
          Parameter.Choice choice = (Parameter.Choice)param;
          List<String> values = new ArrayList<>();
          for (Parameter.Choice.Option option : choice.options) {
            values.add(option.name);
          }
          String value = saved.get(choice.name, choice.choice);
          if (!values.contains(value)) {
            value = choice.choice;
          }
          ref.getArguments().put(choice.name, value);
          options.add(new EnvironmentPanel.Option(choice.name, values, value));
        }
        _skybox.setConfig(ref);
      }
    } catch (RuntimeException e) {
      log.warning("Failed to show skybox.", "skybox", name, e);
    }
    _sidebar.setSkyboxOptions(options);
  }

  /**
   * Returns the preferences holding the options the user picked for a skybox.
   */
  protected Preferences skyboxOptions (String skybox)
  {
    return _prefs.node("skyboxOptions/" + skybox);
  }



  @Override
  protected JComponent createCanvasContainer ()
  {
    // called from the superclass constructor, before our fields are initialized
    _loadout = new Loadout();
    _favorites = new Favorites(_prefs.node("favorites"));
    _saved = new SavedLoadouts(_prefs.node("saved"));
    _icons = new IconCache(path -> {
      try (InputStream in = _rsrcmgr.getResource(path)) {
        return ImageIO.read(in);
      }
    }, () -> _colors);
    _sidebar = new SidebarPanel(_loadout, _icons, _favorites, _saved, this);
    _sidebar.setIslands(Island.loadAll());
    // the sidebar keeps its width; the view takes the rest
    JPanel pane = new JPanel(new BorderLayout());
    _canvas.setMinimumSize(new Dimension(1, 1));
    pane.add(_canvas, BorderLayout.CENTER);
    pane.add(_sidebar, BorderLayout.EAST);
    return pane;
  }

  @Override
  protected CameraHandler createCameraHandler ()
  {
    // the camera only orbits (left drag) and zooms (wheel); panning is disabled
    OrbitCameraHandler camhand = new OrbitCameraHandler(this);
    new MouseOrbiter(camhand) {
      @Override protected void mouseDragged (int x, int y, boolean orbit, boolean zoom, boolean pan) {
        super.mouseDragged(x, y, orbit, false, false);
      }
      @Override protected void mouseWheelMoved (int delta) {
        // glide like the zoom buttons do
        _easeDistance += delta * _unitsPerClick;
      }
    }.addTo(_canvas);
    return camhand;
  }

  @Override
  protected int getAntialiasingLevel ()
  {
    return Antialiasing.getMaxSamples();
  }

  @Override
  protected CanvasToolPrefs createEditablePrefs ()
  {
    return new ModelToolPrefs(_prefs);
  }

  @Override
  protected void didInit ()
  {
    super.didInit();

    // the tool sleeps a frame's worth after each frame, which with vsync halves the frame rate; the
    // frame limiter paces the frames instead
    _sync60 = false;
    _frameLimiter = new FrameLimiter(_frame);

    float[] background = Theme.VIEW_BACKGROUND;
    _compositor.getDefaultBackgroundColor().set(background[0], background[1], background[2], 0f);

    OrbitCameraHandler camhand = (OrbitCameraHandler)_camhand;
    camhand.getCoords().elevation = START_ELEVATION;
    _defaultCoords = new SphereCoords(camhand.getCoords());
    _viewControls = new ViewControls(_frame, _canvas, new ViewControls.Camera() {
      @Override public void rotate (float radians) {
        _easeAzimuth += radians;
      }
      @Override public void zoom (float amount) {
        _easeDistance += amount;
      }
      @Override public void reset () {
        resetCamera();
      }
    });
    _viewControls.show();
    _activities = createActivityIndicator();
    _actionBar = createActionBar();
    _actionBar.show();
    _loadoutName = createLoadoutName();
    _saved.addChangeListener(this::updateLoadoutName);
    _animator = new KnightAnimator();
    _animations = new AnimationPanel(_frame, _canvas, _animator::setUserAnimation);
    _animations.setCollapsed(_prefs.getBoolean("animationsCollapsed", true));
    _animations.setCollapseListener(collapsed -> _prefs.putBoolean("animationsCollapsed", collapsed));
    _animations.show();
    _search = new ItemSearch(_frame, _canvas, _icons, _favorites, _saved,
      _loadout::set, this::loadKnight);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
      this::dispatchSearchKey);

    _colors = ColorPository.loadColorPository(_rsrcmgr);
    registerBundledKnight();
    _knightModel = KnightModel.prepare(_cfgmgr, KNIGHT_MODEL);

    _scene.add(_skybox = new Model(this));
    List<EnvironmentPanel.Choice> skyboxes = findSkyboxes();
    String saved = _prefs.get("skybox", null);
    String skybox = null;
    if (saved == null || (!saved.isEmpty() &&
        skyboxes.stream().noneMatch(choice -> choice.name.equals(saved)))) {
      // nothing chosen yet (or a skybox no longer offered): start with the default, if present
      skybox = skyboxes.stream().filter(choice -> choice.label.equals(DEFAULT_SKYBOX))
        .map(choice -> choice.name).findFirst().orElse(null);
    } else if (!saved.isEmpty()) {
      skybox = saved;
    }
    _sidebar.setSkyboxes(skyboxes, skybox);
    showSkybox(skybox);

    // show the bare knight straight away, then dress it once the items are in
    _scene.add(_model = new Model(this));
    updateKnight();
    loadCatalog();
  }

  /**
   * Reads the items in the background and hands them to the interface.
   */
  protected void loadCatalog ()
  {
    _sidebar.setLoading("Loading items...");
    if (_loading != null) {
      _loading.setStatus("Loading items...");
    }
    Function<String, String> translator = createTranslator();
    new SwingWorker<ItemCatalog, Void>() {
      @Override protected ItemCatalog doInBackground () throws Exception {
        _code = Code.open();
        try (InputStream items = _rsrcmgr.getResource("config/item.dat");
             InputStream accessories = openOptionalResource("config/accessory.dat")) {
          return ItemCatalog.load(_code, items, accessories, translator);
        }
      }
      @Override protected void done () {
        try {
          _catalog = get();
        } catch (InterruptedException | ExecutionException e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          log.warning("Failed to load items.", cause);
          _sidebar.setError("Couldn't read the game's items.<br><br>" + cause.getMessage());
          dismissLoading();
          return;
        }
        _sidebar.setCatalog(_catalog);
        _search.setCatalog(_catalog);
        dismissLoading();
        for (OverlayButton button : _actionButtons) {
          button.setEnabled(true);
        }
        _loadout.restore(_prefs.node("loadout"), _catalog);
      }
    }.execute();
  }

  /**
   * Opens (or closes) the item search on CTRL+K or CTRL+F, wherever the focus is in the editor.
   *
   * @return whether the key was taken.
   */
  protected boolean dispatchSearchKey (KeyEvent event)
  {
    int code = event.getKeyCode();
    if (event.getID() != KeyEvent.KEY_PRESSED || (code != KeyEvent.VK_K && code != KeyEvent.VK_F) ||
        (event.getModifiersEx() & SEARCH_MODIFIERS) != KeyEvent.CTRL_DOWN_MASK) {
      return false;
    }
    if (_search.isOpen()) {
      _search.close();
      return true;
    }
    // not from dialogs the editor has up
    if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != _frame ||
        _catalog == null) {
      return false;
    }
    _search.open();
    return true;
  }

  /**
   * Creates the cycler that shows off the equipped items' activity animations, along with the
   * note over the view controls telling which one is playing (shown only while there are any).
   */
  protected ActivityCycler createActivityIndicator ()
  {
    JLabel label = new JLabel("", JLabel.CENTER);
    label.setIconTextGap(8);
    label.setFont(Theme.font(12f, Font.PLAIN));
    label.setForeground(Theme.TEXT);
    label.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
    Overlay indicator = new Overlay(_frame, _canvas, Overlay.Anchor.BOTTOM_CENTER, label, 0);
    indicator.stackOn(_viewControls.getOverlay());
    return new ActivityCycler(activity -> {
      if (activity == null) {
        indicator.hide();
      } else {
        label.setText("Animation: " + activity);
        indicator.pack();
        indicator.show();
      }
    });
  }

  /**
   * Creates the note under the action bar naming the saved loadout the knight is wearing.
   */
  protected Overlay createLoadoutName ()
  {
    _loadoutNameLabel = new JLabel("", JLabel.CENTER);
    // (names are the user's own text, not markup)
    _loadoutNameLabel.putClientProperty("html.disable", Boolean.TRUE);
    _loadoutNameLabel.setFont(Theme.font(14f, Font.BOLD));
    _loadoutNameLabel.setForeground(Theme.TEXT);
    _loadoutNameLabel.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
    Overlay overlay = new Overlay(_frame, _canvas, Overlay.Anchor.TOP_CENTER, _loadoutNameLabel, 0);
    overlay.stackOn(_actionBar);
    return overlay;
  }

  /**
   * Shows the name of the saved loadout the knight is wearing, or hides it if the knight doesn't
   * match any (as after a change to a loaded one).
   */
  protected void updateLoadoutName ()
  {
    String name = _catalog == null ? null : savedName(_loadout.toShareCode());
    if (name == null) {
      _loadoutName.hide();
      return;
    }
    _loadoutNameLabel.setText("\"" + name + "\"");
    _loadoutName.pack();
    _loadoutName.show();
  }

  /**
   * Returns the name of the saved loadout with the given share code, or null if there's none.
   */
  protected String savedName (String code)
  {
    for (SavedLoadouts.Entry entry : _saved.getEntries()) {
      if (code.equals(_savedCodes.computeIfAbsent(entry.code, this::normalizeCode))) {
        return entry.getName();
      }
    }
    return null;
  }

  /**
   * Returns a saved share code as the loadout would write it now, so that codes from older
   * versions still match; an empty string for invalid ones.
   */
  protected String normalizeCode (String code)
  {
    try {
      ShareCode.Knight knight = ShareCode.decode(code, _catalog);
      return ShareCode.encode(knight.items, knight.shieldAway);
    } catch (ShareCode.InvalidCodeException e) {
      return "";
    }
  }

  /**
   * Creates the bar floating over the top of the view with the actions on the whole knight.
   */
  protected Overlay createActionBar ()
  {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
    bar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    _actionButtons.add(new OverlayButton(Glyph.IMPORT::paint, "Import a loadout from a Share Code",
      false, this::importKnight));
    _shareButton = new OverlayButton(Glyph.SHARE::paint, "Copy this loadout's Share Code",
      false, this::exportKnight);
    _actionButtons.add(_shareButton);
    _actionButtons.add(new OverlayButton(Glyph.SAVE::paint, "Save this loadout", false,
      this::saveKnight));
    _randomizeButton = new OverlayButton(Glyph.RANDOMIZE::paint, "Randomize",
      false, this::randomizeKnight);
    _actionButtons.add(_randomizeButton);
    OverlayButton clear = new OverlayButton(Glyph.CLEAR::paint, "Clear", false, _loadout::clear);
    clear.setHoverColor(Theme.DANGER);
    _actionButtons.add(clear);
    for (OverlayButton button : _actionButtons) {
      // nothing to act on until the items are in
      button.setEnabled(false);
      bar.add(button);
    }
    return new Overlay(_frame, _canvas, Overlay.Anchor.TOP_CENTER, bar, 0);
  }

  /**
   * Takes down the loading screen, once the knight has had a moment to dress.
   */
  protected void dismissLoading ()
  {
    if (_loading != null) {
      _loading.dismiss(LOADING_GRACE, _frame);
      _loading = null;
    }
  }

  /**
   * Creates the translator for the item name keys.
   * Returns null for anything the message bundle doesn't define.
   */
  protected Function<String, String> createTranslator ()
  {
    MessageBundle bundle = _msgmgr.getBundle("item");
    return key -> {
      if (bundle == null || key == null || key.isEmpty()) {
        return null;
      }
      // compound keys ("a.fiery|m.trojan_helm") are translated as a whole if their head exists
      int bar = key.indexOf('|');
      String head = bar < 0 ? key : key.substring(0, bar);
      if (head.startsWith("~") || !bundle.exists(head)) {
        return null;
      }
      return bundle.xlate(key);
    };
  }

  /**
   * Finds the skyboxes offered to the user: the directories under {@link #SKYBOX_DIR} holding a
   * skybox (or plain) model whose names are among {@link #SKYBOXES}, in that order.
   */
  protected List<EnvironmentPanel.Choice> findSkyboxes ()
  {
    Map<String, String> found = new HashMap<>();
    File root = _rsrcmgr.getResourceFile(SKYBOX_DIR);
    if (root == null || !root.isDirectory()) {
      log.info("No skyboxes found.", "dir", root);
      return new ArrayList<>();
    }
    try (Stream<Path> dirs = Files.walk(root.toPath())) {
      dirs.filter(Files::isDirectory).forEach(dir -> {
        String key = skyboxKey(dir.getFileName().toString());
        if (!SKYBOXES.containsKey(key) || found.containsKey(key)) {
          return;
        }
        for (String file : SKYBOX_FILES) {
          if (Files.isRegularFile(dir.resolve(file))) {
            found.put(key, SKYBOX_DIR + "/" +
              root.toPath().relativize(dir).toString().replace(File.separatorChar, '/'));
            break;
          }
        }
      });
    } catch (IOException | UncheckedIOException e) {
      log.warning("Failed to list skyboxes.", "dir", root, e);
    }
    List<EnvironmentPanel.Choice> choices = new ArrayList<>();
    for (Map.Entry<String, String> skybox : SKYBOXES.entrySet()) {
      String path = found.get(skybox.getKey());
      if (path == null) {
        log.info("Skybox not found.", "skybox", skybox.getValue());
      } else {
        choices.add(new EnvironmentPanel.Choice(path, skybox.getValue()));
      }
    }
    return choices;
  }


  /**
   * Resolves a skybox directory to the path of the model it holds, or null.
   */
  protected String resolveSkybox (String dir)
  {
    for (String file : SKYBOX_FILES) {
      String path = dir + "/" + file;
      try {
        if (_cfgmgr.getResourceConfig(path) instanceof ModelConfig) {
          return path;
        }
      } catch (RuntimeException e) {
        log.info("Unreadable skybox model.", "path", path, "error", e);
      }
    }
    return null;
  }


  /**
   * Returns the key a skybox directory is known by in {@link #SKYBOXES}: its name in lower
   * case, without the separators ("grassislandnight" for "Grass_Island_Night").
   */
  protected static String skyboxKey (String dir)
  {
    return dir.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }



  /**
   * Opens a resource that may be missing.
   */
  protected InputStream openOptionalResource (String path)
  {
    try {
      return _rsrcmgr.getResource(path);
    } catch (FileNotFoundException e) {
      log.info("Optional resource not found.", "path", path);
    } catch (IOException e) {
      log.warning("Failed to open resource.", "path", path, e);
    }
    return null;
  }

  /**
   * Falls back to the knight model bundled with spiralview if the installed copy can't be found.
   */
  protected void registerBundledKnight ()
  {
    File file = _rsrcmgr.getResourceFile(KNIGHT_MODEL);
    if (file != null && file.exists()) {
      return;
    }
    try (InputStream in = getClass().getResourceAsStream("/rsrc/" + KNIGHT_MODEL)) {
      if (in == null) {
        return;
      }
      try (BinaryImporter importer = new BinaryImporter(in)) {
        ModelConfig config = (ModelConfig)importer.readObject();
        config.setName(KNIGHT_MODEL);
        config.init(_cfgmgr);
        _cfgmgr.updateResourceConfig(KNIGHT_MODEL, config);
        log.info("Using the bundled knight model.");
      }
    } catch (IOException | ClassCastException e) {
      log.warning("Failed to load the bundled knight model.", e);
    }
  }

  protected void updateHistoryItems ()
  {
    _undoItem.setEnabled(_loadout.canUndo());
    _redoItem.setEnabled(_loadout.canRedo());
  }

  /**
   * Schedules the knight model to be rebuilt; bursts of changes result in a single rebuild.
   */
  protected void knightChanged ()
  {
    if (!_updatePending) {
      _updatePending = true;
      EventQueue.invokeLater(() -> {
        _updatePending = false;
        updateKnight();
        updateLoadoutName();
      });
    }
  }

  protected void updateKnight ()
  {
    if (_model == null) {
      return;
    }
    try {
      ConfigReference<ModelConfig> ref = _loadout.createModelReference(KNIGHT_MODEL);
      Map<Model, ModelConfig> attached = KnightModel.attachmentConfigs(_model);
      _model.setConfig(ref);
      // what took the place of something else would otherwise carry on with its animations
      KnightModel.restartChanged(_model, attached);
      _animations.setAnimations(_model.getAnimations());
      _animator.setModel(_model, _loadout.get(Slot.WEAPON));
      // a slung shield sits out the activities, so the cycler needs to know its model
      Object shield = _loadout.isShieldAway() ? ref.getArguments().get("Shield") : null;
      _activities.setModel(_model, _loadout.isShieldAway(),
        shield instanceof ConfigReference ? ((ConfigReference<?>)shield).getName() : null);
    } catch (RuntimeException e) {
      // a broken or missing asset must not take the whole tool down with it
      log.warning("Failed to build the knight model.", e);
    }
    frameKnight();
    if (_catalog != null) {
      _loadout.save(_prefs.node("loadout"));
    }
  }

  /**
   * Centers the camera on the knight and limits zooming to a close range.
   *
   * <p>This is done again only when the knight's height changes. Its bounds also shift with
   * everything it wears (and with options like slinging the shield), and following them would
   * make the knight jump around the view.
   */
  protected void frameKnight ()
  {
    OrbitCameraHandler camhand = (OrbitCameraHandler)_camhand;
    KnightItem knightHeight = _loadout.get(Slot.HEIGHT);
    Box bounds = _model.getBounds();
    boolean measured = bounds != null && !bounds.isEmpty();
    if (_framedOnBounds && knightHeight == _framedHeight) {
      return;
    }
    _framedOnBounds = measured;
    _framedHeight = knightHeight;
    float height = DEFAULT_KNIGHT_SIZE / 2f, size = DEFAULT_KNIGHT_SIZE;
    if (measured) {
      height = bounds.getCenter().z;
      size = bounds.getDiagonalLength();
    }
    // the knight stands on the origin; only follow its height so outfits don't shift it around
    camhand.getTarget().set(0f, 0f, height);
    camhand.setCoordLimits(-FloatMath.HALF_PI, FloatMath.HALF_PI,
      size * MIN_ZOOM_FACTOR, size * MAX_ZOOM_FACTOR);
    _defaultCoords.distance = size * START_ZOOM_FACTOR;
    if (!_framed) {
      // start out close enough for the outfit to be seen well
      _framed = true;
      camhand.getCoords().distance = _defaultCoords.distance;
    }
    camhand.zoom(0f);
  }

  /**
   * Eases the camera back to its starting position.
   */
  protected void resetCamera ()
  {
    SphereCoords coords = ((OrbitCameraHandler)_camhand).getCoords();
    // the camera may have gone around several times; take the short way back
    _easeAzimuth = FloatMath.normalizeAngle(_defaultCoords.azimuth - coords.azimuth);
    _easeElevation = _defaultCoords.elevation - coords.elevation;
    _easeDistance = _defaultCoords.distance - coords.distance;
  }

  @Override
  protected void updateView (float elapsed)
  {
    _frameLimiter.frameStarted();
    easeCamera(elapsed);
    super.updateView(elapsed);
  }

  @Override
  protected void renderView ()
  {
    super.renderView();
    // the view's frames are swapped right after this; snapshots render outside of them (and of
    // the canvas lock)
    if (!_snapshotSceneOnly) {
      // not sleeping while holding the canvas lock: on X11 it's the AWT toolkit lock, and input
      // can't be read while it's held
      _canvas.pauseContextLock();
      try {
        _frameLimiter.beforeSwap();
      } finally {
        _canvas.resumeContextLock();
      }
    }
  }

  /**
   * Moves the camera part of the way through the motion requested by the view controls, so that
   * it glides rather than jumps.
   */
  protected void easeCamera (float elapsed)
  {
    if (_easeAzimuth == 0f && _easeElevation == 0f && _easeDistance == 0f) {
      return;
    }
    OrbitCameraHandler camhand = (OrbitCameraHandler)_camhand;
    float share = 1f - FloatMath.exp(-CAMERA_EASING * elapsed);
    float azimuth = settle(_easeAzimuth, share, 0.0005f);
    float elevation = settle(_easeElevation, share, 0.0005f);
    float distance = settle(_easeDistance, share, 0.001f);
    float before = camhand.getCoords().distance;
    camhand.orbit(azimuth, elevation);
    camhand.zoom(distance);
    _easeAzimuth -= azimuth;
    _easeElevation -= elevation;
    // stop pushing against the zoom limits
    float zoomed = camhand.getCoords().distance - before;
    _easeDistance = Math.abs(zoomed - distance) > 0.0001f ? 0f : _easeDistance - distance;
  }

  /**
   * Returns the share of the remaining motion to make this frame: all of it, once it's small.
   */
  protected static float settle (float remaining, float share, float epsilon)
  {
    return Math.abs(remaining) <= epsilon ? remaining : remaining * share;
  }

  /**
   * Saves a transparent snapshot of the knight to the user's documents directory.
   */
  protected void saveSnapshot ()
  {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    File file = new File(FileSystemView.getFileSystemView().getDefaultDirectory(),
      "silverweave_" + fmt.format(new Date()) + ".png");
    try {
      ImageIO.write(createSceneSnapshot(true), "png", file);
      log.info("Saved snapshot.", "file", file);
    } catch (IOException e) {
      log.warning("Failed to write snapshot.", "file", file, e);
    }
  }

  /**
   * Renders the scene alone (no grid or other helpers) into an offscreen buffer and returns it.
   */
  protected BufferedImage createSceneSnapshot (boolean alpha)
  {
    int width = _renderer.getWidth(), height = _renderer.getHeight();

    Texture2D color = new Texture2D(_renderer, true);
    color.setImage(alpha ? GL11.GL_RGBA8 : GL11.GL_RGB8, width, height, false, false);

    TextureRenderer trenderer = new TextureRenderer(this, color, null, width, height);
    Color4f obackground = _compositor.getBackgroundColor();
    _compositor.setBackgroundColor(alpha ? Color4f.TRANSPARENT_BLACK : Color4f.BLACK);

    trenderer.startRender();
    _snapshotSceneOnly = true;
    try {
      renderView();
      return createSnapshot(alpha);
    } finally {
      _snapshotSceneOnly = false;
      trenderer.commitRender();
      trenderer.dispose();
      color.delete();
      _compositor.setBackgroundColor(obackground);
    }
  }

  /** Shown while the editor starts up, or null once it's gone. */
  protected LoadingScreen _loading;

  /** How long the loading screen stays up after everything is in, in milliseconds. */
  protected static final int LOADING_GRACE = 500;

  /** How long the randomize button stays disabled after each use, in milliseconds. */
  protected static final int RANDOMIZE_COOLDOWN = 2000;

  /** Knight's current wear. */
  protected Loadout _loadout;

  /** The outfitting controls. */
  protected SidebarPanel _sidebar;

  /** Item icons, shared by the interface. */
  protected IconCache _icons;

  /** The search over every item, brought up with CTRL+K. */
  protected ItemSearch _search;

  /** The modifiers looked at for the search shortcut, which must be CTRL alone. */
  protected static final int SEARCH_MODIFIERS = KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK |
    KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_GRAPH_DOWN_MASK;

  protected ItemCatalog _catalog;

  /** The pcode view, kept open for as long as the catalog is in use. */
  protected Code _code;

  /** Color definitions, for icon colorization. */
  protected volatile ColorPository _colors;

  protected Favorites _favorites;

  protected SavedLoadouts _saved;

  /** Knight model config, held so that the config manager keeps our version of it. */
  protected ModelConfig _knightModel;

  /** Camera's starting position, for resetting the view. */
  protected SphereCoords _defaultCoords;

  /** The camera motion requested by the view controls that's yet to be made. */
  protected float _easeAzimuth, _easeElevation, _easeDistance;

  /** How quickly the camera catches up with the view controls: the higher, the snappier. */
  protected static final float CAMERA_EASING = 14f;

  /** Rotate and zoom buttons over the view. */
  protected ViewControls _viewControls;

  /** Import, share, save, randomize and reset buttons over the view. */
  protected Overlay _actionBar;

  /** The note under the action bar naming the saved loadout the knight is wearing. */
  protected Overlay _loadoutName;
  protected JLabel _loadoutNameLabel;

  /** The saved loadouts' share codes as they'd be written now, by the codes saved. */
  protected final Map<String, String> _savedCodes = new HashMap<>();

  protected JMenuItem _undoItem, _redoItem;

  /** The action bar's share button, which the copied message points at. */
  protected OverlayButton _shareButton;

  /** The action bar's randomize button, held back for a moment after each use. */
  protected OverlayButton _randomizeButton;

  protected final List<OverlayButton> _actionButtons = new ArrayList<>();

  /** Animation list over the view. */
  protected AnimationPanel _animations;

  /** Plays the knight's idle fidgets and weapon poses. */
  protected KnightAnimator _animator;

  /** Cycles through the equipped items' activity animations. */
  protected ActivityCycler _activities;

  /** Whether the camera has been placed for the knight yet. */
  protected boolean _framed;

  /** Whether the camera was last placed from the knight's measured bounds (rather than a
   * guess), and the height item it was wearing then. */
  protected boolean _framedOnBounds;
  protected KnightItem _framedHeight;

  protected Model _skybox;

  /** The directory holding the skyboxes, and the models a skybox directory may hold. */
  protected static final String SKYBOX_DIR = "world/skybox";
  protected static final String[] SKYBOX_FILES = { "skybox.dat", "model.dat" };

  /** The skyboxes offered, by directory key (see {@link #skyboxKey}), with the names shown. */
  protected static final Map<String, String> SKYBOXES = new LinkedHashMap<>();
  static {
    SKYBOXES.put("modelviewer", "Viewer");
    SKYBOXES.put("designmode", "Design Mode");
    SKYBOXES.put("grassislandnight", "Grass Island Night");
    SKYBOXES.put("clockworkskydome", "Clockwork Skydome");
    SKYBOXES.put("clockworktunnels", "Clockwork Tunnels");
    SKYBOXES.put("castlefirestorm", "Castle Firestorm");
    SKYBOXES.put("whitespace", "Whitespace");
  }

  /** The skybox shown until the user picks another. */
  protected static final String DEFAULT_SKYBOX = "Viewer";

  /** The camera's starting angle above the knight, in radians: barely above, facing it. */
  protected static final float START_ELEVATION = 0.1f;

  /** Size assumed for the knight when its model has no bounds yet. */
  protected static final float DEFAULT_KNIGHT_SIZE = 2.2f;

  /** Closest, starting and farthest distances of the camera, relative to the knight's size. */
  protected static final float MIN_ZOOM_FACTOR = 0.35f, START_ZOOM_FACTOR = 0.45f,
    MAX_ZOOM_FACTOR = 1.1f;

  /** System properties forwarded to the knight tester. */
  protected static final String[] FORWARDED_PROPERTIES = {
    "appdir", "resource_dir", "java.library.path", "file.encoding", "sun.java2d.uiScale",
    "org.lwjgl.opengl.contextAPI" };

  /** Running test session, if any. */
  protected Process _testProcess;

  /** Whether a knight rebuild is scheduled. */
  protected boolean _updatePending;

  /** Paces the view's frames, in place of the tool's own limiter. */
  protected FrameLimiter _frameLimiter;
  protected static Preferences _prefs = Preferences.userNodeForPackage(SilverweaveTool.class);

  /** Knight model where everything is put on. */
  public static final String KNIGHT_MODEL = "character/pc/model.dat";
}
