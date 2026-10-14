package com.lucasluqui.silverweave;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.threerings.config.ConfigGroup;
import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.config.ConfigReference;
import com.threerings.export.BinaryImporter;
import com.threerings.math.FloatMath;
import com.threerings.math.SphereCoords;
import com.threerings.opengl.GlCanvas;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.gui.CanvasRoot;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.swing.LogPanel;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ActorSpriteConfig;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.tools.TudeyTool;
import com.threerings.util.ToolUtil;

import com.lucasluqui.silverweave.model.KnightModel;
import com.lucasluqui.silverweave.ui.Glyph;
import com.lucasluqui.silverweave.ui.LoadingScreen;
import com.lucasluqui.silverweave.ui.Overlay;
import com.lucasluqui.silverweave.ui.OverlayButton;
import com.lucasluqui.silverweave.ui.Theme;
import com.lucasluqui.spiralview.ViewKL;
import com.lucasluqui.util.ImageUtil;

import static com.lucasluqui.silverweave.Log.log;

/**
 * A test session in which the user walks around a level as the knight they put together.
 *
 * <p>Runs in its own process (launched by {@link SilverweaveTool}), with the knight model reference
 * handed over in an exported file named on the command line.
 */
public class KnightTester extends TudeyTool
{
  /** The actor the tool scene manager spawns for the player. */
  public static final String PAWN_CONFIG = "Character/PC/Editor";

  /** The knight actor whose movement the test pawn copies. */
  public static final String GAME_KNIGHT = "Character/PC/Default";

  /** The movement used if the knight actor can't be read. */
  protected static final float GAME_SPEED = 5f;
  protected static final int GAME_COLLISION_FLAGS = 4, GAME_COLLISION_MASK = 43;

  /** The level the knight is tested in if none is given. */
  public static final String DEFAULT_SCENE = "/silverweave/islands/torpor-titan/scene.dat";

  /** The line the tester prints once its window is up, so the editor can step aside. */
  public static final String SHOWN_SIGNAL = "silverweave: tester shown";

  protected static final String LOG_FILE = "silverweave-test.log";

  /**
   * The program entry point.
   *
   * @param args the path of the file holding the knight's model reference, then the classpath
   * resource path of the island's level.
   */
  static void main (String[] args)
  {
    ViewKL.setupLAF();
    LoadingScreen loading = new LoadingScreen(
      ImageUtil.loadImageWithinJar("/img/icon-silverweave-test.png"), "Test Loadout",
      "Loading...");
    loading.show();
    // tell the editor, which hides itself now that this has a window up in its place
    System.out.println(SHOWN_SIGNAL);
    System.out.flush();
    // a log of its own, so the editor's isn't overwritten (after the signal, as this takes over
    // the output)
    ToolUtil.configureLog(LOG_FILE);
    KnightTester tester = new KnightTester(args.length > 0 ? new File(args[0]) : null,
      args.length > 1 ? args[1] : DEFAULT_SCENE);
    tester._loading = loading;
    // shown from the event thread: showing the window starts the render loop there, which
    // deadlocks with a window being shown from any other thread
    EventQueue.invokeLater(tester::startup);
  }

  public KnightTester (File knight, String scene)
  {
    super("silverweave");
    _knight = knight;
    _scene = scene;
    _frame.setTitle(_msgs.get("m.test_title"));
    _frame.setIconImage(ImageUtil.loadImageWithinJar("/img/icon-silverweave-test.png"));
    for (Component comp : _frame.getContentPane().getComponents()) {
      if (comp instanceof LogPanel) {
        _frame.getContentPane().remove(comp);
      }
    }
    _frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
    _frame.setExtendedState(_frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

    // escape ends the test (the scene controller would otherwise leave the level, showing nothing)
    Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
      KeyEvent key = (KeyEvent)event;
      if (key.getID() == KeyEvent.KEY_PRESSED && key.getKeyCode() == KeyEvent.VK_ESCAPE) {
        key.consume();
        EventQueue.invokeLater(this::shutdown);
      }
    }, AWTEvent.KEY_EVENT_MASK);

