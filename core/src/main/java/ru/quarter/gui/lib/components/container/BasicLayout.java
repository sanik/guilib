/*
 * Copyright 2020 Stanislav Batalenkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.quarter.gui.lib.components.container;

import org.lwjgl.opengl.GL11;
import ru.quarter.gui.lib.GuiLib;
import ru.quarter.gui.lib.api.IGraphicsComponent;
import ru.quarter.gui.lib.api.IGraphicsLayout;
import ru.quarter.gui.lib.api.IListener;
import ru.quarter.gui.lib.api.ISelector;
import ru.quarter.gui.lib.api.adapter.IScaledResolution;
import ru.quarter.gui.lib.components.GBasic;

import java.util.*;

public class BasicLayout<T extends IGraphicsComponent> extends GBasic implements IGraphicsLayout<T> {

    // dynamic
    IScaledResolution res;

    // private
    private int nextID = 0;
    // for ID access
    private final Map<Integer, T> components = new HashMap<>();
    // for rendering
    private final NavigableSet<T> sorted = new TreeSet<>(((o1, o2) -> {
        if (o1.getDepth() == o2.getDepth()) {
            return o1.getID() - o2.getID();
        }
        return o1.getDepth() - o2.getDepth();
    }));

    private IListener<IGraphicsComponent> tooltip;
    private ISelector selector;

    protected BasicLayout() {}

    public BasicLayout(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public int addComponent(int depth, T component) {
        component.setDepth(depth);
        component.setID(nextID);
        component.setParent(this);
        components.put(nextID, component);
        sorted.add(component);
        return nextID++;
    }

    @Override
    public T getComponent(int id) {
        return components.get(id);
    }

    @Override
    public T removeComponent(int id) {
        T removed = components.remove(id);
        sorted.remove(removed);
        return removed;
    }

    @Override
    public int size() {
        return components.size();
    }

    protected void setContent(List<T> newContent) {
        clear();
        newContent.forEach(component -> addComponent(component.getDepth(), component));
    }

    public void clear() {
        components.clear();
        sorted.clear();
        nextID = 0;
    }

    @Override
    public void setTooltip(IListener<IGraphicsComponent> tooltip) {
        if (tooltip == null) {
            throw new IllegalArgumentException("Tooltip mustn't be null!");
        }
        this.tooltip = tooltip;
    }

    @Override
    public IListener<IGraphicsComponent> getOwnTooltip() {
        return this.tooltip;
    }

    @Override
    public void setSelector(ISelector selector) {
        this.selector = selector;
    }

    @Override
    public ISelector getSelector() {
        return selector;
    }

    @Override
    public boolean checkUpdates() {
        boolean dirty = false;
        for (IGraphicsComponent component : sorted) {
            if (component.checkUpdates()) {
                component.markDirty();
                dirty = true;
            }
        }
        return dirty;
    }

    @Override
    public void onMousePressed(int mouseX, int mouseY, int mouseButton) {
        sorted.forEach(component -> {
            if (component.intersects(mouseX, mouseY)) {
                component.onMousePressed(mouseX - component.getX(), mouseY - component.getY(), mouseButton);
            }
        });
        if (getOwnTooltip() != null) {
            getOwnTooltip().onMousePressed(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        sorted.forEach(component -> {
            if (component.intersects(mouseX, mouseY)) {
                component.onMouseReleased(mouseX - component.getX(), mouseY - component.getY(), mouseButton);
            }
        });
        if (getOwnTooltip() != null) {
            getOwnTooltip().onMouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void onKeyPressed(char typedChar, int keyCode) {
        sorted.forEach(component -> component.onKeyPressed(typedChar, keyCode));
        if (getOwnTooltip() != null) {
            getOwnTooltip().onKeyPressed(typedChar, keyCode);
        }
    }

    @Override
    public void onHover(int mouseX, int mouseY) {
        sorted.forEach(component -> {
            if (component.intersects(mouseX, mouseY)) {
                component.onHover(mouseX - component.getX(), mouseY - component.getY());
            }
        });
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int depth = 0;
        for (IGraphicsComponent component : sorted) {
            if (component.getDepth() != depth) {
                GL11.glTranslatef(0.0F, 0.0F, component.getDepth() - depth);
                depth = component.getDepth();
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            //GL11.glEnable(GL11.GL_BLEND);
            component.render(mouseX, mouseY);
        }

        if (getOwnTooltip() != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, Integer.MAX_VALUE);
            getOwnTooltip().render(mouseX, mouseY);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void update() {
        sorted.forEach(component -> {
            if (component.needUpdate()) {
                component.update();
            }
        });
        needUpdate = false;
        if (getOwnTooltip() != null) {
            getOwnTooltip().update();
        }
    }

    @Override
    public void init() {
        onResize(getWidth(), getHeight());
        sorted.forEach(IGraphicsComponent::init);
        if (getOwnTooltip() != null) {
            getOwnTooltip().init();
        }
    }

    @Override
    public void onClosed() {
        sorted.forEach(IGraphicsComponent::onClosed);
        if (getOwnTooltip() != null) {
            getOwnTooltip().onClosed();
        }
    }

    @Override
    public void onResize(int w, int h) {
        this.res = GuiLib.scaled();
        // TODO Write resize processing
    }
}
