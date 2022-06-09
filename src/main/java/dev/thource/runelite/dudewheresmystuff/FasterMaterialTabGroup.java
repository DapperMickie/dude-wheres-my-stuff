package dev.thource.runelite.dudewheresmystuff;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.util.SwingUtil;

/**
 * This class will be a container (group) for the new Material Tabs. It will contain a list of tabs
 * and a display (JPanel). When a tab is selected, the JPanel "display" will display the content
 * associated with that tab.
 *
 * <p>How to use these tabs:
 *
 * <ol>
 *   <li>1 - Create displays (JPanels) for each tab
 *   <li>2 - Create an empty JPanel to serve as the group's display
 *   <li>3 - Create a new MaterialGroup, passing the panel in step 2 as a param
 *   <li>4 - Create new tabs, passing the group in step 3 and one of the panels in step 1 as params
 *   <li>5 - Add the tabs to the group using the MaterialTabGroup#addTab method
 *   <li>6 - Select one of the tabs using the MaterialTab#select method
 * </ol>
 *
 * @author Psikoi
 */
public class FasterMaterialTabGroup extends JPanel {
  /* The panel on which the content tab's content will be displayed on. */
  private final JPanel display;
  /* A list of all the tabs contained in this group. */
  private final List<FasterMaterialTab> tabs = new ArrayList<>();

  public FasterMaterialTabGroup(JPanel display) {
    this.display = display;
    if (display != null) {
      this.display.setLayout(new BorderLayout());
    }
    setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
    setOpaque(false);
  }

  public FasterMaterialTabGroup() {
    this(null);
  }

  private static void fastRemoveAll(Container c, boolean isMainParent) {
    // If we are not on the EDT this will deadlock, in addition to being totally unsafe
    assert SwingUtilities.isEventDispatchThread();

    // when a component is removed it has to be resized for some reason, but only if it's valid
    // so we make sure to invalidate everything before removing it
    c.invalidate();
    for (int i = 0; i < c.getComponentCount(); i++) {
      Component ic = c.getComponent(i);

      // removeAll and removeNotify are both recursive, so we have to recurse before them
      if (ic instanceof Container) {
        fastRemoveAll((Container) ic, false);
      }

      // each removeNotify needs to remove anything from the event queue that is for that widget
      // this however requires taking a lock, and is moderately slow, so we just execute all of
      // those events with a secondary event loop
      SwingUtil.pumpPendingEvents();

      // call removeNotify early; this is most of the work in removeAll, and generates events that
      // the next secondaryLoop will pickup
      ic.removeNotify();
    }

    if (isMainParent) {
      // Actually remove anything
      c.removeAll();
    }
  }

  private static void revalidateAll(Container c) {
    c.revalidate();
    for (int i = 0; i < c.getComponentCount(); i++) {
      Component ic = c.getComponent(i);

      if (ic instanceof Container) {
        revalidateAll((Container) ic);
      }

      ic.addNotify();
    }
  }

  /* Returns the tab on a certain index. */
  public FasterMaterialTab getTab(int index) {

    if (tabs.isEmpty()) {
      return null;
    }

    return tabs.get(index);
  }

  public void addTab(FasterMaterialTab tab) {
    tabs.add(tab);
    add(tab, BorderLayout.NORTH);
  }

  /***
   * Selects a tab from the group, and sets the display's content to the
   * tab's associated content.
   * @param selectedTab - The tab to select
   */
  public boolean select(FasterMaterialTab selectedTab) {
    if (!tabs.contains(selectedTab)) {
      return false;
    }

    // If the OnTabSelected returned false, exit the method to prevent tab switching
    if (!selectedTab.select()) {
      return false;
    }

    // If the display is available, switch from the old to the new display
    if (display != null) {
      fastRemoveAll(display, true);
      display.add(selectedTab.getContent());
      display.revalidate();
      display.repaint();
    }

    // Unselected all other tabs
    for (FasterMaterialTab tab : tabs) {
      if (!tab.equals(selectedTab)) {
        tab.unselect();
      }
    }

    return true;
  }
}
