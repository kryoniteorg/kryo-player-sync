package org.kryonite.kryoplayersync.paper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kryonite.kryomessaging.api.MessagingService;
import org.kryonite.kryoplayersync.paper.listener.PlayerListener;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KryoPlayerSyncPluginTest {

  private KryoPlayerSyncPlugin testee;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private HikariDataSource dataSourceMock;

  @Mock
  private MessagingService messagingServiceMock;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Server serverMock;

  @BeforeEach
  void setup() {
    try (MockedStatic<PaperPluginLogger> paperPluginLoggerMockedStatic = Mockito.mockStatic(PaperPluginLogger.class)) {
      PluginDescriptionFile pluginDescriptionFile = mock(PluginDescriptionFile.class);

      paperPluginLoggerMockedStatic
          .when(() -> PaperPluginLogger.getLogger(pluginDescriptionFile))
          .thenReturn(mock(Logger.class));

      testee = new KryoPlayerSyncPlugin(
          new JavaPluginLoader(serverMock),
          pluginDescriptionFile,
          new File("data"),
          mock(File.class),
          dataSourceMock,
          messagingServiceMock
      );
    }
  }

  @Test
  void shouldSetupListener() throws SQLException, IOException {
    // Arrange
    when(serverMock.getPluginManager().isPluginEnabled("Vault")).thenReturn(true);
    when(serverMock.getServicesManager().getRegistration(Economy.class).getProvider()).thenReturn(mock(Economy.class));

    // Act
    testee.onEnable();

    // Assert
    verify(dataSourceMock.getConnection(), times(2)).prepareStatement(anyString());
    verify(messagingServiceMock).setupExchange(any(), any());
    verify(serverMock.getPluginManager()).registerEvents(any(PlayerListener.class), any());
  }

  @Test
  void shouldShutdownServer_WhenInventoryRepositoryNotAvailable() throws SQLException, IOException {
    // Arrange
    when(dataSourceMock.getConnection()).thenThrow(SQLException.class);

    // Act
    testee.onEnable();

    // Assert
    verify(serverMock).shutdown();
    verify(messagingServiceMock, never()).setupExchange(any(), any());
    verify(serverMock.getPluginManager(), never()).registerEvents(any(PlayerListener.class), any());
  }

  @Test
  void shouldShutdownServer_WhenVaultPluginNotAvailable() throws IOException {
    // Arrange
    when(serverMock.getPluginManager().isPluginEnabled("Vault")).thenReturn(false);

    // Act
    testee.onEnable();

    // Assert
    verify(serverMock).shutdown();
    verify(messagingServiceMock, never()).setupExchange(any(), any());
    verify(serverMock.getPluginManager(), never()).registerEvents(any(PlayerListener.class), any());
  }
}