    // the canvas consults these debugging overlays, which stay off
    _showGrid = new JCheckBoxMenuItem();
    _showBounds = new JCheckBoxMenuItem();
    _showCompass = new JCheckBoxMenuItem();
    _showStats = new JCheckBoxMenuItem();
  }

  @Override
  public Root createRoot ()
  {
    // called from the superclass constructor; the superclass disposes of it
    if (_canvasRoot == null) {
      _canvasRoot = new CameraRoot(this, _canvas);
    }
    return _canvasRoot;
  }

  @Override
  protected CanvasToolPrefs createEditablePrefs ()
  {
    return new CanvasToolPrefs(Preferences.userNodeForPackage(KnightTester.class));
  }

  @Override
  protected int getAntialiasingLevel ()
  {
    return Antialiasing.getMaxSamples();
  }

  @Override
  protected void didInit ()
  {
    super.didInit();

    // the tool sleeps a frame's worth after each frame, which with vsync halves the frame rate; the
    // frame limiter paces the frames instead
    _sync60 = false;
    _frameLimiter = new FrameLimiter(_frame);

    // render effects draw the scene through offscreen buffers, which lose the antialiasing
    setRenderEffects(false);

    JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
    bar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    JLabel hint = new JLabel("Press ESC to exit. Hold CTRL and drag or scroll to move the camera.");
    hint.setFont(Theme.font(12.5f, Font.PLAIN));
    hint.setForeground(Theme.TEXT);
    hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
    bar.add(hint);
    bar.add(new OverlayButton(Glyph.RESET::paint, "Reset camera",
      "Reset camera state to default", false, this::resetCamera));
    // as wide as its longer label, since the overlay is sized to its contents only once; a wider
    // button would wrap to a second row, out of sight
    _musicButton = new OverlayButton(Glyph.MUTED::paint, "Unmute music", null, false,
      this::toggleMusic);
    _musicButton.setPreferredSize(_musicButton.getPreferredSize());
    bar.add(_musicButton);
    setMusicMuted(_prefs.getBoolean("musicMuted", false));
    new Overlay(_frame, _canvas, Overlay.Anchor.TOP_CENTER, bar, 0).show();
    addMouseCamera();

    // the loading screen comes down once the knight is in the level
    _locdir.addLocationObserver(new LocationAdapter() {
      @Override public void locationDidChange (PlaceObject place) {
        dismissLoading();
      }
      @Override public void locationChangeFailed (int placeId, String reason) {
        dismissLoading();
      }
    });

    // the local client logs on asynchronously; enter the level once it has
    _client.addClientObserver(new ClientAdapter() {
      @Override public void clientDidLogon (Client client) {
        enterScene();
      }
    });
    if (_client.isLoggedOn()) {
      enterScene();
    }
  }

  @Override
  protected void updateView (float elapsed)
  {
    _frameLimiter.frameStarted();
    super.updateView(elapsed);
  }

  @Override
  protected void renderView ()
  {
    super.renderView();
    // not sleeping while holding the canvas lock: on X11 it's the AWT toolkit lock, and input
    // can't be read while it's held
    _canvas.pauseContextLock();
    try {
      _frameLimiter.beforeSwap();
    } finally {
      _canvas.resumeContextLock();
    }
  }

  /**
   * Puts the camera back where the level placed it, before the user moved it.
   */
  protected void resetCamera ()
  {
    OrbitCameraHandler camhand = levelCamera();
    if (camhand != null && camhand == _homeCamera) {
      camhand.getCoords().set(_home);
      camhand.updatePosition();
    }
  }

  /**
   * Mutes the level's music if it's playing, or brings it back if it's muted, remembering the
   * choice for later tests.
   */
  protected void toggleMusic ()
  {
    boolean muted = !_musicMuted;
    _prefs.putBoolean("musicMuted", muted);
    setMusicMuted(muted);
  }

  /**
   * Mutes or unmutes the level's music, and shows which on the music button.
   */
  protected void setMusicMuted (boolean muted)
  {
    _musicMuted = muted;
    // the music plays as streams, whose volume follows this as they play; sound effects don't
    getStreamGain().value = muted ? 0f : 1f;
    _musicButton.setText(muted ? "Unmute music" : "Mute music");
    _musicButton.setGlyph(muted ? Glyph.MUTED::paint : Glyph.SOUND::paint);
    _musicButton.setToolTipText(muted ? "Play the level's music" : "Silence the level's music");
  }

  /**
   * Returns the camera of the level view, or null if the level isn't up.
   */
  protected OrbitCameraHandler levelCamera ()
  {
    // the level view installs its own camera handler while it's showing
    return _entered && _camhand instanceof OrbitCameraHandler ?
      (OrbitCameraHandler)_camhand : null;
  }

  /**
   * Lets the user move the camera. Holding CTRL while dragging
   * (left button to orbit, right to zoom) or turning the mouse wheel.
   */
  protected void addMouseCamera ()
  {
    MouseAdapter mouse = new MouseAdapter() {
      @Override public void mousePressed (MouseEvent e) {
        _lastX = e.getX();
        _lastY = e.getY();
      }
      @Override public void mouseMoved (MouseEvent e) {
        _lastX = e.getX();
        _lastY = e.getY();
      }
      @Override public void mouseDragged (MouseEvent e) {
        int dx = e.getX() - _lastX, dy = e.getY() - _lastY;
        _lastX = e.getX();
        _lastY = e.getY();
        OrbitCameraHandler camhand = movableCamera(e);
        if (camhand == null) {
          return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
          camhand.orbit(-dx * RADIANS_PER_PIXEL, dy * RADIANS_PER_PIXEL);
        } else if (SwingUtilities.isRightMouseButton(e)) {
          camhand.zoom(dy * UNITS_PER_PIXEL);
        }
        camhand.updatePosition();
      }
      @Override public void mouseWheelMoved (MouseWheelEvent e) {
        OrbitCameraHandler camhand = movableCamera(e);
        if (camhand != null) {
          camhand.zoom((float)e.getPreciseWheelRotation() * UNITS_PER_CLICK);
          camhand.updatePosition();
        }
      }
      protected int _lastX, _lastY;
    };
    _canvas.addMouseListener(mouse);
    _canvas.addMouseMotionListener(mouse);
    _canvas.addMouseWheelListener(mouse);
  }

  /**
   * Returns the level camera if the user is moving it with the given event (null if not),
   * noting where it was first, so it can be put back.
   */
  protected OrbitCameraHandler movableCamera (MouseEvent event)
  {
    OrbitCameraHandler camhand = event.isControlDown() ? levelCamera() : null;
    if (camhand != null && camhand != _homeCamera) {
      _homeCamera = camhand;
      _home = new SphereCoords(camhand.getCoords());
    }
    return camhand;
  }

  /**
   * Dresses the pawn as the knight and moves into the test level.
   */
  protected void enterScene ()
  {
    if (_entered) {
      return;
    }
    _entered = true;
    try {
      _knightModel = KnightModel.prepare(_cfgmgr, SilverweaveTool.KNIGHT_MODEL);
      ConfigReference<ModelConfig> knight = readKnight();
      if (knight != null) {
        applyKnight(knight);
      }
      TudeySceneModel scene = readScene();
      scene.init(_cfgmgr);
      scene.setPlaceConfig(new TudeySceneConfig());
      scene.sceneId = 1;
      _server.getSceneRepository().setSceneModel(scene);
      _scenedir.moveTo(scene.sceneId);
    } catch (IOException | RuntimeException e) {
      log.warning("Failed to start the test session.", e);
      dismissLoading();
    }
  }

  /**
   * Makes the actor the player controls look and move like the knight.
   */
  protected void applyKnight (ConfigReference<ModelConfig> knight)
  {
    ActorConfig actor = _cfgmgr.getConfig(ActorConfig.class, PAWN_CONFIG);
    if (actor != null && actor.implementation instanceof ActorConfig.Original &&
        ((ActorConfig.Original)actor.implementation).sprite != null) {
      ActorConfig.Original pawn = (ActorConfig.Original)actor.implementation;
      pawn.sprite.model = knight;
      applyGameMovement(pawn);
      actor.wasUpdated();
      return;
    }
    // no usable pawn config in this install: provide a basic one
    ConfigGroup<ActorConfig> group = _cfgmgr.getGroup(ActorConfig.class);
    if (group == null) {
      log.warning("No actor configs; the test pawn keeps its default look.");
      return;
    }
    ActorConfig config = new ActorConfig();
    config.setName(PAWN_CONFIG);
    ActorConfig.Pawn pawn = new ActorConfig.Pawn();
    ActorSpriteConfig.Moving sprite = new ActorSpriteConfig.Moving();
    sprite.model = knight;
    pawn.sprite = sprite;
    applyGameMovement(pawn);
    config.implementation = pawn;
    group.addConfig(config);
  }

  /**
   * Gives the pawn the knight actor's speed and collisions, or defaults if it can't be read.
   */
  protected void applyGameMovement (ActorConfig.Original pawn)
  {
    float speed = GAME_SPEED;
    int flags = GAME_COLLISION_FLAGS, mask = GAME_COLLISION_MASK;
    try {
      ActorConfig game = _cfgmgr.getConfig(ActorConfig.class, GAME_KNIGHT);
      if (game != null && game.implementation instanceof ActorConfig.Mobile) {
        ActorConfig.Mobile knight = (ActorConfig.Mobile)game.implementation;
        speed = knight.speed;
        flags = knight.collisionFlags;
        mask = knight.collisionMask;
      }
    } catch (RuntimeException | LinkageError e) {
      log.info("Using default knight movement.", "error", e);
    }
    pawn.collisionFlags = flags;
    pawn.collisionMask = mask;
    if (pawn instanceof ActorConfig.Mobile) {
      ((ActorConfig.Mobile)pawn).speed = speed;
    }
  }

  /**
   * Takes down the loading screen, leaving the level a moment to load its models.
   */
  protected void dismissLoading ()
  {
    if (_loading != null) {
      _loading.dismiss(LOADING_GRACE, _frame);
      _loading = null;
    }
  }

  @SuppressWarnings("unchecked")
  protected ConfigReference<ModelConfig> readKnight ()
    throws IOException
  {
    if (_knight == null) {
      return null;
    }
    try (BinaryImporter in = new BinaryImporter(new FileInputStream(_knight))) {
      return (ConfigReference<ModelConfig>)in.readObject();
    }
  }

  protected TudeySceneModel readScene ()
    throws IOException
  {
    try (InputStream stream = KnightTester.class.getResourceAsStream(_scene)) {
      if (stream == null) {
        throw new IOException("Missing test scene " + _scene);
      }
      try (BinaryImporter in = new BinaryImporter(stream)) {
        return (TudeySceneModel)in.readObject();
      }
    }
  }

  /**
   * Keeps the mouse from steering the knight while CTRL is held to move the camera: presses made
   * with CTRL down never reach the level, held buttons are let go when CTRL goes down, and the
   * pointer is taken off the level so the knight keeps facing the same way.
   *
   * <p>The tool's controller is meant to do this itself, but asks for its camera modifiers with
   * the wrong masks.
   */
  protected static class CameraRoot extends CanvasRoot
  {
    public CameraRoot (GlContext ctx, GlCanvas canvas)
    {
      super(ctx, canvas);
      _glCanvas = canvas;
    }

    @Override
    public void mousePressed (MouseEvent e)
    {
      if (e.isControlDown()) {
        _ignored.add(e.getButton());
        return;
      }
      _held.add(e.getButton());
      super.mousePressed(e);
    }

    @Override
    public void mouseReleased (MouseEvent e)
    {
      // the level only hears about the release of a press it heard about
      if (_ignored.remove(e.getButton()) || !_held.remove(e.getButton())) {
        return;
      }
      super.mouseReleased(e);
    }

    @Override
    public void mouseDragged (MouseEvent e)
    {
      if (!steering(e)) {
        super.mouseDragged(e);
      }
    }

    @Override
    public void mouseMoved (MouseEvent e)
    {
      if (!steering(e)) {
        super.mouseMoved(e);
      }
    }

    @Override
    public void mouseWheelMoved (MouseWheelEvent e)
    {
      if (!e.isControlDown()) {
        super.mouseWheelMoved(e);
      }
    }

    @Override
    public void keyPressed (KeyEvent e)
    {
      super.keyPressed(e);
      if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
        letGo(e.getWhen(), _lastX, _lastY);
        setCameraHeld(true);
      }
    }

    @Override
    public void keyReleased (KeyEvent e)
    {
      super.keyReleased(e);
      if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
        setCameraHeld(false);
      }
    }

    @Override
    protected void updateHoverComponent (int mx, int my)
    {
      // nowhere near the level while CTRL is held
      super.updateHoverComponent(_cameraHeld ? -1 : mx, _cameraHeld ? -1 : my);
    }

    /**
     * Notes whether CTRL is held to move the camera, taking the pointer off the level while it
     * is, and putting it back where it really is after.
     */
    protected void setCameraHeld (boolean held)
    {
      if (held == _cameraHeld) {
        return;
      }
      _cameraHeld = held;
      if (held) {
        updateHoverComponent(-1, -1);
      } else {
        // (the level hasn't heard where the pointer went while the camera moved)
        super.mouseMoved(new MouseEvent(_glCanvas, MouseEvent.MOUSE_MOVED,
          System.currentTimeMillis(), 0, _lastX, _lastY, 0, false));
      }
    }

    /**
     * Checks whether a motion event belongs to moving the camera rather than the knight,
     * letting go of any held buttons if it does.
     */
    protected boolean steering (MouseEvent e)
    {
      _lastX = e.getX();
      _lastY = e.getY();
      // (catching a CTRL release that happened while another window had the keyboard)
      if (!e.isControlDown()) {
        setCameraHeld(false);
      }
      if (!e.isControlDown() && _ignored.isEmpty()) {
        return false;
      }
      letGo(e.getWhen(), e.getX(), e.getY());
      return true;
    }

    /**
     * Tells the level the buttons it thinks are held have been released, so the knight stops.
     */
    protected void letGo (long when, int x, int y)
    {
      for (Iterator<Integer> it = _held.iterator(); it.hasNext(); ) {
        int button = it.next();
        it.remove();
        // these presses' releases will come, but the level has had them already
        _ignored.add(button);
        super.mouseReleased(new MouseEvent(_glCanvas, MouseEvent.MOUSE_RELEASED, when, 0,
          x, y, 0, false, button));
      }
    }

    protected final GlCanvas _glCanvas;

    /** The buttons the level knows are down, and those whose presses it never got. */
    protected final Set<Integer> _held = new HashSet<>(), _ignored = new HashSet<>();

    /** The last place the pointer was seen. */
    protected int _lastX, _lastY;

    /** Whether CTRL is held to move the camera. */
    protected boolean _cameraHeld;
  }

  /** The file holding the knight's model reference. */
  protected final File _knight;

  /** The classpath resource path of the level the knight is tested in. */
  protected final String _scene;

  /** The knight model config, held so that the config manager keeps our version of it. */
  protected ModelConfig _knightModel;

  /** Shown while the test starts up, or null once it's gone. */
  protected LoadingScreen _loading;

  /** How long the loading screen stays up after entering the level, in milliseconds. */
  protected static final int LOADING_GRACE = 1000;

  /** The button muting and unmuting the music, and whether the music is muted. */
  protected OverlayButton _musicButton;
  protected boolean _musicMuted;

  protected static final Preferences _prefs = Preferences.userNodeForPackage(KnightTester.class);

  /** Paces the view's frames, in place of the tool's own limiter. */
  protected FrameLimiter _frameLimiter;

  protected boolean _entered;

  /** The level camera the user has moved, and where it was before they first did. */
  protected OrbitCameraHandler _homeCamera;
  protected SphereCoords _home;

  /** How far dragging orbits and zooms the camera, and how far a turn of the wheel zooms it
   * (as clyde's mouse orbiter does). */
  protected static final float RADIANS_PER_PIXEL = FloatMath.PI / 1000f, UNITS_PER_PIXEL = 0.02f,
    UNITS_PER_CLICK = 0.3f;
}
