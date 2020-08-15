package ru.spliterash.musicbox.players;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import ru.spliterash.musicbox.Lang;
import ru.spliterash.musicbox.MusicBox;
import ru.spliterash.musicbox.MusicBoxConfig;
import ru.spliterash.musicbox.customPlayers.interfaces.PlayerSongPlayer;
import ru.spliterash.musicbox.customPlayers.objects.RadioPlayer;
import ru.spliterash.musicbox.customPlayers.objects.SpeakerPlayer;
import ru.spliterash.musicbox.gui.song.GUIMode;
import ru.spliterash.musicbox.song.MusicBoxSong;
import ru.spliterash.musicbox.song.MusicBoxSongManager;
import ru.spliterash.musicbox.gui.song.SongContainerGUI;
import ru.spliterash.musicbox.utils.BukkitUtils;

import java.io.File;
import java.util.Optional;

/**
 * Класс в котором находится вся инфа об игроке
 * Пока он на сервере
 */
@Getter
public class PlayerWrapper {
    public static final String METADATA_KEY = "musicboxInstance";
    private static final File playersFolder = new File(MusicBox.getInstance().getDataFolder(), "players");
    /**
     * Игрок которому принадлежит этот инстанц
     */
    private final Player player;
    private final PlayerConfig config;
    /**
     * Включен ли режим колонки
     * Не хочу хранить это в конфиге
     * Поскольку с этим может возникнуть много проблем
     */
    private boolean speaker;
    /**
     * Активный проигрыватель(если есть)
     */
    private PlayerSongPlayer activePlayer;

    /**
     * Боссбар для отображения проигрывания
     */
    @Getter(AccessLevel.NONE)
    private BossBar playBar;

    /**
     * Вызывается только если игрок захочет что нибудь послушать
     */
    private PlayerWrapper(Player player) {
        this.player = player;
        MusicBoxConfig.BossBarSetting bossBarConfig = MusicBox.getInstance().getConfigObject().getBossbar();
        if (bossBarConfig.isEnable()) {
            this.playBar = Bukkit.createBossBar("", BarColor.valueOf(bossBarConfig.getColor()), BarStyle.valueOf(bossBarConfig.getStyle()));
            this.playBar.setVisible(false);
            this.playBar.addPlayer(player);
        }
        this.config = PlayerConfig.load(new File(playersFolder, player.getUniqueId() + ".yml"));
    }

    /**
     * Отдаёт не пустой {@link Optional} если игрок хоть раз включал музыку
     */
    public static Optional<PlayerWrapper> getInstanceOptional(Player player) {
        return Optional.ofNullable(BukkitUtils.extractMetadata(PlayerWrapper.class, player, METADATA_KEY));
    }

    /**
     * Создаёт инстанц если его нет и возращает
     */
    public static PlayerWrapper getInstance(Player player) {
        return getInstanceOptional(player)
                .orElseGet(() -> {
                    PlayerWrapper instance = new PlayerWrapper(player);
                    player.setMetadata(METADATA_KEY, new FixedMetadataValue(MusicBox.getInstance(), instance));
                    return instance;
                });
    }

    public static void clearAll() {
        Bukkit
                .getOnlinePlayers()
                .stream()
                .map(PlayerWrapper::getInstanceOptional)
                .forEach(o -> o.ifPresent(PlayerWrapper::destroy));
    }

    /**
     * Открыть инвентарь для выбора музыки
     */
    public void openDefaultInventory() {
        SongContainerGUI gui = MusicBoxSongManager.getRootContainer().createGUI(this);
        gui.openPage(0, GUIMode.DEFAULT_MODE);
    }

    /**
     * Открыть инвентарь для покупки пластинок
     */
    public void openShopInventory() {
        SongContainerGUI gui = MusicBoxSongManager.getRootContainer().createGUI(this);
        gui.openPage(0, GUIMode.SHOP_MODE);
    }


    /**
     * Сохраняет всё на жесткий диск
     * И удаляет инстанц из игрока
     */
    public void destroy() {
        stopPlay();
        player.removeMetadata(METADATA_KEY, MusicBox.getInstance());
        if (playBar != null)
            playBar.removeAll();
        config.save();
    }

    public boolean isPlayNow() {
        return activePlayer != null;
    }

    /**
     * Может ли игрок поменять режим
     * А именно включить режим колонки
     */
    public boolean canSwitch() {
        return getPlayer().hasPermission("musicbox.speaker");
    }

    public boolean switchModeChecked() {
        if (canSwitch()) {
            player.sendMessage(Lang.CANT_SWITCH.toString());
            return false;
        } else {
            switchMode();
            return true;
        }
    }

    /**
     * Меняет режим с колонки и обратно
     */
    public void switchMode() {
        speaker = !speaker;
        if (isPlayNow()) {
            PlayerSongPlayer oldPlayer = getActivePlayer();
            play(oldPlayer.getMusicBoxSong(), oldPlayer.getSongPlayer().getTick());
        }
    }

    public void play(MusicBoxSong song) {
        play(song, (short) -1);
    }

    /**
     * Проигрывает игроку звук, с учётом его режима звучания
     * <p>
     * Внимание... Метод пересоздаёт текущий проигрыватель
     * для перемотки юзайте метод
     *
     * @param song Мелодия которую нужно проиграть
     * @param tick С какого тика (-1 если с начала)
     */
    public void play(MusicBoxSong song, short tick) {
        if (speaker)
            startSpeaker(song);
        else
            startRadio(song);
        if (tick > -1)
            activePlayer.getSongPlayer().setTick(tick);
    }

    public void startSpeaker(MusicBoxSong song) {
        destroyActivePlayer();
        activePlayer = new SpeakerPlayer(song, this);
    }

    public void startRadio(MusicBoxSong song) {
        destroyActivePlayer();
        activePlayer = new RadioPlayer(song, this);
    }

    /**
     * Останавливает активный проигрыватель
     */
    public synchronized void destroyActivePlayer() {
        if (activePlayer != null) {
            activePlayer.destroy();
            activePlayer = null;
        }
    }

    public void setBarVisible(boolean visible) {
        if (playBar != null)
            playBar.setVisible(visible);
    }

    public void setBarTitle(String title) {
        if (playBar != null)
            playBar.setTitle(title);
    }

    public void setBarProgress(double progress) {
        if (playBar != null)
            playBar.setProgress(progress);
    }

    /**
     * Полностью офает проигрывание
     * Метод на будущее если будут плейлисты
     */
    public void stopPlay() {
        destroyActivePlayer();
    }
}
