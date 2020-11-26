package theic2.xenobyteport.modules;

import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import theic2.xenobyteport.api.config.Cfg;
import theic2.xenobyteport.api.gui.ElementAligment;
import theic2.xenobyteport.api.gui.WidgetMessage;
import theic2.xenobyteport.api.gui.WidgetMode;
import theic2.xenobyteport.api.module.Category;
import theic2.xenobyteport.api.module.CheatModule;
import theic2.xenobyteport.api.module.PerformMode;
import theic2.xenobyteport.gui.click.elements.Button;
import theic2.xenobyteport.gui.click.elements.GuiWidget;
import theic2.xenobyteport.gui.click.elements.Panel;
import theic2.xenobyteport.render.Colors;
import theic2.xenobyteport.render.GuiScaler;
import theic2.xenobyteport.utils.TickHelper;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Widgets extends CheatModule {

    private Map<CheatModule, GuiWidget> keyabled, modulesInfo;
    @Cfg("showKeyabled")
    private boolean showKeyabled;
    @Cfg("showWidget")
    private boolean showWidget;
    @Cfg("showInfo")
    private boolean showInfo;
    private List<GuiWidget> infoWidgets;
    private int infoWidgetsDelay;

    public Widgets() {
        super("Widgets", Category.MISC, PerformMode.TOGGLE);
        modulesInfo = new HashMap<CheatModule, GuiWidget>();
        infoWidgets = new CopyOnWriteArrayList<GuiWidget>();
        keyabled = new HashMap<CheatModule, GuiWidget>();
        infoWidgetsDelay = TickHelper.FOUR_SEC;
        showKeyabled = true;
        showWidget = true;
        showInfo = true;
        cfgState = true;
    }

    @Override
    public void onPostInit() {
        keyabledMessage(moduleHandler().xenoGui());
        moduleHandler().categoryedModules().filter(CheatModule::provideStateEvents).forEach(this::keyabledMessage);
    }

    public void widgetMessage(WidgetMessage mess) {
        infoWidgets.add(0, new GuiWidget(mess.getMessage(), mess.getMode(), ElementAligment.LEFT, Colors.TRANSPARENT_DARKEST, infoWidgetsDelay));
        updateWidgetPoses();
    }

    public void infoMessage(WidgetMessage mess) {
        if (mess.hasModule()) {
            modulesInfo.put(mess.getModule(), new GuiWidget(mess.getMessage(), mess.getMode(), ElementAligment.LEFT, Colors.TRANSPARENT_DARKEST, 0));
            updateInfoPoses();
        }
    }

    public void hideInfoMessage(CheatModule m) {
        if (modulesInfo.containsKey(m)) {
            modulesInfo.remove(m);
            updateInfoPoses();
        }
    }

    private void keyabledMessage(CheatModule m) {
        String out = moduleHandler().isEnabled(m) || m.hasKeyBind() ? m.getName() + (m.hasKeyBind() ? "-" + m.getKeyName() : "") : null;
        int height = 0;
        if (out != null) {
            keyabled.put(m, new GuiWidget(out, m.getMode() == PerformMode.SINGLE ? WidgetMode.INFO : moduleHandler().isEnabled(m) ? WidgetMode.SUCCESS : WidgetMode.FAIL, ElementAligment.LEFT, Colors.TRANSPARENT_DARK, 0));
        } else {
            if (keyabled.containsKey(m)) {
                keyabled.remove(m);
            } else {
                return;
            }
        }
        keyabled = sorted(keyabled);
        for (GuiWidget w : keyabled.values()) {
            w.setY(height);
            height += w.getHeight();
        }
    }

    private void updateInfoPoses() {
        if (GuiScaler.isGuiCreated()) {
            modulesInfo = sorted(modulesInfo);
            int height = 0;
            for (GuiWidget w : modulesInfo.values()) {
                height += w.getHeight();
                w.setPos(GuiScaler.scaledScreenWidth() - w.getWidth(), GuiScaler.scaledScreenHeight() - height);
            }
        }
    }

    private void updateWidgetPoses() {
        if (GuiScaler.isGuiCreated()) {
            infoWidgets.forEach(w -> {
                w.setY(GuiScaler.scaledScreenHeight() - w.getHeight() * (infoWidgets.indexOf(w) + 1));
            });
        }
    }

    private LinkedHashMap<CheatModule, GuiWidget> sorted(Map<CheatModule, GuiWidget> map) {
        return map.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getWidth(), e1.getValue().getWidth()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    @SubscribeEvent
    public void guiInit(InitGuiEvent.Pre e) {
        updateWidgetPoses();
        updateInfoPoses();
    }

    @Override
    public void onDrawGuiLast() {
        if (showWidget) {
            infoWidgets.forEach(GuiWidget::draw);
        }
    }

    @Override
    public void onDrawGuiOverlay() {
        try {
            if (showInfo) {
                modulesInfo.values().forEach(GuiWidget::draw);
            }
            if (showKeyabled) {
                keyabled.values().forEach(GuiWidget::draw);
            }
        } catch (ConcurrentModificationException e) {
        }
    }

    @Override
    public void onTick(boolean inGame) {
        infoWidgets.removeIf(w -> --w.delay <= 0);
    }

    @Override
    public boolean provideStateEvents() {
        return false;
    }

    @Override
    public void onModuleBinded(CheatModule m) {
        keyabledMessage(m);
    }

    @Override
    public void onModuleEnabled(CheatModule m) {
        keyabledMessage(m);
    }

    @Override
    public void onModuleUnBinded(CheatModule m) {
        keyabledMessage(m);
    }

    @Override
    public void onModuleDisabled(CheatModule m) {
        hideInfoMessage(m);
        keyabledMessage(m);
    }

    @Override
    public String moduleDesc() {
        return "Отображение заданных информационных виджетов";
    }

    @Override
    public Panel settingPanel() {
        return new Panel(
                new Button("Keyabled", showKeyabled) {
                    @Override
                    public void onLeftClick() {
                        buttonValue(showKeyabled = !showKeyabled);
                    }

                    @Override
                    public String elementDesc() {
                        return "Активные и забинженные модули";
                    }
                },
                new Button("Messages", showWidget) {
                    @Override
                    public void onLeftClick() {
                        buttonValue(showWidget = !showWidget);
                    }

                    @Override
                    public String elementDesc() {
                        return "Краткие виджет-сообщения (снизу слева)";
                    }
                },
                new Button("InfoPanels", showInfo) {
                    @Override
                    public void onLeftClick() {
                        buttonValue(showInfo = !showInfo);
                    }

                    @Override
                    public String elementDesc() {
                        return "Инфо панели (снизу справа)";
                    }
                }
        );
    }

}