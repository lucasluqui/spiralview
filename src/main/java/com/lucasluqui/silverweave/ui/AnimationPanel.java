package com.lucasluqui.silverweave.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.Border;

import com.threerings.opengl.model.Animation;

import com.lucasluqui.silverweave.model.KnightModel;

/**
 * A panel floating over the top left of the 3D view that lists the knight's animations and plays
 * or stops them.
 */
public class AnimationPanel
{
  /**
   * @param onPlay notified of the animation the user plays, and with null once it ends or is
   * stopped.
   */
  public AnimationPanel (Window owner, Component view, Consumer<Animation> onPlay)
  {
    _onPlay = onPlay;
    JPanel content = _content = new JPanel(new BorderLayout(0, GAP));
    content.setBorder(expandedBorder());

    // the title doubles as the switch that collapses the panel down to just itself
    JLabel title = new JLabel("ANIMATIONS");
    title.setFont(Theme.sideFont(10.5f, Font.BOLD));
    title.setForeground(Theme.MUTED);
    _chevron = new JLabel();
    JPanel header = new JPanel(new BorderLayout()) {
      @Override public Dimension getPreferredSize () {
        // as wide as the list, so the panel keeps its width when collapsed (qualified, as a bare
        // WIDTH in here is the one components inherit from ImageObserver)
        return new Dimension(AnimationPanel.WIDTH + SCROLL_SHIFT, super.getPreferredSize().height);
      }
    };
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6 + SCROLL_SHIFT));
    header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    header.add(title, BorderLayout.WEST);
    header.add(_chevron, BorderLayout.EAST);
    Clickable.install(header, () -> setCollapsed(!_collapsed), hover -> {
      _headerHover = hover;
      title.setForeground(hover ? Theme.TEXT : Theme.MUTED);
      updateChevron();
    });
    updateChevron();
    content.add(header, BorderLayout.NORTH);

    _list = new JList<>(_model);
    _list.setOpaque(false);
    _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _list.setFixedCellHeight(28);
    _list.setCellRenderer(new DefaultListCellRenderer() {
      @Override public Component getListCellRendererComponent (
        JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, false);
        Animation animation = (Animation)value;
        setText(KnightModel.label(animation.getName()));
        setFont(Theme.sideFont(12.5f, Font.PLAIN));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        // the selection (and the animation under the mouse) is a rounded highlight, painted below
        _highlight = selected ? Theme.SELECTION :
          (index == _hoverIndex ? Theme.SURFACE_HOVER : null);
        setOpaque(false);
        setForeground(animation.isPlaying() ? Theme.ACCENT : Theme.TEXT);
        return this;
      }
      @Override protected void paintComponent (Graphics g) {
        if (_highlight != null) {
          Graphics2D g2 = Theme.smooth(g);
          g2.setColor(_highlight);
          g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, SELECTION_ARC, SELECTION_ARC);
          g2.dispose();
        }
        super.paintComponent(g);
      }
      protected Color _highlight;
    });
    _list.addListSelectionListener(e -> update());
    MouseAdapter mouse = new MouseAdapter() {
      @Override public void mouseClicked (MouseEvent e) {
        if (e.getClickCount() == 2 && _list.getSelectedValue() != null) {
          play(_list.getSelectedValue());
        }
      }
      @Override public void mouseMoved (MouseEvent e) {
        setHoverIndex(indexAt(e.getPoint()));
      }
      @Override public void mouseDragged (MouseEvent e) {
        setHoverIndex(indexAt(e.getPoint()));
      }
      @Override public void mouseExited (MouseEvent e) {
        setHoverIndex(-1);
      }
    };
    _list.addMouseListener(mouse);
    _list.addMouseMotionListener(mouse);
    JScrollPane scroll = _scroll = new JScrollPane(_list,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    Theme.overlayScroll(scroll);
    scroll.setPreferredSize(new Dimension(WIDTH + SCROLL_SHIFT, LIST_HEIGHT));
    content.add(scroll, BorderLayout.CENTER);

    _button = new OverlayButton(PLAY, "Play", null, false, this::toggle);
    // a filled button, so it reads as part of the panel rather than floating loose in it
    _button.setRestingColor(Theme.SURFACE);
    JPanel south = _south = new JPanel(new BorderLayout());
    south.setOpaque(false);
    south.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, SCROLL_SHIFT));
    south.add(_button, BorderLayout.CENTER);
    content.add(south, BorderLayout.SOUTH);

    _overlay = new Overlay(owner, view, Overlay.Anchor.TOP_LEFT, content, 16);

    // animations end on their own, so keep the button and highlights in step with them
    _poll = new Timer(250, e -> update());
    _poll.start();
  }

  public void show ()
  {
    _overlay.show();
  }

  public boolean isCollapsed ()
  {
    return _collapsed;
  }

  /**
   * Collapses the panel down to its title, or expands it again. Whatever is playing carries on.
   */
  public void setCollapsed (boolean collapsed)
  {
    if (collapsed == _collapsed) {
      return;
    }
    _collapsed = collapsed;
    _scroll.setVisible(!collapsed);
    _south.setVisible(!collapsed);
    // the layout leaves its gap under the title even with nothing below it
    ((BorderLayout)_content.getLayout()).setVgap(collapsed ? 0 : GAP);
    _content.setBorder(collapsed ? collapsedBorder() : expandedBorder());
    updateChevron();
    _overlay.pack();
    if (_onCollapse != null) {
      _onCollapse.accept(collapsed);
    }
  }

  public void setCollapseListener (Consumer<Boolean> onCollapse)
  {
    _onCollapse = onCollapse;
  }

  /**
   * Updates the animations listed after the knight model changes, keeping the selection. Whatever
   * the user was playing went away with the old model.
   */
  public void setAnimations (Animation[] animations)
  {
    _playing = null;
    _hoverIndex = -1;
    Animation selected = _list.getSelectedValue();
    String name = selected == null ? null : selected.getName();
    _model.clear();
    List<Animation> playable = new ArrayList<>();
    for (Animation animation : animations) {
      if (KnightModel.isPlayable(animation.getName())) {
        playable.add(animation);
      }
    }
    playable.sort(Comparator.comparing(a -> KnightModel.label(a.getName()),
      String.CASE_INSENSITIVE_ORDER));
    for (Animation animation : playable) {
      _model.addElement(animation);
      if (animation.getName().equals(name)) {
        _list.setSelectedValue(animation, false);
      }
    }
    if (_list.getSelectedIndex() < 0 && !_model.isEmpty()) {
      _list.setSelectedIndex(0);
    }
    update();
  }

  /**
   * Stops the animation the user is playing, or plays the selected one if there's none.
   */
  protected void toggle ()
  {
    if (_playing != null) {
      _playing.stop();
      finished();
    } else if (_list.getSelectedValue() != null) {
      play(_list.getSelectedValue());
    }
    update();
  }

  /**
   * Plays an animation in place of the one the user was playing, if any.
   */
  protected void play (Animation animation)
  {
    if (_playing != null && _playing.isPlaying()) {
      _playing.stop();
    }
    _playing = animation;
    _onPlay.accept(animation);
    animation.start();
    update();
  }

  /**
   * Notes that the user's animation is over.
   */
  protected void finished ()
  {
    _playing = null;
    _onPlay.accept(null);
  }

  /**
   * Returns the index of the animation under a point in the list, or -1 if there's none.
   */
  protected int indexAt (Point point)
  {
    int index = _list.locationToIndex(point);
    Rectangle bounds = index < 0 ? null : _list.getCellBounds(index, index);
    return bounds != null && bounds.contains(point) ? index : -1;
  }

  protected void setHoverIndex (int index)
  {
    if (index != _hoverIndex) {
      _hoverIndex = index;
      _list.repaint();
    }
  }

  /**
   * Points the chevron by the title the way the panel would go: down to expand it while collapsed,
   * up to collapse it while expanded.
   */
  protected void updateChevron ()
  {
    _chevron.setIcon((_collapsed ? Glyph.CHEVRON_DOWN : Glyph.CHEVRON_UP).icon(
      CHEVRON_SIZE, _headerHover ? Theme.TEXT : Theme.MUTED));
  }

  /**
   * Returns the padding around the expanded panel: less room on the right, so the scroll bar sits
   * out by the edge; the title and button make the difference up themselves. The title stays where
   * it is when the panel collapses.
   */
  protected static Border expandedBorder ()
  {
    return BorderFactory.createEmptyBorder(TITLE_TOP, 10, 10, 10 - SCROLL_SHIFT);
  }

  /**
   * Returns the padding around the collapsed panel, which makes it as tall as the button bars over
   * the view, with the title's capitals about centered.
   */
  protected static Border collapsedBorder ()
  {
    return BorderFactory.createEmptyBorder(TITLE_TOP, 10, 13, 10 - SCROLL_SHIFT);
  }

  /**
   * Syncs the button with the playback state; animations end on their own.
   */
  protected void update ()
  {
    if (_playing != null && !_playing.isPlaying()) {
      finished();
    }
    boolean playing = _playing != null;
    _button.setText(playing ? "Stop" : "Play");
    _button.setGlyph(playing ? STOP : PLAY);
    _button.setEnabled(playing || _list.getSelectedValue() != null);
    // only repaint when an animation starts or stops: on some systems (X11 without a compositor
    // keeping up), every repaint of a see-through window can flash
    BitSet playingNow = new BitSet();
    for (int ii = 0; ii < _model.size(); ii++) {
      playingNow.set(ii, _model.get(ii).isPlaying());
    }
    if (!playingNow.equals(_shownPlaying)) {
      _shownPlaying = playingNow;
      _list.repaint();
    }
  }

  /** How much further right the scroll bar sits than the title and button reach. */
  protected static final int SCROLL_SHIFT = 5;

  /** The space between the title, the list and the button. */
  protected static final int GAP = 8;

  /** The space above the title, the same expanded and collapsed. */
  protected static final int TITLE_TOP = 13;

  /** The button's symbols, kept so that setting the same one again is no change. */
  protected static final Consumer<Graphics2D> PLAY = Glyph.PLAY::paint, STOP = Glyph.STOP::paint;

  /** The roundness of the selected animation's highlight. */
  protected static final int SELECTION_ARC = 12;

  /** The size of the animation list. */
  protected static final int WIDTH = 240, LIST_HEIGHT = 300;

  /** The size of the chevron by the title. */
  protected static final int CHEVRON_SIZE = 12;

  protected final DefaultListModel<Animation> _model = new DefaultListModel<>();
  protected final JPanel _content, _south;
  protected final JLabel _chevron;
  protected final JList<Animation> _list;
  protected final JScrollPane _scroll;
  protected final OverlayButton _button;
  protected final Overlay _overlay;
  protected final Timer _poll;
  protected final Consumer<Animation> _onPlay;

  /** The animation the user is playing, if any. */
  protected Animation _playing;

  /** Which of the listed animations were playing when the list was last painted. */
  protected BitSet _shownPlaying = new BitSet();

  /** The index of the animation under the mouse, or -1. */
  protected int _hoverIndex = -1;

  protected boolean _collapsed;

  /** Whether the mouse is over the title. */
  protected boolean _headerHover;

  protected Consumer<Boolean> _onCollapse;
}
